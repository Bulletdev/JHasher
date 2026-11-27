package com.bulletdev.jhasher.core.identify;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public interface HashAlgorithm {
    String id();
    String name();
    String category();
    Pattern pattern();
    default int priority() { return 50; }
    default boolean matches(String hash) {
        return hash != null && pattern().matcher(hash).matches();
    }
    Optional<String> generate(String input, Map<String, String> options);
    Optional<Integer> hashcatMode();
    Optional<String> johnFormat();
}
