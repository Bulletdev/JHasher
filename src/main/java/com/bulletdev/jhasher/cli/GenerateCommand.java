package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.generate.HashGenerator;
import com.bulletdev.jhasher.core.output.OutputFormat;
import com.bulletdev.jhasher.core.output.OutputFormatter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(name = "generate", description = "Generate hash from input")
public class GenerateCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Parameters(index = "0", description = "Hash algorithm (e.g. md5, sha256)")
    String algorithm;

    @Parameters(index = "1", arity = "0..1", description = "Input string to hash")
    String input;

    @Option(names = "--file", description = "Read input from file")
    File file;

    @Option(names = "--verify", description = "Expected hash to verify against")
    String expectedHash;

    @Option(names = "--salt", description = "Salt for hash generation")
    String salt;

    @Option(names = "--rounds", description = "Number of rounds")
    String rounds;

    @Option(names = "--all", description = "Generate with all algorithms")
    boolean allAlgorithms;

    @Override
    public void run() {
        HashGenerator generator = new HashGenerator();
        OutputFormat format = parent.outputFormat;

        String data = resolveInput();
        if (data == null) {
            System.err.println(OutputFormatter.formatError("No input provided. Pass input as argument, --file, or via stdin."));
            return;
        }

        try {
            if (allAlgorithms) {
                List<Map.Entry<String, String>> results = generator.generateAll(data);
                for (Map.Entry<String, String> entry : results) {
                    System.out.print(OutputFormatter.formatGenerateResult(entry.getKey(), entry.getValue(), format));
                }
                return;
            }

            if (expectedHash != null) {
                boolean match = generator.verify(algorithm, data, expectedHash);
                if (match) {
                    System.out.println(OutputFormatter.formatSuccess("Hash matches!"));
                } else {
                    System.out.println(OutputFormatter.formatError("Hash does NOT match."));
                }
                return;
            }

            Map<String, String> options = new LinkedHashMap<>();
            if (salt != null) {
                options.put("salt", salt);
            }
            if (rounds != null) {
                options.put("rounds", rounds);
            }

            String hash = generator.generate(algorithm, data, options);
            System.out.print(OutputFormatter.formatGenerateResult(algorithm, hash, format));

        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError(e.getMessage()));
        }
    }

    private String resolveInput() {
        if (input != null) {
            return input;
        }
        if (file != null) {
            try {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                System.err.println(OutputFormatter.formatError("Failed to read file: " + e.getMessage()));
                return null;
            }
        }
        // Read from stdin
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
