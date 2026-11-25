package com.sentinel.apisentinel.check.dto;

import com.sentinel.apisentinel.check.HealthCheckResult;

import java.time.Instant;

public record HealthCheckResponse(
        Long id,
        Long endpointId,
        Instant checkedAt,
        int responseTimeMs,
        Integer httpStatusCode,
        boolean isHealthy,
        String errorReason,
        String errorDetails,
        int responseSizeBytes
) {
    public static HealthCheckResponse from(HealthCheckResult result) {
        return new HealthCheckResponse(
                result.getId(),
                result.getEndpoint().getId(),
                result.getCheckedAt(),
                result.getResponseTimeMs(),
                result.getHttpStatusCode(),
                result.isHealthy(),
                result.getErrorReason(),
                result.getErrorDetails(),
                result.getResponseSizeBytes()
        );
    }
}
