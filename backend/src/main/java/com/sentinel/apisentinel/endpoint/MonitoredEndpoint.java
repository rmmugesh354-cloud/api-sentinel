package com.sentinel.apisentinel.endpoint;

import com.sentinel.apisentinel.common.Auditable;
import com.sentinel.apisentinel.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "monitored_endpoints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoredEndpoint extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    @Builder.Default
    private HttpMethod httpMethod = HttpMethod.GET;

    @Column(name = "expected_status_codes", nullable = false)
    @Builder.Default
    private String expectedStatusCodes = "200";

    @Column(name = "check_interval_seconds", nullable = false)
    @Builder.Default
    private int checkIntervalSeconds = 60;

    @Column(name = "timeout_millis", nullable = false)
    @Builder.Default
    private int timeoutMillis = 5000;

    @Column(name = "latency_threshold_millis", nullable = false)
    @Builder.Default
    private int latencyThresholdMillis = 2000;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "consecutive_failures_threshold", nullable = false)
    @Builder.Default
    private int consecutiveFailuresThreshold = 3;

    @Column(name = "consecutive_failures_count", nullable = false)
    @Builder.Default
    private int consecutiveFailuresCount = 0;

    @Column(name = "consecutive_successes_count", nullable = false)
    @Builder.Default
    private int consecutiveSuccessesCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 50)
    @Builder.Default
    private EndpointStatus currentStatus = EndpointStatus.UNKNOWN;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "next_check_at", nullable = false)
    @Builder.Default
    private Instant nextCheckAt = Instant.now();
}
