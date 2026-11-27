package com.bulletdev.jhasher.core.jwt;

import com.bulletdev.jhasher.core.jwt.JwtDecoder.JwtAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtDecoderTest {

    private JwtDecoder decoder;

    // Standard test JWT: {"alg":"HS256","typ":"JWT"}.{"sub":"1234567890","name":"John Doe","iat":1516239022}
    private static final String TEST_JWT =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setUp() {
        decoder = new JwtDecoder();
    }

    @Test
    void testDecodeHeader() {
        JwtAnalysis analysis = decoder.decode(TEST_JWT);
        assertEquals("HS256", analysis.algorithm());
        assertEquals("HS256", analysis.header().get("alg"));
        assertEquals("JWT", analysis.header().get("typ"));
    }

    @Test
    void testDecodePayload() {
        JwtAnalysis analysis = decoder.decode(TEST_JWT);
        assertEquals("1234567890", analysis.payload().get("sub"));
        assertEquals("John Doe", analysis.payload().get("name"));
    }

    @Test
    void testDecodeExpired() {
        // Craft a token with exp in the past (epoch 1000000)
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"test\",\"exp\":1000000}";

        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String expiredToken = header + "." + payload + ".fakesig";

        JwtAnalysis analysis = decoder.decode(expiredToken);
        assertTrue(analysis.timestamps().isExpired(), "Token with past exp should be expired");
    }
}
