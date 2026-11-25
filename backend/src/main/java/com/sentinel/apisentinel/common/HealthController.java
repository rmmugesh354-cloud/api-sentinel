package com.sentinel.apisentinel.common;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health", description = "Application readiness and liveness probe")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check backend liveness and system status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "api-sentinel",
                "version", "1.0.0",
                "timestamp", Instant.now().toString()
        )));
    }
}
