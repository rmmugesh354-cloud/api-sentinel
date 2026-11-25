package com.sentinel.apisentinel.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserSettingsRequest(
        @NotNull(message = "Email notifications toggle is required")
        Boolean emailNotificationsEnabled,

        @Email(message = "Notification email must be valid")
        String notificationEmail,

        @NotNull(message = "Default check interval is required")
        @Min(value = 10, message = "Interval must be at least 10s")
        @Max(value = 86400, message = "Interval cannot exceed 86400s")
        Integer defaultCheckIntervalSeconds,

        @NotNull(message = "Default consecutive failures threshold is required")
        @Min(value = 1, message = "Failure threshold must be at least 1")
        @Max(value = 10, message = "Failure threshold cannot exceed 10")
        Integer defaultConsecutiveFailures
) {}
