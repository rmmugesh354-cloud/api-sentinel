package com.sentinel.apisentinel.incident;

import com.sentinel.apisentinel.common.PageResponse;
import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.endpoint.EndpointStatus;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import com.sentinel.apisentinel.incident.dto.IncidentResponse;
import com.sentinel.apisentinel.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    @Transactional
    public void handleFailure(MonitoredEndpoint endpoint, String cause, String errorDetails) {
        int failures = endpoint.getConsecutiveFailuresCount() + 1;
        endpoint.setConsecutiveFailuresCount(failures);
        endpoint.setConsecutiveSuccessesCount(0);

        if (failures >= endpoint.getConsecutiveFailuresThreshold()) {
            endpoint.setCurrentStatus(EndpointStatus.UNHEALTHY);

            // Check if active incident already exists (OPEN or ACKNOWLEDGED) to prevent duplicates
            Optional<Incident> activeIncidentOpt = incidentRepository.findFirstByEndpointIdAndStatusIn(
                    endpoint.getId(), List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED));

            if (activeIncidentOpt.isEmpty()) {
                Incident incident = Incident.builder()
                        .endpoint(endpoint)
                        .status(IncidentStatus.OPEN)
                        .startedAt(Instant.now())
                        .rootCause(cause != null ? cause : "Unknown outage")
                        .errorSummary(errorDetails)
                        .failureCount(failures)
                        .build();

                incident.addEvent(IncidentEventType.INCIDENT_OPENED,
                        "Incident opened after " + failures + " consecutive check failures: " + cause);

                Incident savedIncident = incidentRepository.save(incident);
                log.info("Opened new incident ID {} for endpoint '{}'", savedIncident.getId(), endpoint.getName());

                notificationService.notifyIncidentOpened(endpoint.getUser(), savedIncident);
            } else {
                Incident existing = activeIncidentOpt.get();
                existing.setFailureCount(existing.getFailureCount() + 1);
                existing.setErrorSummary(errorDetails);
                existing.addEvent(IncidentEventType.FAILURE_RECORDED,
                        "Recurring failure recorded (count " + existing.getFailureCount() + "): " + cause);
                incidentRepository.save(existing);
            }
        } else {
            endpoint.setCurrentStatus(EndpointStatus.DEGRADED);
        }
    }

    @Transactional
    public void handleSuccess(MonitoredEndpoint endpoint, boolean withinLatency) {
        endpoint.setConsecutiveSuccessesCount(endpoint.getConsecutiveSuccessesCount() + 1);
        endpoint.setConsecutiveFailuresCount(0);
        endpoint.setCurrentStatus(withinLatency ? EndpointStatus.HEALTHY : EndpointStatus.DEGRADED);

        // If an open or acknowledged incident exists, automatically resolve it
        Optional<Incident> activeIncidentOpt = incidentRepository.findFirstByEndpointIdAndStatusIn(
                endpoint.getId(), List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED));

        if (activeIncidentOpt.isPresent()) {
            Incident incident = activeIncidentOpt.get();
            incident.setStatus(IncidentStatus.RESOLVED);
            incident.setResolvedAt(Instant.now());
            incident.addEvent(IncidentEventType.INCIDENT_RESOLVED,
                    "Endpoint passed health checks and successfully recovered");

            Incident saved = incidentRepository.save(incident);
            log.info("Automatically resolved incident ID {} for endpoint '{}'", saved.getId(), endpoint.getName());

            notificationService.notifyIncidentResolved(endpoint.getUser(), saved);
        }
    }

    @Transactional
    public IncidentResponse acknowledgeIncident(Long incidentId, Long userId) {
        Incident incident = incidentRepository.findByIdAndEndpointUserId(incidentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));

        if (incident.getStatus() == IncidentStatus.OPEN) {
            incident.setStatus(IncidentStatus.ACKNOWLEDGED);
            incident.setAcknowledgedAt(Instant.now());
            incident.addEvent(IncidentEventType.INCIDENT_ACKNOWLEDGED, "Incident acknowledged by user");
            incident = incidentRepository.save(incident);
            log.info("Incident ID {} acknowledged by user ID {}", incidentId, userId);
        }

        return IncidentResponse.from(incident);
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> getIncidents(Long userId, IncidentStatus status, Pageable pageable) {
        Page<Incident> page;
        if (status != null) {
            page = incidentRepository.findAllByEndpointUserIdAndStatus(userId, status, pageable);
        } else {
            page = incidentRepository.findAllByEndpointUserId(userId, pageable);
        }
        return PageResponse.from(page.map(IncidentResponse::from));
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(Long incidentId, Long userId) {
        Incident incident = incidentRepository.findByIdAndEndpointUserId(incidentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident", "id", incidentId));
        return IncidentResponse.from(incident);
    }
}
