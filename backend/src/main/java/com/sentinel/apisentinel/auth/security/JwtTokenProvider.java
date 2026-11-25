package com.sentinel.apisentinel.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(ensureBase64(jwtSecret));
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpirationMs = jwtExpirationMs;
    }

    private static String ensureBase64(String secret) {
        // In case the raw secret is a plain string, convert to base64 if not already
        try {
            Decoders.BASE64.decode(secret);
            return secret;
        } catch (Exception e) {
            return java.util.Base64.getEncoder().encodeToString(secret.getBytes());
        }
    }

    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return generateToken(userPrincipal.getEmail(), userPrincipal.getId(), 
                userPrincipal.getAuthorities().iterator().next().getAuthority());
    }

    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("userId", Long.class);
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token format");
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty");
        } catch (SecurityException ex) {
            log.warn("Invalid JWT signature");
        }
        return false;
    }
}
