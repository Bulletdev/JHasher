package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.jwt.JwtAttacker;
import com.bulletdev.jhasher.core.jwt.JwtAttacker.AttackResult;
import com.bulletdev.jhasher.core.jwt.JwtDecoder;
import com.bulletdev.jhasher.core.jwt.JwtDecoder.JwtAnalysis;
import com.bulletdev.jhasher.core.jwt.JwtForger;
import com.bulletdev.jhasher.core.output.OutputFormat;
import com.bulletdev.jhasher.core.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(name = "jwt", description = "JWT operations",
        subcommands = {JwtCommand.JwtDecodeCmd.class, JwtCommand.JwtAttackCmd.class, JwtCommand.JwtForgeCmd.class})
public class JwtCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Override
    public void run() {
        picocli.CommandLine.usage(this, System.out);
    }

    @Command(name = "decode", description = "Decode and analyze a JWT token")
    public static class JwtDecodeCmd implements Runnable {

        @ParentCommand
        JwtCommand parent;

        @Parameters(index = "0", description = "JWT token string")
        String token;

        @Override
        public void run() {
            OutputFormat format = parent.parent.outputFormat;
            try {
                JwtDecoder decoder = new JwtDecoder();
                JwtAnalysis analysis = decoder.decode(token);
                System.out.print(OutputFormatter.formatJwtAnalysis(analysis, format));
            } catch (Exception e) {
                System.err.println(OutputFormatter.formatError(e.getMessage()));
            }
        }
    }

    @Command(name = "attack", description = "Run attacks against a JWT token")
    public static class JwtAttackCmd implements Runnable {

        @ParentCommand
        JwtCommand parent;

        @Parameters(index = "0", description = "JWT token string")
        String token;

        @Option(names = "--url", description = "Target URL for verification")
        String targetUrl;

        @Option(names = "--wordlist", description = "File with secrets to try (one per line)")
        File wordlist;

        @Option(names = "--public-key", description = "Public key file for HMAC confusion attack")
        File publicKeyFile;

        @Override
        public void run() {
            try {
                JwtAttacker attacker = new JwtAttacker();
                Map<String, String> options = new LinkedHashMap<>();

                if (publicKeyFile != null) {
                    String pk = Files.readString(publicKeyFile.toPath(), StandardCharsets.UTF_8);
                    options.put("publicKey", pk);
                }

                if (wordlist != null) {
                    // Read wordlist and pass as comma-separated
                    List<String> secrets = Files.readAllLines(wordlist.toPath(), StandardCharsets.UTF_8);
                    options.put("wordlist", String.join(",", secrets));
                }

                List<AttackResult> results = attacker.runAll(token, options);

                for (AttackResult result : results) {
                    if (result.success()) {
                        System.out.println(OutputFormatter.formatSuccess(
                                result.attackName() + ": " + result.description()));
                        if (!result.forgedToken().isEmpty()) {
                            System.out.println("  Token: " + result.forgedToken());
                        }
                    } else {
                        System.out.println(OutputFormatter.formatError(
                                result.attackName() + ": " + result.description()));
                    }
                    System.out.println();
                }
            } catch (Exception e) {
                System.err.println(OutputFormatter.formatError(e.getMessage()));
            }
        }
    }

    @Command(name = "forge", description = "Forge a JWT token")
    public static class JwtForgeCmd implements Runnable {

        @ParentCommand
        JwtCommand parent;

        @Option(names = "--alg", required = true, description = "Algorithm (e.g. HS256, none)")
        String algorithm;

        @Option(names = "--payload", required = true, description = "Payload as JSON string")
        String payloadJson;

        @Option(names = "--key", description = "Signing key")
        String key;

        @Override
        public void run() {
            try {
                JwtForger forger = new JwtForger();
                String token = forger.forge(algorithm, payloadJson, key != null ? key : "");
                System.out.println(token);
            } catch (Exception e) {
                System.err.println(OutputFormatter.formatError(e.getMessage()));
            }
        }
    }
}
