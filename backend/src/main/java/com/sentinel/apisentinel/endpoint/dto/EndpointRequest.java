package com.sentinel.apisentinel.endpoint.dto;

import com.sentinel.apisentinel.endpoint.HttpMethod;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EndpointRequest(
        @NotBlank(message = "Endpoint name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,

        @NotBlank(message = "Endpoint URL is required")
        @Pattern(regexp = "^(https?):\\/\\/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", 
                 message = "URL must be a valid HTTP or HTTPS address")
        @Size(max = 2048, message = "URL cannot exceed 2048 characters")
        String url,

        @NotNull(message = "HTTP method is required")
        HttpMethod httpMethod,

        @NotBlank(message = "Expected status codes are required (e.g. 200, 201, 204)")
        @Pattern(regexp = "^[0-9]{3}(,[0-9]{3})*$", message = "Expected status codes must be comma-separated 3-digit integers (e.g., '200' or '200,201,204')")
        String expectedStatusCodes,

        @NotNull(message = "Check interval is required")
        @Min(value = 10, message = "Check interval must be at least 10 seconds")
        @Max(value = 86400, message = "Check interval cannot exceed 86400 seconds (24 hours)")
        Integer checkIntervalSeconds,

        @NotNull(message = "Timeout is required")
        @Min(value = 500, message = "Timeout must be at least 500 ms")
        @Max(value = 15000, message = "Timeout cannot exceed 15000 ms")
        Integer timeoutMillis,

        @NotNull(message = "Latency threshold is required")
        @Min(value = 100, message = "Latency threshold must be at least 100 ms")
        @Max(value = 30000, message = "Latency threshold cannot exceed 30000 ms")
        Integer latencyThresholdMillis,

        @NotNull(message = "Consecutive failures threshold is required")
        @Min(value = 1, message = "Consecutive failures threshold must be at least 1")
        @Max(value = 10, message = "Consecutive failures threshold cannot exceed 10")
        Integer consecutiveFailuresThreshold
) {}
