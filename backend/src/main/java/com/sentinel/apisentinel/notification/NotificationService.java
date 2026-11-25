package com.sentinel.apisentinel.notification;

import com.sentinel.apisentinel.common.PageResponse;
import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.incident.Incident;
import com.sentinel.apisentinel.notification.dto.NotificationResponse;
import com.sentinel.apisentinel.user.User;
import com.sentinel.apisentinel.user.UserSettings;
import com.sentinel.apisentinel.user.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public void notifyIncidentOpened(User user, Incident incident) {
        String title = "Incident Triggered: " + incident.getEndpoint().getName() + " is failing";
        String message = String.format("Endpoint '%s' (%s) has triggered an outage incident. Root cause: %s. Consecutive failures: %d",
                incident.getEndpoint().getName(),
                incident.getEndpoint().getUrl(),
                incident.getRootCause(),
                incident.getFailureCount());

        createInAppNotification(user, incident, title, message);
        sendEmailIfEnabled(user, title, message);
    }

    @Transactional
    public void notifyIncidentResolved(User user, Incident incident) {
        String title = "Incident Resolved: " + incident.getEndpoint().getName() + " has recovered";
        String message = String.format("Endpoint '%s' (%s) has satisfied recovery health policy and returned to normal status.",
                incident.getEndpoint().getName(),
                incident.getEndpoint().getUrl());

        createInAppNotification(user, incident, title, message);
        sendEmailIfEnabled(user, title, message);
    }

    private void createInAppNotification(User user, Incident incident, String title, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .incident(incident)
                .channel(NotificationChannel.IN_APP)
                .status(NotificationStatus.SENT)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    private void sendEmailIfEnabled(User user, String title, String message) {
        Optional<UserSettings> settingsOpt = userSettingsRepository.findByUserId(user.getId());
        if (settingsOpt.isPresent() && settingsOpt.get().isEmailNotificationsEnabled()) {
            String recipient = settingsOpt.get().getNotificationEmail();
            if (recipient == null || recipient.isBlank()) {
                recipient = user.getEmail();
            }

            boolean sent = emailNotificationService.sendEmail(recipient, "[API Sentinel] " + title, message);

            Notification emailLog = Notification.builder()
                    .user(user)
                    .channel(NotificationChannel.EMAIL)
                    .status(sent ? NotificationStatus.SENT : NotificationStatus.FAILED)
                    .title(title)
                    .message(message)
                    .isRead(true)
                    .errorDetails(sent ? null : "Email delivery failed or SMTP not configured")
                    .build();
            notificationRepository.save(emailLog);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PageResponse.from(page.map(NotificationResponse::from));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepository.markAllAsReadForUser(userId);
    }
}
