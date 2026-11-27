package com.bulletdev.jhasher.tui;

import com.bulletdev.jhasher.core.generate.HashGenerator;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import java.util.List;
import java.util.Map;

/**
 * Hash generation screen.
 * Allows the user to generate hashes using a specified algorithm or all algorithms.
 */
public class GenerateWindow extends BasicWindow {

    private final HashGenerator generator;
    private final TextBox inputBox;
    private final TextBox algorithmBox;
    private final TextBox outputBox;

    public GenerateWindow() {
        super("Generate Hash");
        setHints(List.of(Window.Hint.CENTERED));

        this.generator = new HashGenerator();

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Input text:"));
        inputBox = new TextBox(new TerminalSize(64, 1));
        panel.addComponent(inputBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Algorithm (or 'all'):"));
        algorithmBox = new TextBox(new TerminalSize(30, 1));
        algorithmBox.setText("sha256");
        panel.addComponent(algorithmBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Button("Generate", this::onGenerate));

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(64, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Output:"));
        outputBox = new TextBox(new TerminalSize(64, 12));
        outputBox.setReadOnly(true);
        panel.addComponent(outputBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 1)));
        panel.addComponent(new Button("Back", this::close));

        setComponent(panel);
    }

    private void onGenerate() {
        String input = inputBox.getText();
        String algo = algorithmBox.getText().trim();

        if (input.isEmpty()) {
            showError("Please enter input text.");
            return;
        }

        if (algo.isEmpty()) {
            showError("Please enter an algorithm name or 'all'.");
            return;
        }

        try {
            if ("all".equalsIgnoreCase(algo)) {
                List<Map.Entry<String, String>> results = generator.generateAll(input);
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> entry : results) {
                    sb.append(String.format("%-20s %s%n", entry.getKey() + ":", entry.getValue()));
                }
                outputBox.setText(sb.toString());
            } else {
                String result = generator.generate(algo, input, Map.of());
                outputBox.setText(result);
            }
        } catch (Exception e) {
            showError("Generation failed: " + e.getMessage());
        }
    }

    private void showError(String message) {
        if (getTextGUI() != null) {
            new MessageDialogBuilder()
                    .setTitle("Error")
                    .setText(message)
                    .addButton(MessageDialogButton.OK)
                    .build()
                    .showDialog(getTextGUI());
        }
    }
}
