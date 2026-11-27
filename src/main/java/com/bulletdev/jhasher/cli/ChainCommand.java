package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.output.OutputFormat;
import com.bulletdev.jhasher.core.output.OutputFormatter;
import com.bulletdev.jhasher.core.transform.TransformChain;
import com.bulletdev.jhasher.core.transform.TransformChain.StepResult;
import com.bulletdev.jhasher.core.transform.TransformRegistry;
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

@Command(name = "chain", description = "Transform pipeline (CyberChef in your terminal)")
public class ChainCommand implements Runnable {

    @ParentCommand
    JHasherCli parent;

    @Parameters(index = "0", description = "Recipe: pipe-delimited transforms (e.g. 'base64encode|upper')")
    String recipe;

    @Parameters(index = "1", arity = "0..1", description = "Input string")
    String input;

    @Option(names = "--file", description = "Read input from file")
    File file;

    @Option(names = "--show-steps", description = "Show intermediate step results")
    boolean showSteps;

    @Override
    public void run() {
        OutputFormat format = parent.outputFormat;

        String data = resolveInput();
        if (data == null) {
            System.err.println(OutputFormatter.formatError("No input provided. Pass input as argument, --file, or via stdin."));
            return;
        }

        try {
            TransformRegistry registry = TransformRegistry.getInstance();
            TransformChain chain = TransformChain.parse(recipe, registry);

            if (showSteps) {
                List<StepResult> steps = chain.executeWithIntermediates(data.getBytes(StandardCharsets.UTF_8));
                String result = new String(steps.getLast().output(), StandardCharsets.UTF_8);
                System.out.print(OutputFormatter.formatChainResult(result, true, steps, format));
            } else {
                String result = chain.executeString(data);
                System.out.print(OutputFormatter.formatChainResult(result, false, null, format));
            }
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
