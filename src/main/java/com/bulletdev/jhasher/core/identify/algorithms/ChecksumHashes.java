package com.bulletdev.jhasher.core.identify.algorithms;

import com.bulletdev.jhasher.core.identify.HashAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public final class ChecksumHashes {

    private ChecksumHashes() {}

    public static final class Crc32 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{8}$");

        @Override public String id() { return "crc32"; }
        @Override public String name() { return "CRC-32"; }
        @Override public String category() { return "checksum"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 10; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            CRC32 crc = new CRC32();
            crc.update(input.getBytes(StandardCharsets.UTF_8));
            return Optional.of(String.format("%08x", crc.getValue()));
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Adler32Hash implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{8}$");

        @Override public String id() { return "adler32"; }
        @Override public String name() { return "Adler-32"; }
        @Override public String category() { return "checksum"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 5; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            Adler32 adler = new Adler32();
            adler.update(input.getBytes(StandardCharsets.UTF_8));
            return Optional.of(String.format("%08x", adler.getValue()));
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Crc16 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{4}$");

        @Override public String id() { return "crc16"; }
        @Override public String name() { return "CRC-16"; }
        @Override public String category() { return "checksum"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 10; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            // CRC-16/CCITT-FALSE
            byte[] data = input.getBytes(StandardCharsets.UTF_8);
            int crc = 0xFFFF;
            for (byte b : data) {
                crc ^= (b & 0xFF) << 8;
                for (int i = 0; i < 8; i++) {
                    if ((crc & 0x8000) != 0) {
                        crc = (crc << 1) ^ 0x1021;
                    } else {
                        crc <<= 1;
                    }
                }
                crc &= 0xFFFF;
            }
            return Optional.of(String.format("%04x", crc));
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }
}
