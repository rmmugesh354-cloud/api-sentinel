package com.sentinel.apisentinel.endpoint;

import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.check.dto.HealthCheckResponse;
import com.sentinel.apisentinel.common.ApiResponse;
import com.sentinel.apisentinel.common.PageResponse;
import com.sentinel.apisentinel.endpoint.dto.EndpointRequest;
import com.sentinel.apisentinel.endpoint.dto.EndpointResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/endpoints")
@RequiredArgsConstructor
@Tag(name = "Endpoints", description = "Monitored endpoint management and manual checks")
public class EndpointController {

    private final EndpointService endpointService;

    @GetMapping
    @Operation(summary = "Get paginated list of monitored endpoints for current user")
    public ResponseEntity<ApiResponse<PageResponse<EndpointResponse>>> getEndpoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<EndpointResponse> response = endpointService.getEndpoints(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all monitored endpoints for current user (for filters and selectors)")
    public ResponseEntity<ApiResponse<List<EndpointResponse>>> getAllEndpoints(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<EndpointResponse> response = endpointService.getAllEndpoints(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    @Operation(summary = "Register a new endpoint for automated monitoring")
    public ResponseEntity<ApiResponse<EndpointResponse>> createEndpoint(
            @Valid @RequestBody EndpointRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        EndpointResponse response = endpointService.createEndpoint(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Endpoint registered successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a specific monitored endpoint")
    public ResponseEntity<ApiResponse<EndpointResponse>> getEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        EndpointResponse response = endpointService.getEndpoint(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update configuration of an existing endpoint")
    public ResponseEntity<ApiResponse<EndpointResponse>> updateEndpoint(
            @PathVariable Long id,
            @Valid @RequestBody EndpointRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        EndpointResponse response = endpointService.updateEndpoint(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Endpoint updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an endpoint and all its historical results and incidents")
    public ResponseEntity<ApiResponse<String>> deleteEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        endpointService.deleteEndpoint(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Endpoint deleted successfully", null));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause monitoring for an endpoint")
    public ResponseEntity<ApiResponse<EndpointResponse>> pauseEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        EndpointResponse response = endpointService.pauseEndpoint(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Monitoring paused", response));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume monitoring for a paused endpoint")
    public ResponseEntity<ApiResponse<EndpointResponse>> resumeEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        EndpointResponse response = endpointService.resumeEndpoint(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Monitoring resumed", response));
    }

    @PostMapping("/{id}/check")
    @Operation(summary = "Trigger an immediate manual health check")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> triggerManualCheck(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        HealthCheckResponse response = endpointService.triggerManualCheck(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Manual check completed", response));
    }

    @GetMapping("/{id}/checks")
    @Operation(summary = "Get historical check records for an endpoint")
    public ResponseEntity<ApiResponse<PageResponse<HealthCheckResponse>>> getRecentChecks(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<HealthCheckResponse> response = endpointService.getRecentChecks(id, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
