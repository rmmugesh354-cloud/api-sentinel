package com.sentinel.apisentinel.dashboard;

import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.ApiResponse;
import com.sentinel.apisentinel.dashboard.dto.AvailabilityMetricsResponse;
import com.sentinel.apisentinel.dashboard.dto.DashboardSummaryResponse;
import com.sentinel.apisentinel.dashboard.dto.LatencyMetricsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Operational metrics, availability analytics, and latency percentiles")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get high-level summary of monitored endpoints, open incidents, and 24h uptime")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        DashboardSummaryResponse response = dashboardService.getSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/latency")
    @Operation(summary = "Get response time percentiles (p50, p95, p99) and time series")
    public ResponseEntity<ApiResponse<LatencyMetricsResponse>> getLatencyMetrics(
            @RequestParam(required = false) Long endpointId,
            @RequestParam(defaultValue = "24h") String window,
            @AuthenticationPrincipal UserPrincipal principal) {
        LatencyMetricsResponse response = dashboardService.getLatencyMetrics(principal.getId(), endpointId, window);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/availability")
    @Operation(summary = "Get availability uptime percentage for a given window")
    public ResponseEntity<ApiResponse<AvailabilityMetricsResponse>> getAvailabilityMetrics(
            @RequestParam(required = false) Long endpointId,
            @RequestParam(defaultValue = "24h") String window,
            @AuthenticationPrincipal UserPrincipal principal) {
        AvailabilityMetricsResponse response = dashboardService.getAvailabilityMetrics(principal.getId(), endpointId, window);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
