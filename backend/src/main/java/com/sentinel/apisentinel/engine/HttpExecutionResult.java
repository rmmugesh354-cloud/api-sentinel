package com.sentinel.apisentinel.engine;

public record HttpExecutionResult(
        Integer httpStatusCode,
        long latencyMs,
        int responseSizeBytes,
        boolean isNetworkSuccess,
        String errorType,
        String errorMessage
) {
    public static HttpExecutionResult success(int httpStatusCode, long latencyMs, int responseSizeBytes) {
        return new HttpExecutionResult(httpStatusCode, latencyMs, responseSizeBytes, true, null, null);
    }

    public static HttpExecutionResult failure(long latencyMs, String errorType, String errorMessage) {
        return new HttpExecutionResult(null, latencyMs, 0, false, errorType, errorMessage);
    }

    public static HttpExecutionResult failure(int httpStatusCode, long latencyMs, int responseSizeBytes, String errorType, String errorMessage) {
        return new HttpExecutionResult(httpStatusCode, latencyMs, responseSizeBytes, false, errorType, errorMessage);
    }
}
