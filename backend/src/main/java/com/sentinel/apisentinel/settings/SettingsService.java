package com.sentinel.apisentinel.settings;

import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.settings.dto.UserSettingsRequest;
import com.sentinel.apisentinel.settings.dto.UserSettingsResponse;
import com.sentinel.apisentinel.user.User;
import com.sentinel.apisentinel.user.UserRepository;
import com.sentinel.apisentinel.user.UserSettings;
import com.sentinel.apisentinel.user.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettingsService {

    private final UserSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserSettingsResponse getSettings(Long userId) {
        UserSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return UserSettingsResponse.from(settings);
    }

    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsRequest request) {
        UserSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.setEmailNotificationsEnabled(request.emailNotificationsEnabled());
        settings.setNotificationEmail(request.notificationEmail() != null ? request.notificationEmail().trim() : null);
        settings.setDefaultCheckIntervalSeconds(request.defaultCheckIntervalSeconds());
        settings.setDefaultConsecutiveFailures(request.defaultConsecutiveFailures());

        UserSettings saved = settingsRepository.save(settings);
        log.info("Updated settings for user ID {}", userId);
        return UserSettingsResponse.from(saved);
    }

    private UserSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        UserSettings settings = UserSettings.builder()
                .user(user)
                .emailNotificationsEnabled(false)
                .notificationEmail(user.getEmail())
                .defaultCheckIntervalSeconds(60)
                .defaultConsecutiveFailures(3)
                .build();
        return settingsRepository.save(settings);
    }
}
