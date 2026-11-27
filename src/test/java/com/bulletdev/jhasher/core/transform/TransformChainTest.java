package com.bulletdev.jhasher.core.transform;

import com.bulletdev.jhasher.core.transform.TransformChain.StepResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransformChainTest {

    private TransformRegistry registry;

    @BeforeEach
    void setUp() {
        registry = TransformRegistry.getInstance();
    }

    @Test
    void testBase64Encode() {
        TransformChain chain = TransformChain.parse("base64encode", registry);
        String result = chain.executeString("hello");
        assertEquals("aGVsbG8=", result);
    }

    @Test
    void testBase64Roundtrip() {
        TransformChain chain = TransformChain.parse("base64encode | base64decode", registry);
        String result = chain.executeString("hello");
        assertEquals("hello", result);
    }

    @Test
    void testUrlEncodeRoundtrip() {
        TransformChain chain = TransformChain.parse("urlencode | urldecode", registry);
        String result = chain.executeString("hello world&foo=bar");
        assertEquals("hello world&foo=bar", result);
    }

    @Test
    void testSha256KnownValue() {
        TransformChain chain = TransformChain.parse("sha256", registry);
        String result = chain.executeString("abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result);
    }

    @Test
    void testUpper() {
        TransformChain chain = TransformChain.parse("upper", registry);
        String result = chain.executeString("hello");
        assertEquals("HELLO", result);
    }

    @Test
    void testHexRoundtrip() {
        TransformChain chain = TransformChain.parse("hex-encode | hex-decode", registry);
        String result = chain.executeString("hello");
        assertEquals("hello", result);
    }

    @Test
    void testChainMultipleSteps() {
        TransformChain chain = TransformChain.parse("base64encode | upper", registry);
        String result = chain.executeString("hello");
        assertEquals("AGVSBG8=", result);
    }

    @Test
    void testEmptyRecipe() {
        assertThrows(IllegalArgumentException.class, () -> {
            TransformChain.parse("", registry);
        });
    }
}
