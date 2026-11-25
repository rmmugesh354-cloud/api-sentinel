package com.sentinel.apisentinel.engine;

import com.sentinel.apisentinel.common.exception.SsrfSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfValidator();
        validator.setAllowLoopbackForTests(false);
    }

    @Test
    @DisplayName("Should reject null or blank URLs")
    void testBlankUrl() {
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl(null));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "file:///etc/passwd",
            "gopher://example.com",
            "ldap://example.com"
    })
    @DisplayName("Should reject non-HTTP(S) protocol schemes")
    void testProhibitedSchemes(String url) {
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8080/api",
            "http://127.0.0.1:8080/metrics",
            "http://127.0.0.2/admin",
            "http://sub.localhost/service"
    })
    @DisplayName("Should reject loopback addresses")
    void testLoopbackAddresses(String url) {
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl(url));
    }

    @Test
    @DisplayName("Should reject well-known cloud metadata IP")
    void testCloudMetadataIp() throws UnknownHostException {
        InetAddress metadataIp = InetAddress.getByName("169.254.169.254");
        assertThrows(SsrfSecurityException.class, () -> validator.validateIpAddress(metadataIp));
    }

    @Test
    @DisplayName("Should reject RFC 1918 private IPv4 addresses")
    void testPrivateIpv4() throws UnknownHostException {
        InetAddress classA = InetAddress.getByName("10.0.0.1");
        InetAddress classB = InetAddress.getByName("172.16.0.1");
        InetAddress classC = InetAddress.getByName("192.168.1.1");

        assertThrows(SsrfSecurityException.class, () -> validator.validateIpAddress(classA));
        assertThrows(SsrfSecurityException.class, () -> validator.validateIpAddress(classB));
        assertThrows(SsrfSecurityException.class, () -> validator.validateIpAddress(classC));
    }

    @Test
    @DisplayName("Should reject Carrier-Grade NAT (100.64.0.0/10)")
    void testCarrierGradeNat() throws UnknownHostException {
        InetAddress cgnatIp = InetAddress.getByName("100.64.1.1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateIpAddress(cgnatIp));
    }

    @Test
    @DisplayName("Should accept valid public IP addresses")
    void testValidPublicIp() throws UnknownHostException {
        InetAddress publicIp = InetAddress.getByName("8.8.8.8");
        assertDoesNotThrow(() -> validator.validateIpAddress(publicIp));
    }
}
