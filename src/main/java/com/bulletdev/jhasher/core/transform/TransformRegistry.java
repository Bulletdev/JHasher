package com.bulletdev.jhasher.core.transform;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Singleton registry for all available transforms.
 * Built-in transforms from {@link Transforms} are registered automatically.
 */
public final class TransformRegistry {

    private static final TransformRegistry INSTANCE = new TransformRegistry();

    private final Map<String, Transform> transforms = new LinkedHashMap<>();

    private TransformRegistry() {
        for (Transform t : Transforms.ALL) {
            register(t);
        }
    }

    public static TransformRegistry getInstance() {
        return INSTANCE;
    }

    public void register(Transform transform) {
        transforms.put(transform.id(), transform);
    }

    public Transform getById(String id) {
        return transforms.get(id);
    }

    public List<Transform> getAll() {
        return List.copyOf(transforms.values());
    }

    public List<Transform> getByCategory(String category) {
        return transforms.values().stream()
                .filter(t -> t.category().equalsIgnoreCase(category))
                .collect(Collectors.toUnmodifiableList());
    }

    public List<String> listIds() {
        return List.copyOf(transforms.keySet());
    }
}
