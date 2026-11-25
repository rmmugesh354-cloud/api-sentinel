package com.sentinel.apisentinel.incident;

import com.sentinel.apisentinel.auth.security.UserPrincipal;
import com.sentinel.apisentinel.common.ApiResponse;
import com.sentinel.apisentinel.common.PageResponse;
import com.sentinel.apisentinel.incident.dto.IncidentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents", description = "Operational incidents and failure timeline")
public class IncidentController {

    private final IncidentService incidentService;

    @GetMapping
    @Operation(summary = "Get paginated incidents with optional status filter")
    public ResponseEntity<ApiResponse<PageResponse<IncidentResponse>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<IncidentResponse> response = incidentService.getIncidents(principal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get incident details and event timeline")
    public ResponseEntity<ApiResponse<IncidentResponse>> getIncident(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        IncidentResponse response = incidentService.getIncident(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/acknowledge")
    @Operation(summary = "Acknowledge an open incident")
    public ResponseEntity<ApiResponse<IncidentResponse>> acknowledge(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        IncidentResponse response = incidentService.acknowledgeIncident(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Incident acknowledged", response));
    }
}
