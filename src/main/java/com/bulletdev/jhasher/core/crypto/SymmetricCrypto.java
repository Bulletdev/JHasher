package com.bulletdev.jhasher.core.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

/**
 * Symmetric encryption and decryption supporting AES (GCM, CBC, CTR) and ChaCha20-Poly1305.
 */
public final class SymmetricCrypto {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public record CryptoResult(byte[] data, String algorithm, String mode, byte[] iv) {}

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private SymmetricCrypto() {}

    // ---- public API ----

    /**
     * Encrypt plaintext with the given algorithm, key, and optional IV.
     *
     * @param algorithm e.g. "aes-256-gcm", "aes-128-cbc", "chacha20-poly1305"
     * @param plaintext data to encrypt
     * @param key       raw key bytes - length must match algorithm
     * @param iv        initialisation vector, or null to auto-generate
     * @return CryptoResult containing ciphertext (IV is NOT prepended)
     */
    public static CryptoResult encrypt(String algorithm, byte[] plaintext, byte[] key, byte[] iv) {
        ParsedAlgorithm parsed = parseAlgorithm(algorithm);
        validateKeyLength(parsed, key);

        if (iv == null) {
            iv = generateDefaultIV(parsed);
        }

        try {
            Cipher cipher = createCipher(parsed, Cipher.ENCRYPT_MODE, key, iv);
            byte[] ciphertext = cipher.doFinal(plaintext);
            return new CryptoResult(ciphertext, parsed.cipherFamily(), parsed.mode(), iv);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt ciphertext with the given algorithm, key, and IV.
     */
    public static CryptoResult decrypt(String algorithm, byte[] ciphertext, byte[] key, byte[] iv) {
        ParsedAlgorithm parsed = parseAlgorithm(algorithm);
        validateKeyLength(parsed, key);

        if (iv == null) {
            throw new IllegalArgumentException("IV is required for decryption");
        }

        try {
            Cipher cipher = createCipher(parsed, Cipher.DECRYPT_MODE, key, iv);
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new CryptoResult(plaintext, parsed.cipherFamily(), parsed.mode(), iv);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience: encrypt a UTF-8 string, derive the key from a password via SHA-256,
     * prepend the IV to the ciphertext, and return Base64.
     */
    public static String encryptToBase64(String algorithm, String plaintext, String keyString) {
        ParsedAlgorithm parsed = parseAlgorithm(algorithm);
        byte[] key = KeyDerivation.deriveKeySimple(keyString, parsed.keyLengthBytes());
        byte[] iv = generateDefaultIV(parsed);

        CryptoResult result = encrypt(algorithm, plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8), key, iv);

        // Prepend IV to ciphertext
        byte[] combined = new byte[iv.length + result.data().length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(result.data(), 0, combined, iv.length, result.data().length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Convenience: Base64-decode, extract the IV from the leading bytes, decrypt, return UTF-8 string.
     */
    public static String decryptFromBase64(String algorithm, String base64Data, String keyString) {
        ParsedAlgorithm parsed = parseAlgorithm(algorithm);
        byte[] key = KeyDerivation.deriveKeySimple(keyString, parsed.keyLengthBytes());

        byte[] combined = Base64.getDecoder().decode(base64Data);
        int ivLen = defaultIVLength(parsed);

        if (combined.length < ivLen) {
            throw new IllegalArgumentException(
                    "Base64 data too short: expected at least " + ivLen + " bytes for IV, got " + combined.length);
        }

        byte[] iv = Arrays.copyOfRange(combined, 0, ivLen);
        byte[] ciphertext = Arrays.copyOfRange(combined, ivLen, combined.length);

        CryptoResult result = decrypt(algorithm, ciphertext, key, iv);
        return new String(result.data(), java.nio.charset.StandardCharsets.UTF_8);
    }

    // ---- internal helpers ----

    private record ParsedAlgorithm(String cipherFamily, int keyLengthBits, String mode) {
        int keyLengthBytes() {
            return keyLengthBits / 8;
        }
    }

    private static ParsedAlgorithm parseAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Algorithm must not be null or blank");
        }

        String lower = algorithm.toLowerCase(Locale.ROOT).trim();

        // ChaCha20-Poly1305
        if (lower.equals("chacha20-poly1305")) {
            return new ParsedAlgorithm("ChaCha20", 256, "Poly1305");
        }

        // AES variants: aes-<bits>-<mode>
        if (lower.startsWith("aes-")) {
            String[] parts = lower.split("-");
            if (parts.length != 3) {
                throw new IllegalArgumentException(
                        "Invalid AES algorithm format. Expected 'aes-<bits>-<mode>', got: " + algorithm);
            }

            int bits;
            try {
                bits = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid key size in algorithm: " + algorithm);
            }

            if (bits != 128 && bits != 192 && bits != 256) {
                throw new IllegalArgumentException("Unsupported AES key size: " + bits + ". Use 128, 192, or 256.");
            }

            String mode = parts[2].toUpperCase(Locale.ROOT);
            return switch (mode) {
                case "GCM" -> new ParsedAlgorithm("AES", bits, "GCM");
                case "CBC" -> new ParsedAlgorithm("AES", bits, "CBC");
                case "CTR" -> new ParsedAlgorithm("AES", bits, "CTR");
                default -> throw new IllegalArgumentException(
                        "Unsupported AES mode: " + mode + ". Use GCM, CBC, or CTR.");
            };
        }

        throw new IllegalArgumentException(
                "Unknown algorithm: " + algorithm
                        + ". Supported: aes-128-gcm, aes-192-gcm, aes-256-gcm, aes-128-cbc, aes-256-cbc, "
                        + "aes-128-ctr, aes-256-ctr, chacha20-poly1305");
    }

    private static void validateKeyLength(ParsedAlgorithm parsed, byte[] key) {
        int expected = parsed.keyLengthBytes();
        if (key.length != expected) {
            throw new IllegalArgumentException(
                    "Invalid key length for " + parsed.cipherFamily() + "-" + parsed.keyLengthBits() + "-" + parsed.mode()
                            + ": expected " + expected + " bytes, got " + key.length);
        }
    }

    private static int defaultIVLength(ParsedAlgorithm parsed) {
        return switch (parsed.mode()) {
            case "GCM", "Poly1305" -> 12;
            case "CBC", "CTR" -> 16;
            default -> throw new IllegalArgumentException("No default IV length for mode: " + parsed.mode());
        };
    }

    private static byte[] generateDefaultIV(ParsedAlgorithm parsed) {
        byte[] iv = new byte[defaultIVLength(parsed)];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    private static Cipher createCipher(ParsedAlgorithm parsed, int mode, byte[] key, byte[] iv) throws Exception {
        return switch (parsed.mode()) {
            case "GCM" -> {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
                cipher.init(mode, new SecretKeySpec(key, "AES"), gcmSpec);
                yield cipher;
            }
            case "CBC" -> {
                // nosemgrep: java.lang.security.audit.cbc-padding-oracle.cbc-padding-oracle
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
                yield cipher;
            }
            case "CTR" -> {
                Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
                cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
                yield cipher;
            }
            case "Poly1305" -> {
                Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
                cipher.init(mode, new SecretKeySpec(key, "ChaCha20"), new IvParameterSpec(iv));
                yield cipher;
            }
            default -> throw new IllegalArgumentException("Unsupported cipher mode: " + parsed.mode());
        };
    }
}
