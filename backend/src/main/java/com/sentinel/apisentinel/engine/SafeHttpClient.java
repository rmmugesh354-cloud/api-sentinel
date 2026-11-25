package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.endpoint.HttpMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SafeHttpClient {

    private final int maxResponseSizeBytes;

    public SafeHttpClient(
            @Value("${app.monitoring.limits.max-response-size-bytes:102400}") int maxResponseSizeBytes) {
        this.maxResponseSizeBytes = maxResponseSizeBytes;
    }

    public HttpExecutionResult execute(String targetUrl, HttpMethod method, int timeoutMillis) {
        int effectiveTimeout = Math.max(500, Math.min(timeoutMillis, 15000));
        long startNanos = System.nanoTime();
        HttpURLConnection conn = null;

        try {
            URL url = URI.create(targetUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();

            // SSRF Safety: Explicitly disallow automatic redirects so every destination is validated
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(effectiveTimeout);
            conn.setReadTimeout(effectiveTimeout);
            conn.setRequestMethod(method.name());
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; APISentinel/1.0; +https://apisentinel.dev)");
            conn.setRequestProperty("Accept", "*/*");

            if (method == HttpMethod.POST) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] emptyJson = "{}".getBytes();
                conn.getOutputStream().write(emptyJson);
            }

            int statusCode = conn.getResponseCode();
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            // Read response body up to maxResponseSizeBytes
            int bytesRead = 0;
            InputStream is = (statusCode >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is != null) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    bytesRead += read;
                    if (bytesRead >= maxResponseSizeBytes) {
                        break;
                    }
                }
                is.close();
            }

            return HttpExecutionResult.success(statusCode, latencyMs, bytesRead);

        } catch (SocketTimeoutException e) {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.debug("Timeout checking {}: {}", targetUrl, e.getMessage());
            return HttpExecutionResult.failure(latencyMs, "TIMEOUT", "Request timed out after " + effectiveTimeout + "ms");
        } catch (ConnectException e) {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.debug("Connection refused checking {}: {}", targetUrl, e.getMessage());
            return HttpExecutionResult.failure(latencyMs, "CONNECTION_REFUSED", "Connection refused by target host");
        } catch (UnknownHostException e) {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.debug("DNS lookup failed for {}: {}", targetUrl, e.getMessage());
            return HttpExecutionResult.failure(latencyMs, "DNS_FAILURE", "Failed to resolve domain name");
        } catch (Exception e) {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            log.debug("Network error checking {}: {}", targetUrl, e.getMessage());
            return HttpExecutionResult.failure(latencyMs, "NETWORK_ERROR", sanitizeMessage(e.getMessage()));
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String sanitizeMessage(String rawMessage) {
        if (rawMessage == null) return "Unknown network exception";
        return rawMessage.replaceAll("(?i)(password|secret|token|bearer)=[^&\\s]+", "$1=***");
    }
}
