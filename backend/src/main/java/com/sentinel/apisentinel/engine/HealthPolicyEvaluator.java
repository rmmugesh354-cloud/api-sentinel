package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class HealthPolicyEvaluator {

    public record PolicyEvaluation(
            boolean isHealthy,
            boolean isWithinLatency,
            String errorReason,
            String errorDetails
    ) {}

    public PolicyEvaluation evaluate(MonitoredEndpoint endpoint, HttpExecutionResult execResult) {
        // Check for network level failures (DNS, timeout, connection refused, SSRF blocked)
        if (!execResult.isNetworkSuccess() || execResult.httpStatusCode() == null) {
            return new PolicyEvaluation(
                    false,
                    false,
                    execResult.errorType() != null ? execResult.errorType() : "NETWORK_FAILURE",
                    execResult.errorMessage() != null ? execResult.errorMessage() : "Failed to establish HTTP connection"
            );
        }

        int statusCode = execResult.httpStatusCode();
        Set<Integer> expectedCodes = parseExpectedCodes(endpoint.getExpectedStatusCodes());

        boolean statusMatch = expectedCodes.contains(statusCode);
        boolean latencyMatch = execResult.latencyMs() <= endpoint.getLatencyThresholdMillis();

        if (!statusMatch) {
            return new PolicyEvaluation(
                    false,
                    latencyMatch,
                    "UNEXPECTED_STATUS_CODE",
                    "Expected " + endpoint.getExpectedStatusCodes() + " but received HTTP " + statusCode
            );
        }

        if (!latencyMatch) {
            // Status code is matched, but response time exceeds the threshold
            return new PolicyEvaluation(
                    true, // It is functional, but degraded
                    false,
                    "LATENCY_THRESHOLD_EXCEEDED",
                    "Response latency " + execResult.latencyMs() + "ms exceeded threshold of " + endpoint.getLatencyThresholdMillis() + "ms"
            );
        }

        return new PolicyEvaluation(true, true, null, null);
    }

    private Set<Integer> parseExpectedCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of(200);
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }
}
