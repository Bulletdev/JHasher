package com.bulletdev.jhasher.core.identify;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public final class ContextDatabase {

    private static final ContextDatabase INSTANCE = new ContextDatabase();

    private final Map<String, HashContext> contexts;

    private ContextDatabase() {
        this.contexts = loadContexts();
    }

    public static ContextDatabase getInstance() {
        return INSTANCE;
    }

    public Optional<HashContext> getContext(String algorithmId) {
        return Optional.ofNullable(contexts.get(algorithmId));
    }

    public Map<String, HashContext> getAll() {
        return Map.copyOf(contexts);
    }

    private Map<String, HashContext> loadContexts() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("data/hash-contexts.json")) {
            if (is == null) {
                return Map.of();
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, HashContext>>() {}.getType();
                Map<String, HashContext> loaded = gson.fromJson(reader, type);
                return loaded != null ? loaded : Map.of();
            }
        } catch (IOException e) {
            return Map.of();
        }
    }
}
