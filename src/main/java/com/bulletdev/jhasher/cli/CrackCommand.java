package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.crack.CrackListener;
import com.bulletdev.jhasher.core.crack.CrackResult;
import com.bulletdev.jhasher.core.crack.DictionaryCracker;
import com.bulletdev.jhasher.core.crack.WordlistSource;
import com.bulletdev.jhasher.core.identify.HashIdentifier;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;
import com.bulletdev.jhasher.core.identify.HashRegistry;
import com.bulletdev.jhasher.core.output.OutputFormatter;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Command(name = "crack", description = "Quick dictionary crack (lightweight, not a Hashcat replacement)")
public class CrackCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Parameters(index = "0", description = "Hash to crack")
    String hash;

    @Option(names = "--wordlist", description = "Path to wordlist file")
    File wordlistFile;

    @Option(names = "--algo", description = "Algorithm (auto-detect if omitted)")
    String algorithm;

    @Option(names = "--threads", defaultValue = "4", description = "Number of threads (default: 4)")
    int threads;

    @Override
    public void run() {
        WordlistSource wordlist;
        try {
            if (wordlistFile != null) {
                wordlist = WordlistSource.fromFile(wordlistFile.toPath());
            } else {
                wordlist = WordlistSource.bundled("top1000");
            }
        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError("Failed to load wordlist: " + e.getMessage()));
            return;
        }

        // Show what we are doing
        String algoDisplay = algorithm;
        if (algoDisplay == null) {
            HashIdentifier identifier = new HashIdentifier();
            List<HashMatch> matches = identifier.identify(hash);
            if (!matches.isEmpty()) {
                algoDisplay = matches.getFirst().algorithm().id();
                System.out.println(colorize("[*] ", Color.CYAN) + "Auto-detected algorithm: "
                        + colorize(matches.getFirst().algorithm().name(), Color.WHITE));
            } else {
                System.err.println(OutputFormatter.formatError("Could not identify hash type. Use --algo to specify."));
                return;
            }
        }

        long totalEstimate = wordlist.estimateSize();
        String sourceLabel = wordlistFile != null ? wordlistFile.getName() : "built-in top-1000";
        System.out.println(colorize("[*] ", Color.CYAN) + "Wordlist: " + sourceLabel
                + (totalEstimate > 0 ? " (" + formatNumber(totalEstimate) + " words)" : ""));
        System.out.println(colorize("[*] ", Color.CYAN) + "Threads: " + threads);
        System.out.println();

        DictionaryCracker cracker = new DictionaryCracker();
        String detectedAlgo = algoDisplay;

        CrackListener listener = new CrackListener() {
            @Override
            public void onProgress(long attempts, long total, double speed) {
                printProgress(attempts, total, speed);
            }

            @Override
            public void onFound(String plaintext) {
                // Handled in onComplete
            }

            @Override
            public void onComplete(CrackResult result) {
                // Clear progress line
                System.out.print("\r" + " ".repeat(80) + "\r");

                if (result.found()) {
                    System.out.println(colorize("[+] FOUND: ", Color.GREEN)
                            + colorize(result.plaintext(), Color.WHITE));
                    System.out.println(colorize("    Algorithm: ", Color.CYAN) + result.algorithm());
                    System.out.println(colorize("    Attempts:  ", Color.CYAN) + formatNumber(result.attempts()));
                    System.out.println(colorize("    Time:      ", Color.CYAN) + formatDuration(result.durationMs()));
                    System.out.println(colorize("    Speed:     ", Color.CYAN) + formatSpeed(result.speed()));
                } else {
                    System.out.println(colorize("[-] NOT FOUND in wordlist.", Color.YELLOW));
                    System.out.println(colorize("    Attempts:  ", Color.CYAN) + formatNumber(result.attempts()));
                    System.out.println(colorize("    Time:      ", Color.CYAN) + formatDuration(result.durationMs()));
                    System.out.println();
                    printHashcatSuggestion(detectedAlgo);
                }
            }
        };

        try {
            if (threads > 1) {
                cracker.crackParallel(hash, algorithm, wordlist, listener, threads);
            } else {
                cracker.crack(hash, algorithm, wordlist, listener);
            }
        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError("Crack failed: " + e.getMessage()));
        }
    }

    private void printProgress(long attempts, long total, double speed) {
        if (total > 0) {
            int percent = (int) ((attempts * 100) / total);
            int barWidth = 30;
            int filled = (int) ((attempts * barWidth) / total);
            String bar = "=".repeat(filled) + ">" + " ".repeat(Math.max(0, barWidth - filled - 1));
            System.out.print(String.format("\r[%s] %s/%s | %s",
                    bar, formatNumber(attempts), formatNumber(total), formatSpeed(speed)));
        } else {
            System.out.print(String.format("\r[...] %s tried | %s",
                    formatNumber(attempts), formatSpeed(speed)));
        }
        System.out.flush();
    }

    private void printHashcatSuggestion(String algorithmId) {
        if (algorithmId == null) {
            return;
        }

        Optional<Integer> hashcatMode = HashRegistry.getInstance().getById(algorithmId)
                .flatMap(a -> a.hashcatMode());

        if (hashcatMode.isPresent()) {
            System.out.println(colorize("    Suggestion: ", Color.CYAN)
                    + "hashcat -m " + hashcatMode.get() + " hash.txt wordlist.txt -r best64.rule");
        } else {
            System.out.println(colorize("    Suggestion: ", Color.CYAN)
                    + "Try hashcat or john with a larger wordlist and rules.");
        }
    }

    private static String colorize(String text, Color color) {
        if (System.getenv("NO_COLOR") != null) {
            return text;
        }
        return Ansi.ansi().fgBright(color).a(text).reset().toString();
    }

    private static String formatNumber(long n) {
        return String.format("%,d", n);
    }

    private static String formatSpeed(double hashesPerSec) {
        if (hashesPerSec >= 1_000_000) {
            return String.format("%.1fM h/s", hashesPerSec / 1_000_000);
        } else if (hashesPerSec >= 1_000) {
            return String.format("%.0fK h/s", hashesPerSec / 1_000);
        } else {
            return String.format("%.0f h/s", hashesPerSec);
        }
    }

    private static String formatDuration(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60_000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            long minutes = ms / 60_000;
            long seconds = (ms % 60_000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
}
