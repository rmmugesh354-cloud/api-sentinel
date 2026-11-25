package com.sentinel.apisentinel.endpoint;

import com.sentinel.apisentinel.check.HealthCheckResult;
import com.sentinel.apisentinel.check.HealthCheckResultRepository;
import com.sentinel.apisentinel.check.dto.HealthCheckResponse;
import com.sentinel.apisentinel.common.PageResponse;
import com.sentinel.apisentinel.common.exception.BadRequestException;
import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.endpoint.dto.EndpointRequest;
import com.sentinel.apisentinel.endpoint.dto.EndpointResponse;
import com.sentinel.apisentinel.engine.MonitoringScheduler;
import com.sentinel.apisentinel.engine.SsrfValidator;
import com.sentinel.apisentinel.user.User;
import com.sentinel.apisentinel.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final MonitoredEndpointRepository endpointRepository;
    private final HealthCheckResultRepository checkResultRepository;
    private final UserRepository userRepository;
    private final SsrfValidator ssrfValidator;
    private final MonitoringScheduler monitoringScheduler;

    @Value("${app.monitoring.limits.max-endpoints-per-user:50}")
    private int maxEndpointsPerUser;

    @Transactional
    public EndpointResponse createEndpoint(EndpointRequest request, Long userId) {
        long currentCount = endpointRepository.countByUserId(userId);
        if (currentCount >= maxEndpointsPerUser) {
            throw new BadRequestException("Endpoint quota exceeded. Maximum permitted per user: " + maxEndpointsPerUser);
        }

        // Validate URL against SSRF
        ssrfValidator.validateUrl(request.url());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        MonitoredEndpoint endpoint = MonitoredEndpoint.builder()
                .user(user)
                .name(request.name().trim())
                .url(request.url().trim())
                .httpMethod(request.httpMethod())
                .expectedStatusCodes(request.expectedStatusCodes().trim())
                .checkIntervalSeconds(request.checkIntervalSeconds())
                .timeoutMillis(request.timeoutMillis())
                .latencyThresholdMillis(request.latencyThresholdMillis())
                .consecutiveFailuresThreshold(request.consecutiveFailuresThreshold())
                .isActive(true)
                .currentStatus(EndpointStatus.UNKNOWN)
                .nextCheckAt(Instant.now()) // Check immediately on creation
                .build();

        MonitoredEndpoint saved = endpointRepository.save(endpoint);
        log.info("User ID {} registered new endpoint ID {}: {}", userId, saved.getId(), saved.getName());
        return EndpointResponse.from(saved);
    }

    @Transactional
    public EndpointResponse updateEndpoint(Long id, EndpointRequest request, Long userId) {
        MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));

        // Validate URL if modified
        if (!endpoint.getUrl().equals(request.url().trim())) {
            ssrfValidator.validateUrl(request.url());
            endpoint.setUrl(request.url().trim());
        }

        endpoint.setName(request.name().trim());
        endpoint.setHttpMethod(request.httpMethod());
        endpoint.setExpectedStatusCodes(request.expectedStatusCodes().trim());
        endpoint.setCheckIntervalSeconds(request.checkIntervalSeconds());
        endpoint.setTimeoutMillis(request.timeoutMillis());
        endpoint.setLatencyThresholdMillis(request.latencyThresholdMillis());
        endpoint.setConsecutiveFailuresThreshold(request.consecutiveFailuresThreshold());

        MonitoredEndpoint updated = endpointRepository.save(endpoint);
        log.info("User ID {} updated endpoint ID {}", userId, id);
        return EndpointResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<EndpointResponse> getEndpoints(Long userId, Pageable pageable) {
        Page<MonitoredEndpoint> page = endpointRepository.findAllByUserId(userId, pageable);
        return PageResponse.from(page.map(EndpointResponse::from));
    }

    @Transactional(readOnly = true)
    public List<EndpointResponse> getAllEndpoints(Long userId) {
        return endpointRepository.findAllByUserId(userId)
                .stream()
                .map(EndpointResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EndpointResponse getEndpoint(Long id, Long userId) {
        MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));
        return EndpointResponse.from(endpoint);
    }

    @Transactional
    public EndpointResponse pauseEndpoint(Long id, Long userId) {
        MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));

        endpoint.setActive(false);
        endpoint.setCurrentStatus(EndpointStatus.PAUSED);
        MonitoredEndpoint saved = endpointRepository.save(endpoint);
        log.info("User ID {} paused endpoint ID {}", userId, id);
        return EndpointResponse.from(saved);
    }

    @Transactional
    public EndpointResponse resumeEndpoint(Long id, Long userId) {
        MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));

        endpoint.setActive(true);
        endpoint.setCurrentStatus(EndpointStatus.UNKNOWN);
        endpoint.setNextCheckAt(Instant.now());
        MonitoredEndpoint saved = endpointRepository.save(endpoint);
        log.info("User ID {} resumed endpoint ID {}", userId, id);
        return EndpointResponse.from(saved);
    }

    @Transactional
    public void deleteEndpoint(Long id, Long userId) {
        MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));

        endpointRepository.delete(endpoint);
        log.info("User ID {} deleted endpoint ID {}", userId, id);
    }

    public HealthCheckResponse triggerManualCheck(Long id, Long userId) {
        MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));

        HealthCheckResult result = monitoringScheduler.runManualCheck(endpoint.getId());
        if (result == null) {
            throw new BadRequestException("Could not execute check: Endpoint may be paused or inactive");
        }
        return HealthCheckResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PageResponse<HealthCheckResponse> getRecentChecks(Long id, Long userId, Pageable pageable) {
        // Enforce user ownership
        endpointRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", id));

        Page<HealthCheckResult> results = checkResultRepository.findAllByEndpointIdOrderByCheckedAtDesc(id, pageable);
        return PageResponse.from(results.map(HealthCheckResponse::from));
    }
}
