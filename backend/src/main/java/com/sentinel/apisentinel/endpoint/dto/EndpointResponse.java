package com.sentinel.apisentinel.endpoint.dto;

import com.sentinel.apisentinel.endpoint.EndpointStatus;
import com.sentinel.apisentinel.endpoint.HttpMethod;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;

import java.time.Instant;

public record EndpointResponse(
        Long id,
        Long userId,
        String name,
        String url,
        HttpMethod httpMethod,
        String expectedStatusCodes,
        int checkIntervalSeconds,
        int timeoutMillis,
        int latencyThresholdMillis,
        boolean isActive,
        int consecutiveFailuresThreshold,
        int consecutiveFailuresCount,
        int consecutiveSuccessesCount,
        EndpointStatus currentStatus,
        Instant lastCheckedAt,
        Instant nextCheckAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static EndpointResponse from(MonitoredEndpoint endpoint) {
        return new EndpointResponse(
                endpoint.getId(),
                endpoint.getUser().getId(),
                endpoint.getName(),
                endpoint.getUrl(),
                endpoint.getHttpMethod(),
                endpoint.getExpectedStatusCodes(),
                endpoint.getCheckIntervalSeconds(),
                endpoint.getTimeoutMillis(),
                endpoint.getLatencyThresholdMillis(),
                endpoint.isActive(),
                endpoint.getConsecutiveFailuresThreshold(),
                endpoint.getConsecutiveFailuresCount(),
                endpoint.getConsecutiveSuccessesCount(),
                endpoint.getCurrentStatus(),
                endpoint.getLastCheckedAt(),
                endpoint.getNextCheckAt(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt()
        );
    }
}
