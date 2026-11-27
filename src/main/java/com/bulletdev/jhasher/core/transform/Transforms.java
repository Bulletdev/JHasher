package com.bulletdev.jhasher.core.transform;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * All built-in transforms for JHasher's CyberChef-style transform chain.
 */
public final class Transforms {

    private Transforms() {}

    // -----------------------------------------------------------------------
    // ENCODING
    // -----------------------------------------------------------------------

    public static final class Base64Encode implements Transform {
        @Override public String id() { return "base64encode"; }
        @Override public String name() { return "Base64 Encode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return Base64.getEncoder().encode(input);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Base64Decode implements Transform {
        @Override public String id() { return "base64decode"; }
        @Override public String name() { return "Base64 Decode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return Base64.getDecoder().decode(input);
            } catch (IllegalArgumentException e) {
                return errorBytes(e);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Base64UrlEncode implements Transform {
        @Override public String id() { return "base64url-encode"; }
        @Override public String name() { return "Base64 URL Encode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return Base64.getUrlEncoder().withoutPadding().encode(input);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Base64UrlDecode implements Transform {
        @Override public String id() { return "base64url-decode"; }
        @Override public String name() { return "Base64 URL Decode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return Base64.getUrlDecoder().decode(input);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class HexEncode implements Transform {
        @Override public String id() { return "hex-encode"; }
        @Override public String name() { return "Hex Encode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return HexFormat.of().formatHex(input).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class HexDecode implements Transform {
        @Override public String id() { return "hex-decode"; }
        @Override public String name() { return "Hex Decode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String inputString = new String(input, StandardCharsets.UTF_8).trim();
                return HexFormat.of().parseHex(inputString);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class UrlEncode implements Transform {
        @Override public String id() { return "urlencode"; }
        @Override public String name() { return "URL Encode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                return URLEncoder.encode(s, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class UrlDecode implements Transform {
        @Override public String id() { return "urldecode"; }
        @Override public String name() { return "URL Decode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                return URLDecoder.decode(s, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class HtmlEncode implements Transform {
        @Override public String id() { return "html-encode"; }
        @Override public String name() { return "HTML Encode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                String encoded = s
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&#39;");
                return encoded.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class HtmlDecode implements Transform {
        @Override public String id() { return "html-decode"; }
        @Override public String name() { return "HTML Decode"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                String decoded = s
                        .replace("&#39;", "'")
                        .replace("&quot;", "\"")
                        .replace("&gt;", ">")
                        .replace("&lt;", "<")
                        .replace("&amp;", "&");
                return decoded.getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class UnicodeEscape implements Transform {
        @Override public String id() { return "unicode-escape"; }
        @Override public String name() { return "Unicode Escape"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder();
                for (char c : s.toCharArray()) {
                    sb.append(String.format("\\u%04x", (int) c));
                }
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class UnicodeUnescape implements Transform {
        @Override public String id() { return "unicode-unescape"; }
        @Override public String name() { return "Unicode Unescape"; }
        @Override public String category() { return "encoding"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
                Matcher matcher = pattern.matcher(s);
                StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    char c = (char) Integer.parseInt(matcher.group(1), 16);
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(c)));
                }
                matcher.appendTail(sb);
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // HASHING
    // -----------------------------------------------------------------------

    public static final class Md5Hash implements Transform {
        @Override public String id() { return "md5"; }
        @Override public String name() { return "MD5"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            return digestHex(input, "MD5");
        }
    }

    public static final class Sha1Hash implements Transform {
        @Override public String id() { return "sha1"; }
        @Override public String name() { return "SHA-1"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            return digestHex(input, "SHA-1");
        }
    }

    public static final class Sha256Hash implements Transform {
        @Override public String id() { return "sha256"; }
        @Override public String name() { return "SHA-256"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            return digestHex(input, "SHA-256");
        }
    }

    public static final class Sha384Hash implements Transform {
        @Override public String id() { return "sha384"; }
        @Override public String name() { return "SHA-384"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            return digestHex(input, "SHA-384");
        }
    }

    public static final class Sha512Hash implements Transform {
        @Override public String id() { return "sha512"; }
        @Override public String name() { return "SHA-512"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            return digestHex(input, "SHA-512");
        }
    }

    public static final class Crc32Hash implements Transform {
        @Override public String id() { return "crc32"; }
        @Override public String name() { return "CRC32"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                CRC32 crc = new CRC32();
                crc.update(input);
                return String.format("%08x", crc.getValue()).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class HmacSha256 implements Transform {
        @Override public String id() { return "hmac-sha256"; }
        @Override public String name() { return "HMAC-SHA256"; }
        @Override public String category() { return "hashing"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String key = params.get("key");
                if (key == null || key.isEmpty()) {
                    return "[ERROR: --key parameter is required for hmac-sha256]".getBytes(StandardCharsets.UTF_8);
                }
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] hash = mac.doFinal(input);
                return HexFormat.of().formatHex(hash).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // CRYPTO
    // -----------------------------------------------------------------------

    public static final class AesGcmEncrypt implements Transform {
        @Override public String id() { return "aes-gcm-encrypt"; }
        @Override public String name() { return "AES-GCM Encrypt"; }
        @Override public String category() { return "crypto"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String key = params.get("key");
                if (key == null || key.isEmpty()) {
                    return "[ERROR: --key parameter is required for aes-gcm-encrypt]".getBytes(StandardCharsets.UTF_8);
                }
                byte[] keyBytes = deriveAesKey(key);
                byte[] iv = new byte[12];
                new SecureRandom().nextBytes(iv);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                        new GCMParameterSpec(128, iv));
                byte[] ciphertext = cipher.doFinal(input);

                // Output = IV (12 bytes) + ciphertext + tag
                byte[] output = new byte[iv.length + ciphertext.length];
                System.arraycopy(iv, 0, output, 0, iv.length);
                System.arraycopy(ciphertext, 0, output, iv.length, ciphertext.length);
                return output;
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class AesGcmDecrypt implements Transform {
        @Override public String id() { return "aes-gcm-decrypt"; }
        @Override public String name() { return "AES-GCM Decrypt"; }
        @Override public String category() { return "crypto"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String key = params.get("key");
                if (key == null || key.isEmpty()) {
                    return "[ERROR: --key parameter is required for aes-gcm-decrypt]".getBytes(StandardCharsets.UTF_8);
                }
                if (input.length < 12) {
                    return "[ERROR: input too short for AES-GCM decryption (need at least 12 bytes for IV)]"
                            .getBytes(StandardCharsets.UTF_8);
                }
                byte[] keyBytes = deriveAesKey(key);
                byte[] iv = Arrays.copyOfRange(input, 0, 12);
                byte[] ciphertext = Arrays.copyOfRange(input, 12, input.length);

                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"),
                        new GCMParameterSpec(128, iv));
                return cipher.doFinal(ciphertext);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class XorTransform implements Transform {
        @Override public String id() { return "xor"; }
        @Override public String name() { return "XOR"; }
        @Override public String category() { return "crypto"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String key = params.get("key");
                if (key == null || key.isEmpty()) {
                    return "[ERROR: --key parameter is required for xor]".getBytes(StandardCharsets.UTF_8);
                }
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                byte[] output = new byte[input.length];
                for (int i = 0; i < input.length; i++) {
                    output[i] = (byte) (input[i] ^ keyBytes[i % keyBytes.length]);
                }
                return output;
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Rot13 implements Transform {
        @Override public String id() { return "rot13"; }
        @Override public String name() { return "ROT13"; }
        @Override public String category() { return "crypto"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder(s.length());
                for (char c : s.toCharArray()) {
                    if (c >= 'a' && c <= 'z') {
                        sb.append((char) ('a' + (c - 'a' + 13) % 26));
                    } else if (c >= 'A' && c <= 'Z') {
                        sb.append((char) ('A' + (c - 'A' + 13) % 26));
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class RotN implements Transform {
        @Override public String id() { return "rotn"; }
        @Override public String name() { return "ROT-N"; }
        @Override public String category() { return "crypto"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String nStr = params.get("n");
                if (nStr == null || nStr.isEmpty()) {
                    return "[ERROR: --n parameter is required for rotn]".getBytes(StandardCharsets.UTF_8);
                }
                int n = ((Integer.parseInt(nStr) % 26) + 26) % 26;
                String s = new String(input, StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder(s.length());
                for (char c : s.toCharArray()) {
                    if (c >= 'a' && c <= 'z') {
                        sb.append((char) ('a' + (c - 'a' + n) % 26));
                    } else if (c >= 'A' && c <= 'Z') {
                        sb.append((char) ('A' + (c - 'A' + n) % 26));
                    } else {
                        sb.append(c);
                    }
                }
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // TEXT
    // -----------------------------------------------------------------------

    public static final class Upper implements Transform {
        @Override public String id() { return "upper"; }
        @Override public String name() { return "Uppercase"; }
        @Override public String category() { return "text"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return new String(input, StandardCharsets.UTF_8).toUpperCase()
                        .getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Lower implements Transform {
        @Override public String id() { return "lower"; }
        @Override public String name() { return "Lowercase"; }
        @Override public String category() { return "text"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return new String(input, StandardCharsets.UTF_8).toLowerCase()
                        .getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Reverse implements Transform {
        @Override public String id() { return "reverse"; }
        @Override public String name() { return "Reverse"; }
        @Override public String category() { return "text"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String s = new String(input, StandardCharsets.UTF_8);
                return new StringBuilder(s).reverse().toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Length implements Transform {
        @Override public String id() { return "length"; }
        @Override public String name() { return "Length"; }
        @Override public String category() { return "text"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                return String.valueOf(input.length).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class Repeat implements Transform {
        @Override public String id() { return "repeat"; }
        @Override public String name() { return "Repeat"; }
        @Override public String category() { return "text"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String nStr = params.get("n");
                if (nStr == null || nStr.isEmpty()) {
                    return "[ERROR: --n parameter is required for repeat]".getBytes(StandardCharsets.UTF_8);
                }
                int n = Integer.parseInt(nStr);
                if (n < 0) {
                    return "[ERROR: --n must be non-negative]".getBytes(StandardCharsets.UTF_8);
                }
                String s = new String(input, StandardCharsets.UTF_8);
                return s.repeat(n).getBytes(StandardCharsets.UTF_8);
            } catch (NumberFormatException e) {
                return "[ERROR: --n must be a valid integer]".getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class ReplaceTransform implements Transform {
        @Override public String id() { return "replace"; }
        @Override public String name() { return "Replace"; }
        @Override public String category() { return "text"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                String from = params.get("from");
                String to = params.get("to");
                if (from == null) {
                    return "[ERROR: --from parameter is required for replace]".getBytes(StandardCharsets.UTF_8);
                }
                if (to == null) {
                    to = "";
                }
                String s = new String(input, StandardCharsets.UTF_8);
                return s.replace(from, to).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // COMPRESSION
    // -----------------------------------------------------------------------

    public static final class GzipCompress implements Transform {
        @Override public String id() { return "gzip"; }
        @Override public String name() { return "Gzip Compress"; }
        @Override public String category() { return "compression"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                    gzos.write(input);
                }
                return baos.toByteArray();
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class GzipDecompress implements Transform {
        @Override public String id() { return "gunzip"; }
        @Override public String name() { return "Gzip Decompress"; }
        @Override public String category() { return "compression"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(input);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = gzis.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                }
                return baos.toByteArray();
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // ANALYSIS
    // -----------------------------------------------------------------------

    public static final class Entropy implements Transform {
        @Override public String id() { return "entropy"; }
        @Override public String name() { return "Shannon Entropy"; }
        @Override public String category() { return "analysis"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                if (input.length == 0) {
                    return "0.000".getBytes(StandardCharsets.UTF_8);
                }
                int[] freq = new int[256];
                for (byte b : input) {
                    freq[b & 0xFF]++;
                }
                double entropy = 0.0;
                double len = input.length;
                for (int f : freq) {
                    if (f > 0) {
                        double p = f / len;
                        entropy -= p * (Math.log(p) / Math.log(2));
                    }
                }
                return String.format("%.3f", entropy).getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    public static final class ByteFrequency implements Transform {
        @Override public String id() { return "freq"; }
        @Override public String name() { return "Byte Frequency"; }
        @Override public String category() { return "analysis"; }
        @Override public byte[] execute(byte[] input, Map<String, String> params) {
            try {
                if (input.length == 0) {
                    return "(empty input)".getBytes(StandardCharsets.UTF_8);
                }
                int[] freq = new int[256];
                for (byte b : input) {
                    freq[b & 0xFF]++;
                }
                // Build list of (byteValue, count), sort descending by count
                Map<Integer, Integer> countMap = new LinkedHashMap<>();
                for (int i = 0; i < 256; i++) {
                    if (freq[i] > 0) {
                        countMap.put(i, freq[i]);
                    }
                }
                List<Map.Entry<Integer, Integer>> sorted = countMap.entrySet().stream()
                        .sorted((a, b1) -> Integer.compare(b1.getValue(), a.getValue()))
                        .limit(16)
                        .toList();

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-6s %-8s %-6s  %s%n", "Byte", "Hex", "Count", "Char"));
                sb.append("-".repeat(34)).append("\n");
                for (Map.Entry<Integer, Integer> entry : sorted) {
                    int byteVal = entry.getKey();
                    int count = entry.getValue();
                    String charRepr = (byteVal >= 32 && byteVal < 127) ? String.valueOf((char) byteVal) : ".";
                    sb.append(String.format("%-6d 0x%02x     %-6d  %s%n", byteVal, byteVal, count, charRepr));
                }
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            } catch (Exception e) {
                return errorBytes(e);
            }
        }
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private static byte[] digestHex(byte[] input, String algorithm) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(input);
            return HexFormat.of().formatHex(hash).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            return errorBytes(e);
        }
    }

    /**
     * Derive a 16-byte AES key from a passphrase using SHA-256 truncation.
     */
    private static byte[] deriveAesKey(String passphrase) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(passphrase.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, 16);
    }

    private static byte[] errorBytes(Exception e) {
        return ("[ERROR: " + e.getMessage() + "]").getBytes(StandardCharsets.UTF_8);
    }

    // -----------------------------------------------------------------------
    // ALL BUILT-IN TRANSFORMS
    // -----------------------------------------------------------------------

    public static final List<Transform> ALL = List.of(
            // Encoding
            new Base64Encode(),
            new Base64Decode(),
            new Base64UrlEncode(),
            new Base64UrlDecode(),
            new HexEncode(),
            new HexDecode(),
            new UrlEncode(),
            new UrlDecode(),
            new HtmlEncode(),
            new HtmlDecode(),
            new UnicodeEscape(),
            new UnicodeUnescape(),
            // Hashing
            new Md5Hash(),
            new Sha1Hash(),
            new Sha256Hash(),
            new Sha384Hash(),
            new Sha512Hash(),
            new Crc32Hash(),
            new HmacSha256(),
            // Crypto
            new AesGcmEncrypt(),
            new AesGcmDecrypt(),
            new XorTransform(),
            new Rot13(),
            new RotN(),
            // Text
            new Upper(),
            new Lower(),
            new Reverse(),
            new Length(),
            new Repeat(),
            new ReplaceTransform(),
            // Compression
            new GzipCompress(),
            new GzipDecompress(),
            // Analysis
            new Entropy(),
            new ByteFrequency()
    );
}
