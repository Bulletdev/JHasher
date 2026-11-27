package com.bulletdev.jhasher.core.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographic key and IV generator using SecureRandom.
 */
public final class CryptoKeyGenerator {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private CryptoKeyGenerator() {}

    /**
     * Generate a random key of the specified bit length.
     *
     * @param bits key length in bits (must be positive and a multiple of 8)
     * @return raw key bytes
     */
    public static byte[] generateKey(int bits) {
        if (bits <= 0 || bits % 8 != 0) {
            throw new IllegalArgumentException("Key length must be a positive multiple of 8 bits, got: " + bits);
        }
        byte[] key = new byte[bits / 8];
        SECURE_RANDOM.nextBytes(key);
        return key;
    }

    /**
     * Generate a random key and return it as a lowercase hex string.
     *
     * @param bits key length in bits
     * @return hex-encoded key
     */
    public static String generateKeyHex(int bits) {
        return HexFormat.of().formatHex(generateKey(bits));
    }

    /**
     * Generate a random key and return it as a Base64 string.
     *
     * @param bits key length in bits
     * @return Base64-encoded key
     */
    public static String generateKeyBase64(int bits) {
        return Base64.getEncoder().encodeToString(generateKey(bits));
    }

    /**
     * Generate a random initialisation vector.
     *
     * @param bytes IV length in bytes (e.g. 12 for GCM/ChaCha20, 16 for CBC/CTR)
     * @return raw IV bytes
     */
    public static byte[] generateIV(int bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("IV length must be positive, got: " + bytes);
        }
        byte[] iv = new byte[bytes];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Generate a random alphanumeric password of the specified length using SecureRandom.
     *
     * @param length password length in characters
     * @return random password string
     */
    public static String generatePassword(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Password length must be positive, got: " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
