package com.sentinel.apisentinel.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record LatencyMetricsResponse(
        Integer p50Ms,
        Integer p95Ms,
        Integer p99Ms,
        Double averageMs,
        Integer minMs,
        Integer maxMs,
        long sampleCount,
        List<LatencyPoint> timeSeries
) {
    public record LatencyPoint(
            Instant timestamp,
            int latencyMs,
            boolean isHealthy,
            Integer statusCode
    ) {}
}
