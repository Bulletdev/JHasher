package com.bulletdev.jhasher.core.jwt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JwtDecoder {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    public record JwtTimestamps(
            Instant issuedAt,
            Instant expiresAt,
            Instant notBefore,
            boolean isExpired
    ) {}

    public record JwtAnalysis(
            Map<String, Object> header,
            Map<String, Object> payload,
            String signatureBase64,
            String algorithm,
            JwtTimestamps timestamps,
            List<String> warnings
    ) {}

    public JwtAnalysis decode(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }

        String[] parts = token.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("Invalid JWT format: expected 2 or 3 parts, got " + parts.length);
        }

        Map<String, Object> header = decodeJsonPart(parts[0]);
        Map<String, Object> payload = decodeJsonPart(parts[1]);
        String signatureBase64 = parts.length == 3 ? parts[2] : "";

        String algorithm = header.containsKey("alg") ? String.valueOf(header.get("alg")) : "unknown";

        Instant issuedAt = extractTimestamp(payload, "iat");
        Instant expiresAt = extractTimestamp(payload, "exp");
        Instant notBefore = extractTimestamp(payload, "nbf");
        boolean isExpired = expiresAt != null && expiresAt.isBefore(Instant.now());

        JwtTimestamps timestamps = new JwtTimestamps(issuedAt, expiresAt, notBefore, isExpired);

        List<String> warnings = buildWarnings(algorithm, expiresAt, isExpired, signatureBase64);

        return new JwtAnalysis(header, payload, signatureBase64, algorithm, timestamps, warnings);
    }

    private Map<String, Object> decodeJsonPart(String base64Part) {
        // Handle base64url with or without padding
        String padded = padBase64(base64Part);
        byte[] decoded = URL_DECODER.decode(padded);
        String json = new String(decoded, StandardCharsets.UTF_8);
        return GSON.fromJson(json, MAP_TYPE);
    }

    private String padBase64(String base64) {
        int remainder = base64.length() % 4;
        if (remainder == 2) {
            return base64 + "==";
        } else if (remainder == 3) {
            return base64 + "=";
        }
        return base64;
    }

    private Instant extractTimestamp(Map<String, Object> payload, String claim) {
        Object value = payload.get(claim);
        if (value == null) {
            return null;
        }
        try {
            long epoch = ((Number) value).longValue();
            return Instant.ofEpochSecond(epoch);
        } catch (ClassCastException | NumberFormatException e) {
            return null;
        }
    }

    private List<String> buildWarnings(String algorithm, Instant expiresAt, boolean isExpired, String signature) {
        List<String> warnings = new ArrayList<>();

        if (isExpired) {
            warnings.add("Token is expired");
        }

        if ("none".equalsIgnoreCase(algorithm)) {
            warnings.add("Algorithm is 'none' - unsigned token");
        }

        if (expiresAt == null) {
            warnings.add("Missing expiration claim");
        }

        if ("HS256".equals(algorithm)) {
            warnings.add("Weak algorithm: HS256 may be vulnerable to brute-force if secret is weak");
        }

        if (signature == null || signature.isEmpty()) {
            warnings.add("Empty signature - token integrity cannot be verified");
        }

        return warnings;
    }
}
