package com.bulletdev.jhasher.core.crack;

/**
 * Result of a dictionary crack attempt.
 *
 * @param found      whether a matching plaintext was found
 * @param plaintext  the cracked plaintext, or null if not found
 * @param algorithm  the algorithm id used for cracking
 * @param attempts   total number of words tried
 * @param durationMs wall-clock time in milliseconds
 * @param speed      average hashes per second
 */
public record CrackResult(
        boolean found,
        String plaintext,
        String algorithm,
        long attempts,
        long durationMs,
        double speed
) {}
