package com.sentinel.apisentinel.user;

import com.sentinel.apisentinel.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean emailNotificationsEnabled = false;

    @Column(name = "notification_email")
    private String notificationEmail;

    @Column(name = "default_check_interval_seconds", nullable = false)
    @Builder.Default
    private int defaultCheckIntervalSeconds = 60;

    @Column(name = "default_consecutive_failures", nullable = false)
    @Builder.Default
    private int defaultConsecutiveFailures = 3;
}
