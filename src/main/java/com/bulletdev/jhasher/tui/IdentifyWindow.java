package com.bulletdev.jhasher.tui;

import com.bulletdev.jhasher.core.identify.ContextDatabase;
import com.bulletdev.jhasher.core.identify.HashContext;
import com.bulletdev.jhasher.core.identify.HashIdentifier;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;
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
import java.util.Optional;

/**
 * Hash identification screen.
 * Allows the user to input a hash and identify its possible algorithm.
 */
public class IdentifyWindow extends BasicWindow {

    private final HashIdentifier identifier;
    private final ContextDatabase contextDb;
    private final TextBox hashInput;
    private final Panel resultsPanel;

    public IdentifyWindow() {
        super("Identify Hash");
        setHints(List.of(Window.Hint.CENTERED));

        this.identifier = new HashIdentifier();
        this.contextDb = ContextDatabase.getInstance();

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Enter hash:"));
        hashInput = new TextBox(new TerminalSize(64, 1));
        panel.addComponent(hashInput);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Button("Identify", this::onIdentify));

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(64, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Results:"));
        resultsPanel = new Panel(new LinearLayout(Direction.VERTICAL));
        panel.addComponent(resultsPanel);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 1)));
        panel.addComponent(new Button("Back", this::close));

        setComponent(panel);
    }

    private void onIdentify() {
        resultsPanel.removeAllComponents();

        String hash = hashInput.getText().trim();
        if (hash.isEmpty()) {
            showError("Please enter a hash value.");
            return;
        }

        try {
            List<HashMatch> matches = identifier.identify(hash);

            if (matches.isEmpty()) {
                resultsPanel.addComponent(new Label("No matching algorithms found."));
                return;
            }

            for (HashMatch match : matches) {
                String algoId = match.algorithm().id();
                String algoName = match.algorithm().name();
                double confidence = match.confidence();
                String confidenceStr = String.format("%.0f%%", confidence * 100);

                String hashcatStr = match.algorithm().hashcatMode()
                        .map(m -> "  hashcat: " + m)
                        .orElse("");

                String johnStr = match.algorithm().johnFormat()
                        .map(f -> "  john: " + f)
                        .orElse("");

                resultsPanel.addComponent(new Label(
                        String.format("  %-20s  confidence: %s%s%s",
                                algoName, confidenceStr, hashcatStr, johnStr)));

                // Show context info if available
                Optional<HashContext> ctx = contextDb.getContext(algoId);
                ctx.ifPresent(c -> {
                    if (c.commonSources() != null && !c.commonSources().isEmpty()) {
                        resultsPanel.addComponent(new Label(
                                "    Sources: " + String.join(", ", c.commonSources())));
                    }
                    if (c.strengthRating() != null) {
                        resultsPanel.addComponent(new Label(
                                "    Strength: " + c.strengthRating()
                                        + (c.strengthNote() != null ? " - " + c.strengthNote() : "")));
                    }
                });
            }
        } catch (Exception e) {
            showError("Identification failed: " + e.getMessage());
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
