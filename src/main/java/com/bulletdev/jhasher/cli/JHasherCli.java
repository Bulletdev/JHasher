package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.output.OutputFormat;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "jhasher", mixinStandardHelpOptions = true, version = "JHasher 2.0.0",
        description = "Hash & Crypto Swiss Army Knife",
        subcommands = {
                IdentifyCommand.class,
                GenerateCommand.class,
                ChainCommand.class,
                JwtCommand.class,
                EncryptCommand.class,
                DecryptCommand.class,
                KeygenCommand.class
        })
public class JHasherCli implements Runnable {

    @Option(names = {"-o", "--output"}, defaultValue = "TEXT",
            description = "Output format: TEXT, JSON, TABLE")
    OutputFormat outputFormat;

    @Option(names = "--no-color", description = "Disable colored output")
    boolean noColor;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int exitCode = new CommandLine(new JHasherCli()).execute(args);
        AnsiConsole.systemUninstall();
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
