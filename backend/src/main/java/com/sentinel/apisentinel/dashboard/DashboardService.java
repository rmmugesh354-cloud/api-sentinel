package com.sentinel.apisentinel.dashboard;

import com.sentinel.apisentinel.check.HealthCheckResult;
import com.sentinel.apisentinel.check.HealthCheckResultRepository;
import com.sentinel.apisentinel.check.dto.HealthCheckResponse;
import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.dashboard.dto.AvailabilityMetricsResponse;
import com.sentinel.apisentinel.dashboard.dto.DashboardSummaryResponse;
import com.sentinel.apisentinel.dashboard.dto.LatencyMetricsResponse;
import com.sentinel.apisentinel.endpoint.EndpointStatus;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import com.sentinel.apisentinel.endpoint.MonitoredEndpointRepository;
import com.sentinel.apisentinel.incident.Incident;
import com.sentinel.apisentinel.incident.IncidentRepository;
import com.sentinel.apisentinel.incident.dto.IncidentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final MonitoredEndpointRepository endpointRepository;
    private final HealthCheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        long totalEndpoints = endpointRepository.countByUserId(userId);
        long healthyEndpoints = endpointRepository.countByUserIdAndCurrentStatus(userId, EndpointStatus.HEALTHY);
        long degradedEndpoints = endpointRepository.countByUserIdAndCurrentStatus(userId, EndpointStatus.DEGRADED);
        long unhealthyEndpoints = endpointRepository.countByUserIdAndCurrentStatus(userId, EndpointStatus.UNHEALTHY);
        long pausedEndpoints = endpointRepository.countByUserIdAndCurrentStatus(userId, EndpointStatus.PAUSED);

        long openIncidents = incidentRepository.countActiveIncidentsForUser(userId);

        // 24-hour availability calculation
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long totalChecks = checkResultRepository.countUserChecksSince(userId, since24h);
        long healthyChecks = checkResultRepository.countUserHealthyChecksSince(userId, since24h);

        double overallAvailability = 100.0;
        if (totalChecks > 0) {
            overallAvailability = Math.round(((double) healthyChecks / totalChecks) * 10000.0) / 100.0;
        }

        List<HealthCheckResponse> recentFailures = checkResultRepository.findRecentUserFailures(userId, PageRequest.of(0, 10))
                .stream()
                .map(HealthCheckResponse::from)
                .toList();

        List<IncidentResponse> recentIncidents = incidentRepository.findTop10ByEndpointUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(IncidentResponse::from)
                .toList();

        return new DashboardSummaryResponse(
                totalEndpoints,
                healthyEndpoints,
                degradedEndpoints,
                unhealthyEndpoints,
                pausedEndpoints,
                openIncidents,
                overallAvailability,
                recentFailures,
                recentIncidents
        );
    }

    @Transactional(readOnly = true)
    public LatencyMetricsResponse getLatencyMetrics(Long userId, Long endpointId, String window) {
        Instant since = resolveWindowStartTime(window);

        List<Integer> sortedLatencies;
        List<LatencyMetricsResponse.LatencyPoint> timeSeries = new ArrayList<>();

        if (endpointId != null) {
            // Verify ownership
            MonitoredEndpoint endpoint = endpointRepository.findByIdAndUserId(endpointId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", endpointId));

            sortedLatencies = checkResultRepository.findEndpointResponseTimesSince(endpoint.getId(), since);

            List<HealthCheckResult> results = checkResultRepository.findAllByEndpointIdAndCheckedAtAfterOrderByCheckedAtAsc(endpoint.getId(), since);
            for (HealthCheckResult r : results) {
                timeSeries.add(new LatencyMetricsResponse.LatencyPoint(
                        r.getCheckedAt(),
                        r.getResponseTimeMs(),
                        r.isHealthy(),
                        r.getHttpStatusCode()
                ));
            }
        } else {
            sortedLatencies = checkResultRepository.findUserResponseTimesSince(userId, since);
            // Limit points to recent 100 points for aggregate chart
            List<MonitoredEndpoint> userEndpoints = endpointRepository.findAllByUserId(userId);
            for (MonitoredEndpoint ep : userEndpoints) {
                List<HealthCheckResult> epResults = checkResultRepository.findTop50ByEndpointIdOrderByCheckedAtDesc(ep.getId());
                for (HealthCheckResult r : epResults) {
                    if (r.getCheckedAt().isAfter(since)) {
                        timeSeries.add(new LatencyMetricsResponse.LatencyPoint(
                                r.getCheckedAt(),
                                r.getResponseTimeMs(),
                                r.isHealthy(),
                                r.getHttpStatusCode()
                        ));
                    }
                }
            }
            timeSeries.sort((a, b) -> a.timestamp().compareTo(b.timestamp()));
        }

        if (sortedLatencies.isEmpty()) {
            return new LatencyMetricsResponse(0, 0, 0, 0.0, 0, 0, 0, timeSeries);
        }

        int p50 = calculatePercentile(sortedLatencies, 50.0);
        int p95 = calculatePercentile(sortedLatencies, 95.0);
        int p99 = calculatePercentile(sortedLatencies, 99.0);
        int min = sortedLatencies.getFirst();
        int max = sortedLatencies.getLast();

        double sum = 0.0;
        for (int val : sortedLatencies) {
            sum += val;
        }
        double average = Math.round((sum / sortedLatencies.size()) * 10.0) / 10.0;

        return new LatencyMetricsResponse(
                p50,
                p95,
                p99,
                average,
                min,
                max,
                sortedLatencies.size(),
                timeSeries
        );
    }

    @Transactional(readOnly = true)
    public AvailabilityMetricsResponse getAvailabilityMetrics(Long userId, Long endpointId, String window) {
        Instant since = resolveWindowStartTime(window);

        long totalChecks;
        long successfulChecks;

        if (endpointId != null) {
            endpointRepository.findByIdAndUserId(endpointId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("MonitoredEndpoint", "id", endpointId));

            totalChecks = checkResultRepository.countByEndpointIdAndCheckedAtAfter(endpointId, since);
            successfulChecks = checkResultRepository.countByEndpointIdAndCheckedAtAfterAndIsHealthyTrue(endpointId, since);
        } else {
            totalChecks = checkResultRepository.countUserChecksSince(userId, since);
            successfulChecks = checkResultRepository.countUserHealthyChecksSince(userId, since);
        }

        long failedChecks = totalChecks - successfulChecks;
        double availability = 100.0;
        if (totalChecks > 0) {
            availability = Math.round(((double) successfulChecks / totalChecks) * 10000.0) / 100.0;
        }

        String description = String.format("Calculated from %d executed checks over the past %s window. Missed checks are accounted for via scheduling timestamps.",
                totalChecks, window);

        return new AvailabilityMetricsResponse(
                window,
                availability,
                totalChecks,
                successfulChecks,
                failedChecks,
                description
        );
    }

    /**
     * Exact nearest-rank percentile calculation on pre-sorted numeric list.
     */
    public static int calculatePercentile(List<Integer> sortedData, double percentile) {
        if (sortedData == null || sortedData.isEmpty()) {
            return 0;
        }
        if (percentile <= 0) {
            return sortedData.getFirst();
        }
        if (percentile >= 100) {
            return sortedData.getLast();
        }

        int n = sortedData.size();
        int index = (int) Math.ceil((percentile / 100.0) * n) - 1;
        index = Math.max(0, Math.min(index, n - 1));

        return sortedData.get(index);
    }

    private Instant resolveWindowStartTime(String window) {
        if (window == null) {
            return Instant.now().minus(24, ChronoUnit.HOURS);
        }
        return switch (window.toLowerCase().trim()) {
            case "1h" -> Instant.now().minus(1, ChronoUnit.HOURS);
            case "7d" -> Instant.now().minus(7, ChronoUnit.DAYS);
            case "30d" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default -> Instant.now().minus(24, ChronoUnit.HOURS);
        };
    }
}
