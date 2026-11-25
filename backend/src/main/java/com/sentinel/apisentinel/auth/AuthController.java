package com.sentinel.apisentinel.auth;

import com.sentinel.apisentinel.auth.dto.AuthResponse;
import com.sentinel.apisentinel.auth.dto.LoginRequest;
import com.sentinel.apisentinel.auth.dto.RegisterRequest;
import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, authentication and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<AuthResponse>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        AuthResponse response = authService.getCurrentUser(principal);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and invalidate client session")
    public ResponseEntity<ApiResponse<String>> logout() {
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", "Session cleared"));
    }
}
