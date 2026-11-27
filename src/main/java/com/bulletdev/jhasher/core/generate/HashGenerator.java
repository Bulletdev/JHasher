package com.bulletdev.jhasher.core.generate;

import com.bulletdev.jhasher.core.identify.HashAlgorithm;
import com.bulletdev.jhasher.core.identify.HashRegistry;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HashGenerator {

    private final HashRegistry registry;

    public HashGenerator() {
        this.registry = HashRegistry.getInstance();
    }

    public String generate(String algorithmId, String input, Map<String, String> options) {
        HashAlgorithm algorithm = registry.getById(algorithmId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + algorithmId));

        return algorithm.generate(input, options)
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Algorithm '%s' does not support generation".formatted(algorithmId)));
    }

    public List<Map.Entry<String, String>> generateAll(String input) {
        List<Map.Entry<String, String>> results = new ArrayList<>();

        for (HashAlgorithm algorithm : registry.getAll()) {
            Optional<String> hash = algorithm.generate(input, Map.of());
            hash.ifPresent(h -> results.add(new AbstractMap.SimpleImmutableEntry<>(algorithm.id(), h)));
        }

        return List.copyOf(results);
    }

    public boolean verify(String algorithmId, String input, String expectedHash) {
        HashAlgorithm algorithm = registry.getById(algorithmId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown algorithm: " + algorithmId));

        Optional<String> generated = algorithm.generate(input, Map.of());
        return generated.map(g -> g.equalsIgnoreCase(expectedHash)).orElse(false);
    }
}
