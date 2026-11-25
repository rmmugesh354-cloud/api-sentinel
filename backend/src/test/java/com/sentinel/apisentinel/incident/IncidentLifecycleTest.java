package com.sentinel.apisentinel.incident;

import com.sentinel.apisentinel.endpoint.EndpointStatus;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import com.sentinel.apisentinel.notification.NotificationService;
import com.sentinel.apisentinel.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentLifecycleTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private IncidentService incidentService;

    private User user;
    private MonitoredEndpoint endpoint;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("dev@sentinel.io").fullName("Test Dev").build();
        endpoint = MonitoredEndpoint.builder()
                .id(10L)
                .user(user)
                .name("Payments API")
                .url("https://payments.example.com/health")
                .consecutiveFailuresThreshold(3)
                .consecutiveFailuresCount(0)
                .consecutiveSuccessesCount(0)
                .currentStatus(EndpointStatus.HEALTHY)
                .build();
    }

    @Test
    @DisplayName("Should mark endpoint DEGRADED on failures below threshold without creating incident")
    void testFailuresBelowThreshold() {
        incidentService.handleFailure(endpoint, "TIMEOUT", "Timeout occurred");

        assertEquals(1, endpoint.getConsecutiveFailuresCount());
        assertEquals(EndpointStatus.DEGRADED, endpoint.getCurrentStatus());
        verify(incidentRepository, never()).save(any());
        verify(notificationService, never()).notifyIncidentOpened(any(), any());
    }

    @Test
    @DisplayName("Should create OPEN incident when consecutive failures reach threshold")
    void testIncidentCreationAtThreshold() {
        endpoint.setConsecutiveFailuresCount(2); // Next failure reaches 3

        when(incidentRepository.findFirstByEndpointIdAndStatusIn(eq(10L), any()))
                .thenReturn(Optional.empty());

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident inc = invocation.getArgument(0);
            inc.setId(100L);
            return inc;
        });

        incidentService.handleFailure(endpoint, "HTTP_500", "Internal Server Error");

        assertEquals(3, endpoint.getConsecutiveFailuresCount());
        assertEquals(EndpointStatus.UNHEALTHY, endpoint.getCurrentStatus());

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());

        Incident captured = incidentCaptor.getValue();
        assertEquals(IncidentStatus.OPEN, captured.getStatus());
        assertEquals("HTTP_500", captured.getRootCause());
        assertEquals(3, captured.getFailureCount());
        assertNotNull(captured.getStartedAt());
        assertNull(captured.getResolvedAt());

        verify(notificationService).notifyIncidentOpened(eq(user), any(Incident.class));
    }

    @Test
    @DisplayName("Should NOT create duplicate incident if one is already OPEN or ACKNOWLEDGED")
    void testDeduplicateOpenIncidents() {
        endpoint.setConsecutiveFailuresCount(3);

        Incident openIncident = Incident.builder()
                .id(100L)
                .endpoint(endpoint)
                .status(IncidentStatus.OPEN)
                .failureCount(3)
                .build();

        when(incidentRepository.findFirstByEndpointIdAndStatusIn(eq(10L), any()))
                .thenReturn(Optional.of(openIncident));

        incidentService.handleFailure(endpoint, "HTTP_500", "Repeated failure");

        assertEquals(4, endpoint.getConsecutiveFailuresCount());
        assertEquals(4, openIncident.getFailureCount());

        verify(incidentRepository).save(openIncident);
        // Ensure new incident notification was NOT fired again
        verify(notificationService, never()).notifyIncidentOpened(any(), any());
    }

    @Test
    @DisplayName("Should automatically resolve active incident when endpoint recovers")
    void testAutomaticIncidentResolution() {
        endpoint.setConsecutiveFailuresCount(3);
        endpoint.setCurrentStatus(EndpointStatus.UNHEALTHY);

        Incident openIncident = Incident.builder()
                .id(100L)
                .endpoint(endpoint)
                .status(IncidentStatus.OPEN)
                .failureCount(3)
                .build();

        when(incidentRepository.findFirstByEndpointIdAndStatusIn(eq(10L), any()))
                .thenReturn(Optional.of(openIncident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(openIncident);

        incidentService.handleSuccess(endpoint, true);

        assertEquals(0, endpoint.getConsecutiveFailuresCount());
        assertEquals(1, endpoint.getConsecutiveSuccessesCount());
        assertEquals(EndpointStatus.HEALTHY, endpoint.getCurrentStatus());

        assertEquals(IncidentStatus.RESOLVED, openIncident.getStatus());
        assertNotNull(openIncident.getResolvedAt());

        verify(notificationService).notifyIncidentResolved(eq(user), eq(openIncident));
    }
}
