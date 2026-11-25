package com.sentinel.apisentinel.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String fromAddress;

    public EmailNotificationService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${app.email.enabled:false}") boolean emailEnabled,
            @Value("${app.email.from:alerts@apisentinel.dev}") String fromAddress) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
    }

    public boolean isConfigured() {
        return emailEnabled && mailSender != null;
    }

    public boolean sendEmail(String recipient, String subject, String body) {
        if (!isConfigured()) {
            log.info("Email notifications not configured or disabled; skipped sending alert to {}", recipient);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("Successfully dispatched alert email to {}", recipient);
            return true;
        } catch (Exception ex) {
            log.warn("Failed to dispatch alert email to {}: {}", recipient, ex.getMessage());
            return false;
        }
    }
}
