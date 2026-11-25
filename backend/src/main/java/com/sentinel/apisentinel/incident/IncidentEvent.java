package com.sentinel.apisentinel.incident;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "incident_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private IncidentEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
