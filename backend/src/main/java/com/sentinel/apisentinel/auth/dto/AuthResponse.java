package com.sentinel.apisentinel.auth.dto;

public record AuthResponse(
        String token,
        String type,
        Long id,
        String email,
        String fullName,
        String role
) {
    public static AuthResponse of(String token, Long id, String email, String fullName, String role) {
        return new AuthResponse(token, "Bearer", id, email, fullName, role);
    }
}
