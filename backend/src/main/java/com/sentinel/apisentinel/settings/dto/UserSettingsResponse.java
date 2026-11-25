package com.sentinel.apisentinel.settings.dto;

import com.sentinel.apisentinel.user.UserSettings;

public record UserSettingsResponse(
        Long id,
        Long userId,
        boolean emailNotificationsEnabled,
        String notificationEmail,
        int defaultCheckIntervalSeconds,
        int defaultConsecutiveFailures
) {
    public static UserSettingsResponse from(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getId(),
                settings.getUser().getId(),
                settings.isEmailNotificationsEnabled(),
                settings.getNotificationEmail(),
                settings.getDefaultCheckIntervalSeconds(),
                settings.getDefaultConsecutiveFailures()
        );
    }
}
