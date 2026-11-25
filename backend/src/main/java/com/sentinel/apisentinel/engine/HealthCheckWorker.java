package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.check.HealthCheckResult;
import com.sentinel.apisentinel.check.HealthCheckResultRepository;
import com.sentinel.apisentinel.common.exception.SsrfSecurityException;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import com.sentinel.apisentinel.endpoint.MonitoredEndpointRepository;
import com.sentinel.apisentinel.incident.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class HealthCheckWorker {

    private final MonitoredEndpointRepository endpointRepository;
    private final HealthCheckResultRepository checkResultRepository;
    private final SsrfValidator ssrfValidator;
    private final SafeHttpClient safeHttpClient;
    private final HealthPolicyEvaluator policyEvaluator;
    private final IncidentService incidentService;

    @Transactional
    public HealthCheckResult runCheck(Long endpointId) {
        MonitoredEndpoint endpoint = endpointRepository.findById(endpointId).orElse(null);
        if (endpoint == null) {
            log.warn("Health check requested for non-existent endpoint ID {}", endpointId);
            return null;
        }

        if (!endpoint.isActive()) {
            log.debug("Skipping paused endpoint ID {}", endpointId);
            return null;
        }

        HealthCheckResult result;

        try {
            // First: Enforce SSRF validation
            ssrfValidator.validateUrl(endpoint.getUrl());

            // Second: Execute safe HTTP request
            HttpExecutionResult execResult = safeHttpClient.execute(
                    endpoint.getUrl(),
                    endpoint.getHttpMethod(),
                    endpoint.getTimeoutMillis()
            );

            // Third: Evaluate against endpoint health policy
            HealthPolicyEvaluator.PolicyEvaluation eval = policyEvaluator.evaluate(endpoint, execResult);

            result = HealthCheckResult.builder()
                    .endpoint(endpoint)
                    .checkedAt(Instant.now())
                    .responseTimeMs((int) execResult.latencyMs())
                    .httpStatusCode(execResult.httpStatusCode())
                    .isHealthy(eval.isHealthy())
                    .errorReason(eval.errorReason())
                    .errorDetails(eval.errorDetails())
                    .responseSizeBytes(execResult.responseSizeBytes())
                    .build();

            if (eval.isHealthy()) {
                incidentService.handleSuccess(endpoint, eval.isWithinLatency());
            } else {
                incidentService.handleFailure(endpoint, eval.errorReason(), eval.errorDetails());
            }

        } catch (SsrfSecurityException ssrfEx) {
            log.warn("SSRF blocked checking endpoint {} ({}): {}", endpoint.getId(), endpoint.getUrl(), ssrfEx.getMessage());

            result = HealthCheckResult.builder()
                    .endpoint(endpoint)
                    .checkedAt(Instant.now())
                    .responseTimeMs(0)
                    .httpStatusCode(null)
                    .isHealthy(false)
                    .errorReason("SSRF_PROHIBITED")
                    .errorDetails(ssrfEx.getMessage())
                    .responseSizeBytes(0)
                    .build();

            incidentService.handleFailure(endpoint, "SSRF_PROHIBITED", ssrfEx.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error executing health check for endpoint {}: {}", endpoint.getId(), ex.getMessage());

            result = HealthCheckResult.builder()
                    .endpoint(endpoint)
                    .checkedAt(Instant.now())
                    .responseTimeMs(0)
                    .httpStatusCode(null)
                    .isHealthy(false)
                    .errorReason("INTERNAL_CHECK_ERROR")
                    .errorDetails("Internal error executing probe: " + ex.getMessage())
                    .responseSizeBytes(0)
                    .build();

            incidentService.handleFailure(endpoint, "INTERNAL_CHECK_ERROR", ex.getMessage());
        }

        // Save check result record
        HealthCheckResult savedResult = checkResultRepository.save(result);

        // Schedule next check
        endpoint.setLastCheckedAt(Instant.now());
        endpoint.setNextCheckAt(Instant.now().plusSeconds(endpoint.getCheckIntervalSeconds()));
        endpointRepository.save(endpoint);

        return savedResult;
    }
}
