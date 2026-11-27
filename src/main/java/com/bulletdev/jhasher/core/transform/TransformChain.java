package com.bulletdev.jhasher.core.transform;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A chain of transforms parsed from a pipe-delimited recipe string.
 * Each step is a transform id followed by optional --key value parameters.
 *
 * Example recipe: "base64encode | sha256 | hex-encode --upper true"
 */
public final class TransformChain {

    /**
     * Result of a single step in the chain, capturing timing and intermediate data.
     */
    public record StepResult(String transformId, byte[] input, byte[] output, long durationNanos) {}

    private final List<Step> steps;

    private record Step(Transform transform, Map<String, String> params) {}

    private TransformChain(List<Step> steps) {
        this.steps = steps;
    }

    /**
     * Parse a recipe string into a TransformChain.
     *
     * @param recipe  pipe-delimited recipe, e.g. "base64encode | sha256"
     * @param registry the registry to look up transform ids
     * @return a ready-to-execute chain
     * @throws IllegalArgumentException if a transform id is not found
     */
    public static TransformChain parse(String recipe, TransformRegistry registry) {
        String[] parts = recipe.split("\\|");
        List<Step> steps = new ArrayList<>();

        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] tokens = trimmed.split("\\s+");
            String id = tokens[0];

            Transform transform = registry.getById(id);
            if (transform == null) {
                throw new IllegalArgumentException("Unknown transform: " + id);
            }

            Map<String, String> params = new LinkedHashMap<>();
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].startsWith("--") && tokens[i].length() > 2) {
                    String key = tokens[i].substring(2);
                    String value = (i + 1 < tokens.length) ? tokens[++i] : "";
                    params.put(key, value);
                }
            }

            steps.add(new Step(transform, params));
        }

        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Empty recipe");
        }

        return new TransformChain(steps);
    }

    /**
     * Execute the chain on raw byte input, passing output of each step as input to the next.
     */
    public byte[] execute(byte[] input) {
        byte[] current = input;
        for (Step step : steps) {
            current = step.transform().execute(current, step.params());
        }
        return current;
    }

    /**
     * Convenience wrapper that operates on strings.
     */
    public String executeString(String input) {
        byte[] result = execute(input.getBytes(StandardCharsets.UTF_8));
        return new String(result, StandardCharsets.UTF_8);
    }

    /**
     * Execute the chain and return intermediate results for each step.
     */
    public List<StepResult> executeWithIntermediates(byte[] input) {
        List<StepResult> results = new ArrayList<>();
        byte[] current = input;

        for (Step step : steps) {
            byte[] stepInput = current;
            long start = System.nanoTime();
            current = step.transform().execute(current, step.params());
            long duration = System.nanoTime() - start;
            results.add(new StepResult(step.transform().id(), stepInput, current, duration));
        }

        return results;
    }
}
