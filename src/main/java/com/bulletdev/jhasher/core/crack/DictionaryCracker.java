package com.bulletdev.jhasher.core.crack;

import com.bulletdev.jhasher.core.identify.HashAlgorithm;
import com.bulletdev.jhasher.core.identify.HashIdentifier;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;
import com.bulletdev.jhasher.core.identify.HashRegistry;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lightweight dictionary-only hash cracker.
 * <p>
 * This is deliberately limited in scope - it checks each word in a wordlist
 * against the target hash. For serious cracking, use hashcat or john.
 */
public final class DictionaryCracker {

    private static final int PROGRESS_INTERVAL = 1000;
    private static final String POISON_PILL = "\0__DONE__\0";

    private final HashIdentifier identifier = new HashIdentifier();

    /**
     * Crack a hash using a single thread.
     *
     * @param hash        the target hash to crack
     * @param algorithmId algorithm to use, or null for auto-detect
     * @param wordlist    the wordlist source
     * @param listener    progress listener (may be null)
     * @return the crack result
     */
    public CrackResult crack(String hash, String algorithmId, WordlistSource wordlist, CrackListener listener) {
        HashAlgorithm algorithm = resolveAlgorithm(hash, algorithmId);
        if (algorithm == null) {
            CrackResult result = new CrackResult(false, null, algorithmId, 0, 0, 0);
            if (listener != null) {
                listener.onComplete(result);
            }
            return result;
        }

        long totalEstimate = wordlist.estimateSize();
        long startTime = System.currentTimeMillis();
        long attempts = 0;

        try (wordlist) {
            for (String word : wordlist) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }

                attempts++;
                Optional<String> generated = algorithm.generate(word, Map.of());
                if (generated.isPresent() && hashEquals(hash, generated.get())) {
                    long duration = System.currentTimeMillis() - startTime;
                    double speed = duration > 0 ? (attempts * 1000.0) / duration : attempts;
                    CrackResult result = new CrackResult(true, word, algorithm.id(), attempts, duration, speed);
                    if (listener != null) {
                        listener.onFound(word);
                        listener.onComplete(result);
                    }
                    return result;
                }

                if (listener != null && attempts % PROGRESS_INTERVAL == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double speed = elapsed > 0 ? (attempts * 1000.0) / elapsed : attempts;
                    listener.onProgress(attempts, totalEstimate, speed);
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        double speed = duration > 0 ? (attempts * 1000.0) / duration : attempts;
        CrackResult result = new CrackResult(false, null, algorithm.id(), attempts, duration, speed);
        if (listener != null) {
            listener.onComplete(result);
        }
        return result;
    }

    /**
     * Crack a hash using multiple virtual threads.
     * <p>
     * Uses a producer-consumer pattern: one reader thread feeds words into a
     * BlockingQueue and N virtual thread consumers hash and compare.
     *
     * @param hash        the target hash to crack
     * @param algorithmId algorithm to use, or null for auto-detect
     * @param wordlist    the wordlist source
     * @param listener    progress listener (may be null)
     * @param threads     number of consumer threads
     * @return the crack result
     */
    public CrackResult crackParallel(String hash, String algorithmId, WordlistSource wordlist,
                                     CrackListener listener, int threads) {
        if (threads <= 1) {
            return crack(hash, algorithmId, wordlist, listener);
        }

        HashAlgorithm algorithm = resolveAlgorithm(hash, algorithmId);
        if (algorithm == null) {
            CrackResult result = new CrackResult(false, null, algorithmId, 0, 0, 0);
            if (listener != null) {
                listener.onComplete(result);
            }
            return result;
        }

        long totalEstimate = wordlist.estimateSize();
        long startTime = System.currentTimeMillis();
        AtomicLong attempts = new AtomicLong(0);
        AtomicBoolean found = new AtomicBoolean(false);
        AtomicReference<String> foundPlaintext = new AtomicReference<>(null);

        BlockingQueue<String> queue = new LinkedBlockingQueue<>(threads * 1024);
        ThreadFactory vtFactory = Thread.ofVirtual().name("cracker-", 0).factory();

        // Producer: reads words and feeds them into the queue
        Thread producer = vtFactory.newThread(() -> {
            try (wordlist) {
                for (String word : wordlist) {
                    if (found.get() || Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    queue.put(word);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                // Send poison pills to signal consumers to stop
                for (int i = 0; i < threads; i++) {
                    try {
                        queue.put(POISON_PILL);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });

        // Consumers: take words, hash, compare
        Thread[] consumers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            consumers[i] = vtFactory.newThread(() -> {
                try {
                    while (!found.get() && !Thread.currentThread().isInterrupted()) {
                        String word = queue.take();
                        if (POISON_PILL.equals(word)) {
                            break;
                        }

                        long currentAttempts = attempts.incrementAndGet();
                        Optional<String> generated = algorithm.generate(word, Map.of());

                        if (generated.isPresent() && hashEquals(hash, generated.get())) {
                            if (found.compareAndSet(false, true)) {
                                foundPlaintext.set(word);
                                if (listener != null) {
                                    listener.onFound(word);
                                }
                                // Interrupt producer to stop feeding
                                producer.interrupt();
                            }
                            break;
                        }

                        if (listener != null && currentAttempts % PROGRESS_INTERVAL == 0) {
                            long elapsed = System.currentTimeMillis() - startTime;
                            double speed = elapsed > 0 ? (currentAttempts * 1000.0) / elapsed : currentAttempts;
                            listener.onProgress(currentAttempts, totalEstimate, speed);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Start all threads
        producer.start();
        for (Thread consumer : consumers) {
            consumer.start();
        }

        // Wait for completion
        try {
            producer.join();
            for (Thread consumer : consumers) {
                consumer.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Interrupt everything
            producer.interrupt();
            for (Thread consumer : consumers) {
                consumer.interrupt();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        long totalAttempts = attempts.get();
        double speed = duration > 0 ? (totalAttempts * 1000.0) / duration : totalAttempts;

        CrackResult result;
        if (found.get()) {
            result = new CrackResult(true, foundPlaintext.get(), algorithm.id(), totalAttempts, duration, speed);
        } else {
            result = new CrackResult(false, null, algorithm.id(), totalAttempts, duration, speed);
        }

        if (listener != null) {
            listener.onComplete(result);
        }
        return result;
    }

    /**
     * Resolve the algorithm - either by explicit id or auto-detect from hash.
     */
    private HashAlgorithm resolveAlgorithm(String hash, String algorithmId) {
        if (algorithmId != null && !algorithmId.isBlank()) {
            return HashRegistry.getInstance().getById(algorithmId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + algorithmId));
        }

        // Auto-detect
        List<HashMatch> matches = identifier.identify(hash);
        if (matches.isEmpty()) {
            return null;
        }
        return matches.getFirst().algorithm();
    }

    /**
     * Compare hashes case-insensitively for hex hashes, exact for others.
     */
    private static boolean hashEquals(String target, String generated) {
        if (target.equalsIgnoreCase(generated)) {
            return true;
        }
        // Some algorithms produce prefix-based hashes (bcrypt, etc.)
        return target.equals(generated);
    }
}
