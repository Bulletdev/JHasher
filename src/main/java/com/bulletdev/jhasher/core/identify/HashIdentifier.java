package com.bulletdev.jhasher.core.identify;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class HashIdentifier {

    public record HashMatch(HashAlgorithm algorithm, double confidence) {}

    private static final Set<Integer> UNIQUE_HEX_LENGTHS = Set.of(
            56,  // SHA-224
            96   // SHA-384
    );

    private static final Set<Integer> AMBIGUOUS_HEX_LENGTHS = Set.of(
            4,   // CRC-16
            8,   // CRC-32, Adler-32
            16,  // MySQL OLD, Oracle H
            32,  // MD5, MD4, NTLM
            40,  // SHA-1, RIPEMD-160
            64,  // SHA-256, SHA3-256, BLAKE2b-256
            128  // SHA-512, SHA3-512, Whirlpool, BLAKE2b-512
    );

    private final HashRegistry registry;

    public HashIdentifier() {
        this.registry = HashRegistry.getInstance();
    }

    public List<HashMatch> identify(String hash) {
        if (hash == null || hash.isBlank()) {
            return List.of();
        }

        String trimmed = hash.trim();
        List<HashMatch> matches = new ArrayList<>();

        for (HashAlgorithm algorithm : registry.getAll()) {
            if (algorithm.matches(trimmed)) {
                double confidence = computeConfidence(algorithm, trimmed);
                matches.add(new HashMatch(algorithm, confidence));
            }
        }

        matches.sort(
                Comparator.comparingDouble(HashMatch::confidence).reversed()
                        .thenComparingInt(m -> -m.algorithm().priority())
        );

        return List.copyOf(matches);
    }

    private double computeConfidence(HashAlgorithm algorithm, String hash) {
        // Prefix-based patterns have near-certain identification
        if (hasPrefixIndicator(hash)) {
            return 0.99;
        }

        // Check if this is a pure hex string
        if (isPureHex(hash)) {
            int len = hash.length();

            if (UNIQUE_HEX_LENGTHS.contains(len)) {
                return 0.85;
            }

            if (AMBIGUOUS_HEX_LENGTHS.contains(len)) {
                // Base confidence adjusted by priority
                double base = 0.70;
                double priorityBonus = (algorithm.priority() - 25) / 100.0 * 0.15;
                return Math.min(0.85, base + priorityBonus);
            }
        }

        // Fallback for other matches
        return 0.60;
    }

    private boolean hasPrefixIndicator(String hash) {
        return hash.startsWith("$2b$") || hash.startsWith("$2a$") || hash.startsWith("$2y$")
                || hash.startsWith("$6$") || hash.startsWith("$5$") || hash.startsWith("$1$")
                || hash.startsWith("$argon2") || hash.startsWith("$scrypt") || hash.startsWith("$s0$")
                || hash.startsWith("$y$") || hash.startsWith("$pbkdf2")
                || hash.startsWith("$P$") || hash.startsWith("$S$")
                || hash.startsWith("0x0100") || hash.startsWith("0x0200")
                || hash.startsWith("*") || hash.startsWith("md5")
                || hash.startsWith("pbkdf2_sha256$");
    }

    private boolean isPureHex(String hash) {
        for (int i = 0; i < hash.length(); i++) {
            char c = hash.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return !hash.isEmpty();
    }
}
