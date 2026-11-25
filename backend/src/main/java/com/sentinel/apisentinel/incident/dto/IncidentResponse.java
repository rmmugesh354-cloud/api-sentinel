package com.sentinel.apisentinel.incident.dto;

import com.sentinel.apisentinel.incident.Incident;
import com.sentinel.apisentinel.incident.IncidentStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record IncidentResponse(
        Long id,
        Long endpointId,
        String endpointName,
        String endpointUrl,
        IncidentStatus status,
        Instant startedAt,
        Instant acknowledgedAt,
        Instant resolvedAt,
        Long durationSeconds,
        String rootCause,
        String errorSummary,
        int failureCount,
        List<IncidentEventResponse> events,
        Instant createdAt,
        Instant updatedAt
) {
    public static IncidentResponse from(Incident incident) {
        Long duration = null;
        if (incident.getStartedAt() != null) {
            Instant end = incident.getResolvedAt() != null ? incident.getResolvedAt() : Instant.now();
            duration = Duration.between(incident.getStartedAt(), end).getSeconds();
        }

        List<IncidentEventResponse> eventResponses = incident.getEvents() != null
                ? incident.getEvents().stream().map(IncidentEventResponse::from).toList()
                : List.of();

        return new IncidentResponse(
                incident.getId(),
                incident.getEndpoint().getId(),
                incident.getEndpoint().getName(),
                incident.getEndpoint().getUrl(),
                incident.getStatus(),
                incident.getStartedAt(),
                incident.getAcknowledgedAt(),
                incident.getResolvedAt(),
                duration,
                incident.getRootCause(),
                incident.getErrorSummary(),
                incident.getFailureCount(),
                eventResponses,
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
