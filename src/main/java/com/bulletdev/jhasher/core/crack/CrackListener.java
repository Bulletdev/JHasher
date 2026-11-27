package com.bulletdev.jhasher.core.crack;

/**
 * Callback interface for monitoring dictionary crack progress.
 */
public interface CrackListener {

    /**
     * Called periodically to report progress.
     *
     * @param attempts      number of words tried so far
     * @param totalEstimate estimated total words (-1 if unknown)
     * @param speed         hashes per second
     */
    void onProgress(long attempts, long totalEstimate, double speed);

    /**
     * Called when a matching plaintext is found.
     *
     * @param plaintext the plaintext that produced the target hash
     */
    void onFound(String plaintext);

    /**
     * Called when the crack operation completes (found or exhausted).
     *
     * @param result the final result
     */
    void onComplete(CrackResult result);
}
