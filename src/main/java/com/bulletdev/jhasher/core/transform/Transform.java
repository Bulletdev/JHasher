package com.bulletdev.jhasher.core.transform;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public interface Transform {
    String id();
    String name();
    String category();
    byte[] execute(byte[] input, Map<String, String> params);
    default String executeString(String input, Map<String, String> params) {
        byte[] result = execute(input.getBytes(StandardCharsets.UTF_8), params);
        return new String(result, StandardCharsets.UTF_8);
    }
}
