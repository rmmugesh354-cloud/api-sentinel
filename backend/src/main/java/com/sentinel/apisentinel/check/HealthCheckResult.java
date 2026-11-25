package com.sentinel.apisentinel.check;

import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "health_check_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private MonitoredEndpoint endpoint;

    @Column(name = "checked_at", nullable = false)
    @Builder.Default
    private Instant checkedAt = Instant.now();

    @Column(name = "response_time_ms", nullable = false)
    private int responseTimeMs;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "is_healthy", nullable = false)
    private boolean isHealthy;

    @Column(name = "error_reason")
    private String errorReason;

    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    @Column(name = "response_size_bytes")
    @Builder.Default
    private int responseSizeBytes = 0;
}
