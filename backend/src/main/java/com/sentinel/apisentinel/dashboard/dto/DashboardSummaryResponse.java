package com.sentinel.apisentinel.dashboard.dto;

import com.sentinel.apisentinel.check.dto.HealthCheckResponse;
import com.sentinel.apisentinel.incident.dto.IncidentResponse;

import java.util.List;

public record DashboardSummaryResponse(
        long totalEndpoints,
        long healthyEndpoints,
        long degradedEndpoints,
        long unhealthyEndpoints,
        long pausedEndpoints,
        long openIncidents,
        double overallAvailabilityPercentage,
        List<HealthCheckResponse> recentFailures,
        List<IncidentResponse> recentIncidents
) {}
