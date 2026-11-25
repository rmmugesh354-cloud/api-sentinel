package com.sentinel.apisentinel.incident.dto;

import com.sentinel.apisentinel.incident.IncidentEvent;
import com.sentinel.apisentinel.incident.IncidentEventType;

import java.time.Instant;

public record IncidentEventResponse(
        Long id,
        Long incidentId,
        IncidentEventType eventType,
        String description,
        Instant createdAt
) {
    public static IncidentEventResponse from(IncidentEvent event) {
        return new IncidentEventResponse(
                event.getId(),
                event.getIncident().getId(),
                event.getEventType(),
                event.getDescription(),
                event.getCreatedAt()
        );
    }
}
