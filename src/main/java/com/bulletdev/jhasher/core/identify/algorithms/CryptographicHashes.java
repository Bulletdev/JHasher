package com.bulletdev.jhasher.core.identify.algorithms;

import com.bulletdev.jhasher.core.identify.HashAlgorithm;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class CryptographicHashes {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private CryptographicHashes() {}

    private static Optional<String> digestWith(String algorithm, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Optional.of(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> digestWithBC(String algorithm, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME);
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Optional.of(HexFormat.of().formatHex(digest));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static final class Md5 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

        @Override public String id() { return "md5"; }
        @Override public String name() { return "MD5"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 50; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWith("MD5", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(0); }
        @Override public Optional<String> johnFormat() { return Optional.of("raw-md5"); }
    }

    public static final class Md4 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

        @Override public String id() { return "md4"; }
        @Override public String name() { return "MD4"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 30; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("MD4", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(900); }
        @Override public Optional<String> johnFormat() { return Optional.of("raw-md4"); }
    }

    public static final class Sha1 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{40}$");

        @Override public String id() { return "sha1"; }
        @Override public String name() { return "SHA-1"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWith("SHA-1", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(100); }
        @Override public Optional<String> johnFormat() { return Optional.of("raw-sha1"); }
    }

    public static final class Sha224 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{56}$");

        @Override public String id() { return "sha224"; }
        @Override public String name() { return "SHA-224"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWith("SHA-224", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(1300); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Sha256 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

        @Override public String id() { return "sha256"; }
        @Override public String name() { return "SHA-256"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWith("SHA-256", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(1400); }
        @Override public Optional<String> johnFormat() { return Optional.of("raw-sha256"); }
    }

    public static final class Sha384 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{96}$");

        @Override public String id() { return "sha384"; }
        @Override public String name() { return "SHA-384"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWith("SHA-384", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(10800); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Sha512 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{128}$");

        @Override public String id() { return "sha512"; }
        @Override public String name() { return "SHA-512"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWith("SHA-512", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(1700); }
        @Override public Optional<String> johnFormat() { return Optional.of("raw-sha512"); }
    }

    public static final class Sha3_256 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

        @Override public String id() { return "sha3_256"; }
        @Override public String name() { return "SHA3-256"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 30; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("SHA3-256", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(17400); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Sha3_512 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{128}$");

        @Override public String id() { return "sha3_512"; }
        @Override public String name() { return "SHA3-512"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 30; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("SHA3-512", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(17600); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Ripemd160 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{40}$");

        @Override public String id() { return "ripemd160"; }
        @Override public String name() { return "RIPEMD-160"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 30; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("RIPEMD160", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(6000); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Whirlpool implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{128}$");

        @Override public String id() { return "whirlpool"; }
        @Override public String name() { return "Whirlpool"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 25; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("Whirlpool", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(6100); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Blake2b256 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

        @Override public String id() { return "blake2b256"; }
        @Override public String name() { return "BLAKE2b-256"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 25; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("BLAKE2B-256", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(600); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Blake2b512 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{128}$");

        @Override public String id() { return "blake2b512"; }
        @Override public String name() { return "BLAKE2b-512"; }
        @Override public String category() { return "cryptographic"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 20; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return digestWithBC("BLAKE2B-512", input);
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }
}
