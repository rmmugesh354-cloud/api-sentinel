package com.sentinel.apisentinel.auth;

import com.sentinel.apisentinel.auth.dto.AuthResponse;
import com.sentinel.apisentinel.auth.dto.LoginRequest;
import com.sentinel.apisentinel.auth.dto.RegisterRequest;
import com.sentinel.apisentinel.auth.security.JwtTokenProvider;
import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.exception.BadRequestException;
import com.sentinel.apisentinel.common.exception.ResourceNotFoundException;
import com.sentinel.apisentinel.user.Role;
import com.sentinel.apisentinel.user.User;
import com.sentinel.apisentinel.user.UserRepository;
import com.sentinel.apisentinel.user.UserSettings;
import com.sentinel.apisentinel.user.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("An account with email " + normalizedEmail + " already exists");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .fullName(request.fullName().trim())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);

        // Create default settings for the newly registered user
        UserSettings defaultSettings = UserSettings.builder()
                .user(savedUser)
                .emailNotificationsEnabled(false)
                .notificationEmail(savedUser.getEmail())
                .defaultCheckIntervalSeconds(60)
                .defaultConsecutiveFailures(3)
                .build();
        userSettingsRepository.save(defaultSettings);

        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());

        log.info("Registered new user with ID: {}", savedUser.getId());
        return AuthResponse.of(token, savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), savedUser.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String role = principal.getAuthorities().iterator().next().getAuthority();
        String token = tokenProvider.generateToken(principal.getEmail(), principal.getId(), role);

        log.info("User logged in successfully: {}", principal.getEmail());
        return AuthResponse.of(token, principal.getId(), principal.getEmail(), principal.getFullName(), role);
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", principal.getId()));

        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return AuthResponse.of(token, user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
