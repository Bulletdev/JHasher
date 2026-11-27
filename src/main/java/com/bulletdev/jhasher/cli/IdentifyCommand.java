package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.identify.ContextDatabase;
import com.bulletdev.jhasher.core.identify.HashIdentifier;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;
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
import java.util.List;

@Command(name = "identify", description = "Identify hash type with context")
public class IdentifyCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Parameters(index = "0", arity = "0..1", description = "Hash string to identify")
    String hash;

    @Option(names = "--file", description = "File with one hash per line")
    File file;

    @Option(names = "--hashcat-mode", description = "Output only the hashcat mode number")
    boolean hashcatOnly;

    @Option(names = "--json", description = "Output in JSON format")
    boolean json;

    @Override
    public void run() {
        HashIdentifier identifier = new HashIdentifier();
        ContextDatabase contextDb = ContextDatabase.getInstance();
        OutputFormat format = json ? OutputFormat.JSON : parent.outputFormat;

        if (hash != null) {
            processHash(hash.trim(), identifier, contextDb, format);
        } else if (file != null) {
            processFile(file, identifier, contextDb, format);
        } else {
            processStdin(identifier, contextDb, format);
        }
    }

    private void processHash(String input, HashIdentifier identifier, ContextDatabase contextDb, OutputFormat format) {
        List<HashMatch> matches = identifier.identify(input);
        if (hashcatOnly) {
            if (!matches.isEmpty()) {
                matches.getFirst().algorithm().hashcatMode().ifPresent(mode -> System.out.println(mode));
            }
            return;
        }
        System.out.print(OutputFormatter.formatIdentifyResults(matches, contextDb, format));
    }

    private void processFile(File f, HashIdentifier identifier, ContextDatabase contextDb, OutputFormat format) {
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    System.out.println("--- " + trimmed + " ---");
                    processHash(trimmed, identifier, contextDb, format);
                    System.out.println();
                }
            }
        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError("Failed to read file: " + e.getMessage()));
        }
    }

    private void processStdin(HashIdentifier identifier, ContextDatabase contextDb, OutputFormat format) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    processHash(trimmed, identifier, contextDb, format);
                }
            }
        } catch (Exception e) {
            System.err.println(OutputFormatter.formatError("Failed to read stdin: " + e.getMessage()));
        }
    }
}
