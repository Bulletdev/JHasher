package com.bulletdev.jhasher.core.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

/**
 * Key derivation functions: PBKDF2, scrypt, HKDF, and a simple SHA-256 derivation.
 */
public final class KeyDerivation {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public record DerivedKey(byte[] key, byte[] salt, String algorithm, int iterations, int keyLengthBits) {}

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Default PBKDF2 iteration count (OWASP 2024 recommendation). */
    public static final int DEFAULT_PBKDF2_ITERATIONS = 600_000;

    /** Default scrypt parameters. */
    public static final int DEFAULT_SCRYPT_N = 131072; // 2^17
    public static final int DEFAULT_SCRYPT_R = 8;
    public static final int DEFAULT_SCRYPT_P = 1;

    private KeyDerivation() {}

    // ---- PBKDF2 ----

    /**
     * Derive a key using PBKDF2-HMAC-SHA256.
     *
     * @param password      the password
     * @param salt          salt bytes, or null to auto-generate 16 bytes
     * @param iterations    iteration count (use {@link #DEFAULT_PBKDF2_ITERATIONS} if unsure)
     * @param keyLengthBits desired key length in bits (e.g. 256)
     * @return DerivedKey with the key, salt, and parameters
     */
    public static DerivedKey pbkdf2(String password, byte[] salt, int iterations, int keyLengthBits) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be positive, got: " + iterations);
        }
        if (keyLengthBits <= 0 || keyLengthBits % 8 != 0) {
            throw new IllegalArgumentException("Key length must be a positive multiple of 8 bits, got: " + keyLengthBits);
        }

        if (salt == null) {
            salt = generateSalt(16);
        }

        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] key = factory.generateSecret(spec).getEncoded();
            spec.clearPassword();
            return new DerivedKey(key, salt, "PBKDF2WithHmacSHA256", iterations, keyLengthBits);
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 key derivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * PBKDF2 with default iterations ({@value #DEFAULT_PBKDF2_ITERATIONS}).
     */
    public static DerivedKey pbkdf2(String password, byte[] salt, int keyLengthBits) {
        return pbkdf2(password, salt, DEFAULT_PBKDF2_ITERATIONS, keyLengthBits);
    }

    // ---- scrypt ----

    /**
     * Derive a key using scrypt.
     *
     * @param password      the password
     * @param salt          salt bytes, or null to auto-generate 16 bytes
     * @param n             CPU/memory cost parameter (must be power of 2)
     * @param r             block size
     * @param p             parallelisation parameter
     * @param keyLengthBits desired key length in bits
     * @return DerivedKey with the key, salt, and parameters
     */
    public static DerivedKey scrypt(String password, byte[] salt, int n, int r, int p, int keyLengthBits) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        if (keyLengthBits <= 0 || keyLengthBits % 8 != 0) {
            throw new IllegalArgumentException("Key length must be a positive multiple of 8 bits, got: " + keyLengthBits);
        }
        if (n <= 1 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("scrypt N must be a power of 2 greater than 1, got: " + n);
        }

        if (salt == null) {
            salt = generateSalt(16);
        }

        try {
            byte[] key = SCrypt.generate(
                    password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    salt,
                    n, r, p,
                    keyLengthBits / 8
            );
            return new DerivedKey(key, salt, "scrypt", n, keyLengthBits);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("scrypt key derivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * scrypt with default parameters (N={@value #DEFAULT_SCRYPT_N}, r={@value #DEFAULT_SCRYPT_R}, p={@value #DEFAULT_SCRYPT_P}).
     */
    public static DerivedKey scrypt(String password, byte[] salt, int keyLengthBits) {
        return scrypt(password, salt, DEFAULT_SCRYPT_N, DEFAULT_SCRYPT_R, DEFAULT_SCRYPT_P, keyLengthBits);
    }

    // ---- HKDF ----

    /**
     * Derive a key using HKDF-SHA256 (extract-then-expand).
     *
     * @param ikm           input keying material
     * @param salt          optional salt (null uses a zero-filled hash-length array)
     * @param info          optional context/application-specific info (null for empty)
     * @param keyLengthBits desired key length in bits
     * @return DerivedKey with the key and parameters
     */
    public static DerivedKey hkdf(byte[] ikm, byte[] salt, byte[] info, int keyLengthBits) {
        if (ikm == null || ikm.length == 0) {
            throw new IllegalArgumentException("Input keying material (IKM) must not be null or empty");
        }
        if (keyLengthBits <= 0 || keyLengthBits % 8 != 0) {
            throw new IllegalArgumentException("Key length must be a positive multiple of 8 bits, got: " + keyLengthBits);
        }

        try {
            HKDFBytesGenerator hkdfGenerator = new HKDFBytesGenerator(new SHA256Digest());
            HKDFParameters params;

            if (salt != null) {
                params = new HKDFParameters(ikm, salt, info != null ? info : new byte[0]);
            } else {
                params = new HKDFParameters(ikm, new byte[32], info != null ? info : new byte[0]);
            }

            hkdfGenerator.init(params);
            byte[] key = new byte[keyLengthBits / 8];
            hkdfGenerator.generateBytes(key, 0, key.length);

            return new DerivedKey(key, salt, "HKDF-SHA256", 1, keyLengthBits);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("HKDF key derivation failed: " + e.getMessage(), e);
        }
    }

    // ---- simple derivation ----

    /**
     * Derive a key by hashing the password with SHA-256 and truncating to the requested length.
     * This is NOT a secure KDF - use PBKDF2 or scrypt for real password-based key derivation.
     *
     * @param password       the password string
     * @param keyLengthBytes desired key length in bytes (max 32)
     * @return raw key bytes
     */
    public static byte[] deriveKeySimple(String password, int keyLengthBytes) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        if (keyLengthBytes <= 0 || keyLengthBytes > 32) {
            throw new IllegalArgumentException("Key length must be between 1 and 32 bytes, got: " + keyLengthBytes);
        }

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha256.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Arrays.copyOf(hash, keyLengthBytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available: " + e.getMessage(), e);
        }
    }

    // ---- internal ----

    private static byte[] generateSalt(int length) {
        byte[] salt = new byte[length];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }
}
