package com.bulletdev.jhasher.core.identify;

import com.bulletdev.jhasher.core.generate.HashGenerator;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HashIdentifierTest {

    private HashIdentifier identifier;

    @BeforeEach
    void setUp() {
        identifier = new HashIdentifier();
    }

    @Test
    void testIdentifyMd5() {
        List<HashMatch> matches = identifier.identify("5f4dcc3b5aa765d61d8327deb882cf99");
        assertFalse(matches.isEmpty(), "Should identify MD5 hash");
        assertEquals("MD5", matches.getFirst().algorithm().name());
    }

    @Test
    void testIdentifyBcrypt() {
        // Valid bcrypt hash (60 characters total)
        String bcrypt = "$2b$12$WApznUPhDubN0oeveSXHp.Gh7FjzMhMg8eROGXIPHE8bHRDKQs5Lm";
        List<HashMatch> matches = identifier.identify(bcrypt);
        assertFalse(matches.isEmpty(), "Should identify bcrypt hash");
        assertEquals("bcrypt", matches.getFirst().algorithm().name());
    }

    @Test
    void testIdentifyMySql5() {
        String mysql5 = "*6BB4837EB74329105EE4568DDA7DC67ED2CA2AD9";
        List<HashMatch> matches = identifier.identify(mysql5);
        assertFalse(matches.isEmpty(), "Should identify MySQL5 hash");
        assertEquals("MySQL 5.x", matches.getFirst().algorithm().name());
    }

    @Test
    void testIdentifySha256ReturnsMultiple() {
        // 64-character hex string is ambiguous between SHA-256, SHA3-256, BLAKE2b-256
        String hex64 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        List<HashMatch> matches = identifier.identify(hex64);
        assertTrue(matches.size() >= 2, "64-char hex should match multiple algorithms");

        List<String> names = matches.stream().map(m -> m.algorithm().name()).toList();
        assertTrue(names.contains("SHA-256"), "Should include SHA-256");
        assertTrue(names.contains("SHA3-256"), "Should include SHA3-256");
    }

    @Test
    void testIdentifyEmpty() {
        List<HashMatch> matches = identifier.identify("");
        assertTrue(matches.isEmpty(), "Empty string should return no matches");
    }

    @Test
    void testGenerateThenIdentifyMd5() {
        HashGenerator generator = new HashGenerator();
        String hash = generator.generate("md5", "test", Map.of());

        List<HashMatch> matches = identifier.identify(hash);
        assertFalse(matches.isEmpty(), "Should identify generated MD5");
        assertEquals("MD5", matches.getFirst().algorithm().name());
    }

    @Test
    void testGenerateThenIdentifySha256() {
        HashGenerator generator = new HashGenerator();
        String hash = generator.generate("sha256", "test", Map.of());

        List<HashMatch> matches = identifier.identify(hash);
        assertFalse(matches.isEmpty(), "Should identify generated SHA-256");

        List<String> names = matches.stream().map(m -> m.algorithm().name()).toList();
        assertTrue(names.contains("SHA-256"), "Top matches should include SHA-256");
    }
}
