package com.bulletdev.jhasher.core.output;

import com.bulletdev.jhasher.core.identify.ContextDatabase;
import com.bulletdev.jhasher.core.identify.HashContext;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;
import com.bulletdev.jhasher.core.jwt.JwtDecoder.JwtAnalysis;
import com.bulletdev.jhasher.core.transform.TransformChain.StepResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class OutputFormatter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    private OutputFormatter() {}

    private static boolean noColor() {
        return System.getenv("NO_COLOR") != null;
    }

    // ---- Identify results ----

    public static String formatIdentifyResults(List<HashMatch> matches, ContextDatabase contextDb, OutputFormat format) {
        if (matches.isEmpty()) {
            return formatError("No matching hash types found.");
        }

        return switch (format) {
            case JSON -> {
                var list = matches.stream().map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("algorithm", m.algorithm().name());
                    map.put("id", m.algorithm().id());
                    map.put("category", m.algorithm().category());
                    map.put("confidence", Math.round(m.confidence() * 100));
                    m.algorithm().hashcatMode().ifPresent(mode -> map.put("hashcatMode", mode));
                    m.algorithm().johnFormat().ifPresent(fmt -> map.put("johnFormat", fmt));
                    Optional<HashContext> ctx = contextDb.getContext(m.algorithm().id());
                    ctx.ifPresent(c -> {
                        map.put("commonSources", c.commonSources());
                        map.put("strengthRating", c.strengthRating());
                        map.put("attackNotes", c.attackNotes());
                    });
                    return map;
                }).toList();
                yield GSON.toJson(list);
            }
            case TABLE -> {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-25s %-12s %-12s %-15s%n", "Algorithm", "Confidence", "Hashcat", "John"));
                sb.append("-".repeat(70)).append("\n");
                for (HashMatch m : matches) {
                    String hashcat = m.algorithm().hashcatMode().map(String::valueOf).orElse("-");
                    String john = m.algorithm().johnFormat().orElse("-");
                    sb.append(String.format("%-25s %-12s %-12s %-15s%n",
                            m.algorithm().name(),
                            Math.round(m.confidence() * 100) + "%",
                            hashcat,
                            john));
                }
                yield sb.toString();
            }
            case TEXT -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < matches.size(); i++) {
                    HashMatch m = matches.get(i);
                    if (i == 0) {
                        sb.append(colorize("[+] ", Color.GREEN));
                    } else {
                        sb.append(colorize("[*] ", Color.CYAN));
                    }
                    sb.append(colorize(m.algorithm().name(), Color.WHITE));
                    sb.append(" - Confidence: ");
                    sb.append(colorize(Math.round(m.confidence() * 100) + "%", Color.YELLOW));
                    sb.append("\n");

                    m.algorithm().hashcatMode().ifPresent(mode ->
                            sb.append("    Hashcat mode: ").append(mode).append("\n"));
                    m.algorithm().johnFormat().ifPresent(fmt ->
                            sb.append("    John format:  ").append(fmt).append("\n"));

                    Optional<HashContext> ctx = contextDb.getContext(m.algorithm().id());
                    ctx.ifPresent(c -> {
                        if (c.commonSources() != null && !c.commonSources().isEmpty()) {
                            sb.append("    Sources:      ").append(String.join(", ", c.commonSources())).append("\n");
                        }
                        if (c.strengthRating() != null) {
                            sb.append("    Strength:     ").append(c.strengthRating());
                            if (c.strengthNote() != null) {
                                sb.append(" - ").append(c.strengthNote());
                            }
                            sb.append("\n");
                        }
                        if (c.attackNotes() != null && !c.attackNotes().isEmpty()) {
                            sb.append("    Attacks:      ").append(String.join("; ", c.attackNotes())).append("\n");
                        }
                    });
                }
                yield sb.toString();
            }
        };
    }

    // ---- Chain result ----

    public static String formatChainResult(String result, boolean showSteps, List<StepResult> steps, OutputFormat format) {
        return switch (format) {
            case JSON -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("result", result);
                if (showSteps && steps != null) {
                    var stepList = steps.stream().map(s -> {
                        Map<String, Object> sm = new LinkedHashMap<>();
                        sm.put("transform", s.transformId());
                        sm.put("output", new String(s.output(), StandardCharsets.UTF_8));
                        sm.put("durationNanos", s.durationNanos());
                        return sm;
                    }).toList();
                    map.put("steps", stepList);
                }
                yield GSON.toJson(map);
            }
            case TABLE -> {
                StringBuilder sb = new StringBuilder();
                if (showSteps && steps != null) {
                    sb.append(String.format("%-20s %-12s %s%n", "Transform", "Duration", "Output"));
                    sb.append("-".repeat(70)).append("\n");
                    for (StepResult s : steps) {
                        String output = new String(s.output(), StandardCharsets.UTF_8);
                        if (output.length() > 40) {
                            output = output.substring(0, 37) + "...";
                        }
                        sb.append(String.format("%-20s %-12s %s%n",
                                s.transformId(),
                                formatDuration(s.durationNanos()),
                                output));
                    }
                    sb.append("-".repeat(70)).append("\n");
                }
                sb.append("Result: ").append(result).append("\n");
                yield sb.toString();
            }
            case TEXT -> {
                StringBuilder sb = new StringBuilder();
                if (showSteps && steps != null) {
                    for (int i = 0; i < steps.size(); i++) {
                        StepResult s = steps.get(i);
                        sb.append(colorize("  Step " + (i + 1) + ": ", Color.CYAN));
                        sb.append(s.transformId());
                        sb.append(colorize(" (" + formatDuration(s.durationNanos()) + ")", Color.DEFAULT));
                        sb.append("\n");
                        sb.append("    -> ").append(new String(s.output(), StandardCharsets.UTF_8)).append("\n");
                    }
                    sb.append("\n");
                }
                sb.append(colorize("Result: ", Color.GREEN));
                sb.append(result).append("\n");
                yield sb.toString();
            }
        };
    }

    // ---- JWT analysis ----

    public static String formatJwtAnalysis(JwtAnalysis analysis, OutputFormat format) {
        return switch (format) {
            case JSON -> GSON.toJson(analysis);
            case TABLE -> {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%-15s %s%n", "Algorithm:", analysis.algorithm()));
                sb.append(String.format("%-15s %s%n", "Header:", GSON.toJson(analysis.header())));
                sb.append(String.format("%-15s %s%n", "Payload:", GSON.toJson(analysis.payload())));
                sb.append(String.format("%-15s %s%n", "Signature:", analysis.signatureBase64()));
                if (analysis.timestamps() != null) {
                    if (analysis.timestamps().issuedAt() != null) {
                        sb.append(String.format("%-15s %s%n", "Issued At:", analysis.timestamps().issuedAt()));
                    }
                    if (analysis.timestamps().expiresAt() != null) {
                        sb.append(String.format("%-15s %s%n", "Expires At:", analysis.timestamps().expiresAt()));
                    }
                    sb.append(String.format("%-15s %s%n", "Expired:", analysis.timestamps().isExpired()));
                }
                if (analysis.warnings() != null && !analysis.warnings().isEmpty()) {
                    sb.append(String.format("%-15s %s%n", "Warnings:", String.join("; ", analysis.warnings())));
                }
                yield sb.toString();
            }
            case TEXT -> {
                StringBuilder sb = new StringBuilder();
                sb.append(colorize("[JWT Analysis]\n", Color.CYAN));
                sb.append(colorize("  Algorithm: ", Color.YELLOW)).append(analysis.algorithm()).append("\n\n");

                sb.append(colorize("  Header:\n", Color.GREEN));
                for (var entry : analysis.header().entrySet()) {
                    sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }

                sb.append(colorize("\n  Payload:\n", Color.GREEN));
                for (var entry : analysis.payload().entrySet()) {
                    sb.append("    ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }

                sb.append(colorize("\n  Signature: ", Color.GREEN)).append(analysis.signatureBase64()).append("\n");

                if (analysis.timestamps() != null) {
                    sb.append(colorize("\n  Timestamps:\n", Color.GREEN));
                    if (analysis.timestamps().issuedAt() != null) {
                        sb.append("    Issued At:  ").append(analysis.timestamps().issuedAt()).append("\n");
                    }
                    if (analysis.timestamps().expiresAt() != null) {
                        sb.append("    Expires At: ").append(analysis.timestamps().expiresAt()).append("\n");
                    }
                    if (analysis.timestamps().isExpired()) {
                        sb.append(colorize("    ** TOKEN IS EXPIRED **\n", Color.RED));
                    }
                }

                if (analysis.warnings() != null && !analysis.warnings().isEmpty()) {
                    sb.append(colorize("\n  Warnings:\n", Color.YELLOW));
                    for (String w : analysis.warnings()) {
                        sb.append(colorize("    ! ", Color.YELLOW)).append(w).append("\n");
                    }
                }
                yield sb.toString();
            }
        };
    }

    // ---- Generate result ----

    public static String formatGenerateResult(String algo, String hash, OutputFormat format) {
        return switch (format) {
            case JSON -> {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("algorithm", algo);
                map.put("hash", hash);
                yield GSON.toJson(map);
            }
            case TABLE -> String.format("%-20s %s%n", algo, hash);
            case TEXT -> colorize(algo + ": ", Color.CYAN) + hash + "\n";
        };
    }

    // ---- Utility ----

    public static String formatError(String message) {
        if (noColor()) {
            return "[ERROR] " + message;
        }
        return Ansi.ansi().fgBright(Color.RED).a("[ERROR] ").a(message).reset().toString();
    }

    public static String formatSuccess(String message) {
        if (noColor()) {
            return "[OK] " + message;
        }
        return Ansi.ansi().fgBright(Color.GREEN).a("[OK] ").a(message).reset().toString();
    }

    private static String colorize(String text, Color color) {
        if (noColor()) {
            return text;
        }
        return Ansi.ansi().fgBright(color).a(text).reset().toString();
    }

    private static String formatDuration(long nanos) {
        if (nanos < 1_000) {
            return nanos + "ns";
        } else if (nanos < 1_000_000) {
            return String.format("%.1fus", nanos / 1_000.0);
        } else {
            return String.format("%.1fms", nanos / 1_000_000.0);
        }
    }
}
