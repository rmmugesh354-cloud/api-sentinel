package com.sentinel.apisentinel.dashboard.dto;

public record AvailabilityMetricsResponse(
        String window,
        double availabilityPercentage,
        long totalChecks,
        long successfulChecks,
        long failedChecks,
        String calculationDescription
) {}
