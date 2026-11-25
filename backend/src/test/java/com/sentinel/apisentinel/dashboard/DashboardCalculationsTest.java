package com.sentinel.apisentinel.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DashboardCalculationsTest {

    @Test
    @DisplayName("Should return 0 for empty or null latency list")
    void testEmptyList() {
        assertEquals(0, DashboardService.calculatePercentile(Collections.emptyList(), 50.0));
        assertEquals(0, DashboardService.calculatePercentile(null, 95.0));
    }

    @Test
    @DisplayName("Should calculate exact nearest-rank percentiles p50, p95, p99")
    void testPercentileCalculations() {
        // 100 values from 1 to 100
        List<Integer> values = new java.util.ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            values.add(i);
        }

        // p50 for 100 elements should be 50
        assertEquals(50, DashboardService.calculatePercentile(values, 50.0));

        // p95 for 100 elements should be 95
        assertEquals(95, DashboardService.calculatePercentile(values, 95.0));

        // p99 for 100 elements should be 99
        assertEquals(99, DashboardService.calculatePercentile(values, 99.0));
    }

    @Test
    @DisplayName("Should handle single element correctly")
    void testSingleElement() {
        List<Integer> single = List.of(42);
        assertEquals(42, DashboardService.calculatePercentile(single, 50.0));
        assertEquals(42, DashboardService.calculatePercentile(single, 95.0));
        assertEquals(42, DashboardService.calculatePercentile(single, 99.0));
    }
}
