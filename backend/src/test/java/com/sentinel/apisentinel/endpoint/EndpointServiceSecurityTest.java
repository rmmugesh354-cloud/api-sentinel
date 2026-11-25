package com.sentinel.apisentinel.endpoint;

import com.sentinel.apisentinel.check.HealthCheckResultRepository;
import com.sentinel.apisentinel.common.exception.BadRequestException;
import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.endpoint.dto.EndpointRequest;
import com.sentinel.apisentinel.engine.MonitoringScheduler;
import com.sentinel.apisentinel.engine.SsrfValidator;
import com.sentinel.apisentinel.user.Role;
import com.sentinel.apisentinel.user.User;
import com.sentinel.apisentinel.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndpointServiceSecurityTest {

    @Mock
    private MonitoredEndpointRepository endpointRepository;

    @Mock
    private HealthCheckResultRepository checkResultRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SsrfValidator ssrfValidator;

    @Mock
    private MonitoringScheduler monitoringScheduler;

    @InjectMocks
    private EndpointService endpointService;

    private User userA;
    private User userB;
    private MonitoredEndpoint endpointA;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(endpointService, "maxEndpointsPerUser", 50);

        userA = User.builder().id(1L).email("userA@sentinel.io").fullName("User A").role(Role.ROLE_USER).build();
        userB = User.builder().id(2L).email("userB@sentinel.io").fullName("User B").role(Role.ROLE_USER).build();

        endpointA = MonitoredEndpoint.builder()
                .id(100L)
                .user(userA)
                .name("User A Production")
                .url("https://api.usera.com/health")
                .httpMethod(HttpMethod.GET)
                .expectedStatusCodes("200")
                .checkIntervalSeconds(60)
                .timeoutMillis(5000)
                .latencyThresholdMillis(2000)
                .consecutiveFailuresThreshold(3)
                .build();
    }

    @Test
    @DisplayName("Should prevent cross-tenant access when User B attempts to access User A's endpoint")
    void testCrossTenantAccessDeniedOnGet() {
        // Repository filters by ID and User ID
        when(endpointRepository.findByIdAndUserId(eq(100L), eq(2L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> endpointService.getEndpoint(100L, 2L));
    }

    @Test
    @DisplayName("Should prevent cross-tenant modification when User B attempts to update User A's endpoint")
    void testCrossTenantModificationDenied() {
        when(endpointRepository.findByIdAndUserId(eq(100L), eq(2L))).thenReturn(Optional.empty());

        EndpointRequest updateReq = new EndpointRequest(
                "Malicious Rename", "https://hacked.com", HttpMethod.GET, "200", 60, 5000, 2000, 3
        );

        assertThrows(ResourceNotFoundException.class, () -> endpointService.updateEndpoint(100L, updateReq, 2L));
        verify(endpointRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should prevent cross-tenant deletion when User B attempts to delete User A's endpoint")
    void testCrossTenantDeletionDenied() {
        when(endpointRepository.findByIdAndUserId(eq(100L), eq(2L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> endpointService.deleteEndpoint(100L, 2L));
        verify(endpointRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should enforce endpoint quota limit per user")
    void testQuotaLimitEnforcement() {
        when(endpointRepository.countByUserId(1L)).thenReturn(50L);

        EndpointRequest req = new EndpointRequest(
                "Excess Endpoint", "https://api.example.com", HttpMethod.GET, "200", 60, 5000, 2000, 3
        );

        assertThrows(BadRequestException.class, () -> endpointService.createEndpoint(req, 1L));
        verify(endpointRepository, never()).save(any());
    }
}
