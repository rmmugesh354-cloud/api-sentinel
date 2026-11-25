package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.endpoint.HttpMethod;
import com.sentinel.apisentinel.endpoint.MonitoredEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthPolicyEvaluatorTest {

    private HealthPolicyEvaluator evaluator;
    private MonitoredEndpoint endpoint;

    @BeforeEach
    void setUp() {
        evaluator = new HealthPolicyEvaluator();
        endpoint = MonitoredEndpoint.builder()
                .name("Production Gateway")
                .url("https://api.example.com/health")
                .httpMethod(HttpMethod.GET)
                .expectedStatusCodes("200,201,204")
                .latencyThresholdMillis(1000)
                .build();
    }

    @Test
    @DisplayName("Should evaluate healthy when status code matches and latency is within threshold")
    void testHealthyResult() {
        HttpExecutionResult result = HttpExecutionResult.success(200, 250, 1024);
        HealthPolicyEvaluator.PolicyEvaluation eval = evaluator.evaluate(endpoint, result);

        assertTrue(eval.isHealthy());
        assertTrue(eval.isWithinLatency());
        assertNull(eval.errorReason());
    }

    @Test
    @DisplayName("Should evaluate degraded when status code matches but latency exceeds threshold")
    void testDegradedLatencyExceeded() {
        HttpExecutionResult result = HttpExecutionResult.success(200, 1500, 1024);
        HealthPolicyEvaluator.PolicyEvaluation eval = evaluator.evaluate(endpoint, result);

        assertTrue(eval.isHealthy()); // functional
        assertFalse(eval.isWithinLatency()); // degraded
        assertEquals("LATENCY_THRESHOLD_EXCEEDED", eval.errorReason());
    }

    @Test
    @DisplayName("Should evaluate unhealthy when status code does not match expected codes")
    void testUnexpectedStatusCode() {
        HttpExecutionResult result = HttpExecutionResult.success(500, 120, 500);
        HealthPolicyEvaluator.PolicyEvaluation eval = evaluator.evaluate(endpoint, result);

        assertFalse(eval.isHealthy());
        assertEquals("UNEXPECTED_STATUS_CODE", eval.errorReason());
        assertTrue(eval.errorDetails().contains("Expected 200,201,204 but received HTTP 500"));
    }

    @Test
    @DisplayName("Should evaluate unhealthy on network failure (e.g. timeout)")
    void testNetworkFailure() {
        HttpExecutionResult result = HttpExecutionResult.failure(5000, "RESPONSE_TIMEOUT", "Server timed out");
        HealthPolicyEvaluator.PolicyEvaluation eval = evaluator.evaluate(endpoint, result);

        assertFalse(eval.isHealthy());
        assertFalse(eval.isWithinLatency());
        assertEquals("RESPONSE_TIMEOUT", eval.errorReason());
    }
}
