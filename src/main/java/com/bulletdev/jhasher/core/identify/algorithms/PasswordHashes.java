package com.bulletdev.jhasher.core.identify.algorithms;

import com.bulletdev.jhasher.core.identify.HashAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

public final class PasswordHashes {

    private PasswordHashes() {}

    public static final class BCryptHash implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

        @Override public String id() { return "bcrypt"; }
        @Override public String name() { return "bcrypt"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            try {
                int cost = Integer.parseInt(options.getOrDefault("cost", "12"));
                byte[] salt = new byte[16];
                new java.security.SecureRandom().nextBytes(salt);
                String hash = OpenBSDBCrypt.generate(input.toCharArray(), salt, cost);
                return Optional.of(hash);
            } catch (Exception e) {
                return Optional.empty();
            }
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(3200); }
        @Override public Optional<String> johnFormat() { return Optional.of("bcrypt"); }
    }

    public static final class Argon2Hash implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$argon2(id?|d)\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$.+$");

        @Override public String id() { return "argon2"; }
        @Override public String name() { return "Argon2"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(32600); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class ScryptHash implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$(scrypt|s0)\\$");

        @Override public String id() { return "scrypt"; }
        @Override public String name() { return "scrypt"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 90; }

        @Override
        public boolean matches(String hash) {
            return hash != null && PATTERN.matcher(hash).find();
        }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class YescryptHash implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$y\\$");

        @Override public String id() { return "yescrypt"; }
        @Override public String name() { return "yescrypt"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 90; }

        @Override
        public boolean matches(String hash) {
            return hash != null && PATTERN.matcher(hash).find();
        }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class Pbkdf2Hash implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$pbkdf2");

        @Override public String id() { return "pbkdf2"; }
        @Override public String name() { return "PBKDF2"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 85; }

        @Override
        public boolean matches(String hash) {
            return hash != null && PATTERN.matcher(hash).find();
        }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.empty(); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class LinuxSha512Crypt implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$6\\$.+\\$.+$");

        @Override public String id() { return "linux_sha512crypt"; }
        @Override public String name() { return "Linux SHA-512 crypt"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(1800); }
        @Override public Optional<String> johnFormat() { return Optional.of("sha512crypt"); }
    }

    public static final class LinuxSha256Crypt implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$5\\$.+\\$.+$");

        @Override public String id() { return "linux_sha256crypt"; }
        @Override public String name() { return "Linux SHA-256 crypt"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(7400); }
        @Override public Optional<String> johnFormat() { return Optional.of("sha256crypt"); }
    }

    public static final class LinuxMd5Crypt implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$1\\$.+\\$.+$");

        @Override public String id() { return "linux_md5crypt"; }
        @Override public String name() { return "Linux MD5 crypt"; }
        @Override public String category() { return "password"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(500); }
        @Override public Optional<String> johnFormat() { return Optional.of("md5crypt"); }
    }
}
