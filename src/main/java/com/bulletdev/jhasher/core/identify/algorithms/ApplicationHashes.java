package com.bulletdev.jhasher.core.identify.algorithms;

import com.bulletdev.jhasher.core.identify.HashAlgorithm;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ApplicationHashes {

    private ApplicationHashes() {}

    public static final class Ntlm implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{32}$");

        @Override public String id() { return "ntlm"; }
        @Override public String name() { return "NTLM"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 25; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(1000); }
        @Override public Optional<String> johnFormat() { return Optional.of("nt"); }
    }

    public static final class MySqlOld implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{16}$");

        @Override public String id() { return "mysql_old"; }
        @Override public String name() { return "MySQL OLD"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 70; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(200); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class MySql5 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\*[A-F0-9]{40}$");

        @Override public String id() { return "mysql5"; }
        @Override public String name() { return "MySQL 5.x"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(300); }
        @Override public Optional<String> johnFormat() { return Optional.of("mysql-sha1"); }
    }

    public static final class PostgresMd5 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^md5[a-fA-F0-9]{32}$");

        @Override public String id() { return "postgresql_md5"; }
        @Override public String name() { return "PostgreSQL MD5"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(12); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class MsSql2005 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^0x0100[a-fA-F0-9]{48}$");

        @Override public String id() { return "mssql2005"; }
        @Override public String name() { return "MSSQL 2005"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(131); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class MsSql2012 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^0x0200[a-fA-F0-9]{128}$");

        @Override public String id() { return "mssql2012"; }
        @Override public String name() { return "MSSQL 2012+"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(1731); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class WordPress implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$P\\$[a-zA-Z0-9/.]{31}$");

        @Override public String id() { return "wordpress"; }
        @Override public String name() { return "WordPress (phpass)"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(400); }
        @Override public Optional<String> johnFormat() { return Optional.of("phpass"); }
    }

    public static final class Drupal implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^\\$S\\$[a-zA-Z0-9/.]{52}$");

        @Override public String id() { return "drupal"; }
        @Override public String name() { return "Drupal"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(7900); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class DjangoSha256 implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^pbkdf2_sha256\\$");

        @Override public String id() { return "django_sha256"; }
        @Override public String name() { return "Django PBKDF2-SHA256"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 95; }

        @Override
        public boolean matches(String hash) {
            return hash != null && PATTERN.matcher(hash).find();
        }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(10000); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }

    public static final class OracleH implements HashAlgorithm {
        private static final Pattern PATTERN = Pattern.compile("^[a-fA-F0-9]{16}$");

        @Override public String id() { return "oracle_h"; }
        @Override public String name() { return "Oracle H"; }
        @Override public String category() { return "application"; }
        @Override public Pattern pattern() { return PATTERN; }
        @Override public int priority() { return 20; }

        @Override
        public Optional<String> generate(String input, Map<String, String> options) {
            return Optional.empty();
        }

        @Override public Optional<Integer> hashcatMode() { return Optional.of(3100); }
        @Override public Optional<String> johnFormat() { return Optional.empty(); }
    }
}
