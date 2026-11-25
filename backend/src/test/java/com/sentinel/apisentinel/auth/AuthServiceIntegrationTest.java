package com.sentinel.apisentinel.auth;

import com.sentinel.apisentinel.auth.dto.AuthResponse;
import com.sentinel.apisentinel.auth.dto.LoginRequest;
import com.sentinel.apisentinel.auth.dto.RegisterRequest;
import com.sentinel.apisentinel.auth.security.JwtTokenProvider;
import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.exception.BadRequestException;
import com.sentinel.apisentinel.user.Role;
import com.sentinel.apisentinel.user.User;
import com.sentinel.apisentinel.user.UserRepository;
import com.sentinel.apisentinel.user.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should successfully register user and hash password")
    void testSuccessfulRegistration() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "jane@sentinel.io", "StrongSecretPassword123");

        when(userRepository.existsByEmail("jane@sentinel.io")).thenReturn(false);
        when(passwordEncoder.encode("StrongSecretPassword123")).thenReturn("$2a$12$hashedPassword");

        User savedUser = User.builder()
                .id(1L)
                .email("jane@sentinel.io")
                .fullName("Jane Doe")
                .passwordHash("$2a$12$hashedPassword")
                .role(Role.ROLE_USER)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(anyString(), anyLong(), anyString())).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jane@sentinel.io", response.email());
        assertEquals("mocked.jwt.token", response.token());
        assertEquals("Jane Doe", response.fullName());

        verify(passwordEncoder).encode("StrongSecretPassword123");
        verify(userRepository).save(any(User.class));
        verify(userSettingsRepository).save(any());
    }

    @Test
    @DisplayName("Should reject registration with already registered email")
    void testDuplicateEmailRegistration() {
        RegisterRequest request = new RegisterRequest("Jane Doe", "existing@sentinel.io", "StrongSecretPassword123");
        when(userRepository.existsByEmail("existing@sentinel.io")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login user and generate valid JWT token")
    void testSuccessfulLogin() {
        LoginRequest request = new LoginRequest("jane@sentinel.io", "StrongSecretPassword123");

        UserPrincipal principal = new UserPrincipal(
                1L, "jane@sentinel.io", "hashedPass", "Jane Doe",
                Collections.singletonList(() -> "ROLE_USER")
        );

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(tokenProvider.generateToken("jane@sentinel.io", 1L, "ROLE_USER")).thenReturn("valid.jwt.token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("valid.jwt.token", response.token());
        assertEquals("jane@sentinel.io", response.email());
    }

    @Test
    @DisplayName("Should fail login on bad credentials")
    void testFailedLogin() {
        LoginRequest request = new LoginRequest("jane@sentinel.io", "wrongpassword");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
