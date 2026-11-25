package com.sentinel.apisentinel.notification.dto;

import com.sentinel.apisentinel.notification.Notification;
import com.sentinel.apisentinel.notification.NotificationChannel;
import com.sentinel.apisentinel.notification.NotificationStatus;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        Long incidentId,
        NotificationChannel channel,
        NotificationStatus status,
        String title,
        String message,
        boolean isRead,
        Instant createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getIncident() != null ? notification.getIncident().getId() : null,
                notification.getChannel(),
                notification.getStatus(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
