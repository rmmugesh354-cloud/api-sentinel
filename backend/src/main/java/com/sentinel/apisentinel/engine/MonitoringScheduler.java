package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.check.HealthCheckResult;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import com.sentinel.apisentinel.endpoint.MonitoredEndpointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringScheduler {

    private final MonitoredEndpointRepository endpointRepository;
    private final HealthCheckWorker healthCheckWorker;
    private final ThreadPoolTaskExecutor healthCheckExecutor;

    // Concurrency control: prevents overlapping or duplicate in-flight checks for the same endpoint
    private final Set<Long> inFlightEndpointIds = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${app.monitoring.scheduler.fixed-delay-ms:5000}")
    public void scheduleDueChecks() {
        try {
            Instant now = Instant.now();
            List<MonitoredEndpoint> dueEndpoints = endpointRepository.findDueEndpoints(now, PageRequest.of(0, 50));

            for (MonitoredEndpoint endpoint : dueEndpoints) {
                Long endpointId = endpoint.getId();

                if (inFlightEndpointIds.add(endpointId)) {
                    log.debug("Dispatching check for endpoint ID {}: {}", endpointId, endpoint.getName());
                    healthCheckExecutor.execute(() -> {
                        try {
                            healthCheckWorker.runCheck(endpointId);
                        } catch (Exception e) {
                            log.error("Unhandled worker error for endpoint ID {}: {}", endpointId, e.getMessage(), e);
                        } finally {
                            inFlightEndpointIds.remove(endpointId);
                        }
                    });
                } else {
                    log.debug("Check already in-flight for endpoint ID {}, skipping duplicate execution", endpointId);
                }
            }
        } catch (Exception ex) {
            log.error("Scheduler encountered an error polling due endpoints: {}", ex.getMessage());
        }
    }

    public HealthCheckResult runManualCheck(Long endpointId) {
        if (!inFlightEndpointIds.add(endpointId)) {
            log.info("Manual check requested for endpoint ID {}, but check is already in-flight", endpointId);
        }
        try {
            return healthCheckWorker.runCheck(endpointId);
        } finally {
            inFlightEndpointIds.remove(endpointId);
        }
    }

    public boolean isEndpointInFlight(Long endpointId) {
        return inFlightEndpointIds.contains(endpointId);
    }
}
