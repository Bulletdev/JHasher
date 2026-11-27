package com.bulletdev.jhasher.cli;

import com.bulletdev.jhasher.core.output.OutputFormat;
import com.bulletdev.jhasher.gui.JHasherGui;
import com.bulletdev.jhasher.tui.JHasherTui;
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
                KeygenCommand.class,
                CrackCommand.class
        })
public class JHasherCli implements Runnable {

    @Option(names = {"-o", "--output"}, defaultValue = "TEXT",
            description = "Output format: TEXT, JSON, TABLE")
    OutputFormat outputFormat;

    @Option(names = "--no-color", description = "Disable colored output")
    boolean noColor;

    @Option(names = "--gui", description = "Launch graphical interface")
    boolean gui;

    @Option(names = "--tui", description = "Launch terminal UI")
    boolean tui;

    public static void main(String[] args) {
        AnsiConsole.systemInstall();
        int exitCode = new CommandLine(new JHasherCli()).execute(args);
        AnsiConsole.systemUninstall();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    @Override
    public void run() {
        if (gui) {
            JHasherGui.launch();
            return;
        }
        if (tui) {
            try {
                JHasherTui.launch();
            } catch (Exception e) {
                System.err.println("Failed to launch TUI: " + e.getMessage());
            }
            return;
        }
        // No subcommand and no --gui/--tui: launch GUI by default
        JHasherGui.launch();
    }
}
