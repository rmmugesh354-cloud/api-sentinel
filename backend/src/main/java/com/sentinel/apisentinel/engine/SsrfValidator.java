package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.common.exception.SsrfSecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

@Component
@Slf4j
public class SsrfValidator {

    @Value("${app.monitoring.ssrf.allow-loopback-for-tests:false}")
    private boolean allowLoopbackForTests;

    private static final Set<String> CLOUD_METADATA_IPS = Set.of(
            "169.254.169.254",   // AWS, Azure, GCP, OpenStack
            "100.100.100.200",   // Alibaba Cloud
            "192.0.0.170",       // Common internal resolvers
            "192.0.0.171"
    );

    public void validateUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new SsrfSecurityException("Target URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            throw new SsrfSecurityException("Invalid URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SsrfSecurityException("Prohibited protocol scheme: '" + scheme + "'. Only HTTP and HTTPS are permitted");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new SsrfSecurityException("URL must contain a valid host");
        }

        host = host.toLowerCase();

        // Immediate check for obvious loopback and metadata hostnames
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
            if (!allowLoopbackForTests) {
                throw new SsrfSecurityException("Access to local or internal domain '" + host + "' is prohibited");
            }
        }

        // Resolve DNS and inspect every IP address
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new SsrfSecurityException("Failed to resolve host '" + host + "': Host not found");
        }

        if (addresses == null || addresses.length == 0) {
            throw new SsrfSecurityException("No IP address could be resolved for host: " + host);
        }

        for (InetAddress address : addresses) {
            validateIpAddress(address);
        }
    }

    public void validateIpAddress(InetAddress address) {
        String ipStr = address.getHostAddress();

        // Check well-known cloud metadata IP targets
        if (CLOUD_METADATA_IPS.contains(ipStr)) {
            throw new SsrfSecurityException("Access to cloud metadata IP (" + ipStr + ") is strictly prohibited");
        }

        // Check loopback
        if (address.isLoopbackAddress()) {
            if (!allowLoopbackForTests) {
                throw new SsrfSecurityException("Access to loopback IP (" + ipStr + ") is prohibited");
            }
            return;
        }

        // Check RFC 1918 private / site-local addresses
        if (address.isSiteLocalAddress()) {
            if (!allowLoopbackForTests) {
                throw new SsrfSecurityException("Access to private RFC 1918 IP address (" + ipStr + ") is prohibited");
            }
            return;
        }

        // Check link-local addresses (169.254.x.x / fe80::/10)
        if (address.isLinkLocalAddress()) {
            throw new SsrfSecurityException("Access to link-local IP address (" + ipStr + ") is prohibited");
        }

        // Check wildcard / any-local addresses (0.0.0.0, ::)
        if (address.isAnyLocalAddress()) {
            throw new SsrfSecurityException("Access to wildcard local IP address (" + ipStr + ") is prohibited");
        }

        // Check multicast
        if (address.isMulticastAddress()) {
            throw new SsrfSecurityException("Access to multicast IP address (" + ipStr + ") is prohibited");
        }

        // Check Carrier-Grade NAT (100.64.0.0/10) for IPv4
        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int firstOctet = bytes[0] & 0xFF;
            int secondOctet = bytes[1] & 0xFF;
            if (firstOctet == 100 && (secondOctet >= 64 && secondOctet <= 127)) {
                throw new SsrfSecurityException("Access to Carrier-Grade NAT IP address (" + ipStr + ") is prohibited");
            }
            // Check 0.0.0.0/8
            if (firstOctet == 0) {
                throw new SsrfSecurityException("Access to reserved IP address (" + ipStr + ") is prohibited");
            }
            // Check 127.0.0.0/8
            if (firstOctet == 127 && !allowLoopbackForTests) {
                throw new SsrfSecurityException("Access to loopback range (" + ipStr + ") is prohibited");
            }
        }
    }

    public void setAllowLoopbackForTests(boolean allow) {
        this.allowLoopbackForTests = allow;
    }
}
