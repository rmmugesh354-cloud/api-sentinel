package com.sentinel.apisentinel.incident;

import com.sentinel.apisentinel.common.Auditable;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private MonitoredEndpoint endpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "root_cause", nullable = false)
    private String rootCause;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private int failureCount = 1;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<IncidentEvent> events = new ArrayList<>();

    public void addEvent(IncidentEventType eventType, String description) {
        IncidentEvent event = IncidentEvent.builder()
                .incident(this)
                .eventType(eventType)
                .description(description)
                .createdAt(Instant.now())
                .build();
        events.add(event);
    }
}
