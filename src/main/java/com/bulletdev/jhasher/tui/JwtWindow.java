package com.bulletdev.jhasher.tui;

import com.bulletdev.jhasher.core.jwt.JwtDecoder;
import com.bulletdev.jhasher.core.jwt.JwtDecoder.JwtAnalysis;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
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

/**
 * JWT decode screen.
 * Decodes a JWT token and displays its header, payload, timestamps, and warnings.
 */
public class JwtWindow extends BasicWindow {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final JwtDecoder decoder;
    private final TextBox tokenBox;
    private final TextBox outputBox;

    public JwtWindow() {
        super("JWT Decode");
        setHints(List.of(Window.Hint.CENTERED));

        this.decoder = new JwtDecoder();

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("JWT Token:"));
        tokenBox = new TextBox(new TerminalSize(72, 1));
        panel.addComponent(tokenBox);

        panel.addComponent(new EmptySpace(new TerminalSize(72, 0)));

        panel.addComponent(new Button("Decode", this::onDecode));

        panel.addComponent(new EmptySpace(new TerminalSize(72, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(72, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(72, 0)));

        panel.addComponent(new Label("Analysis:"));
        outputBox = new TextBox(new TerminalSize(72, 20));
        outputBox.setReadOnly(true);
        panel.addComponent(outputBox);

        panel.addComponent(new EmptySpace(new TerminalSize(72, 1)));
        panel.addComponent(new Button("Back", this::close));

        setComponent(panel);
    }

    private void onDecode() {
        String token = tokenBox.getText().trim();

        if (token.isEmpty()) {
            showError("Please enter a JWT token.");
            return;
        }

        try {
            JwtAnalysis analysis = decoder.decode(token);

            StringBuilder sb = new StringBuilder();

            sb.append("Algorithm: ").append(analysis.algorithm()).append("\n\n");

            sb.append("--- Header ---\n");
            sb.append(GSON.toJson(analysis.header())).append("\n\n");

            sb.append("--- Payload ---\n");
            sb.append(GSON.toJson(analysis.payload())).append("\n\n");

            sb.append("--- Timestamps ---\n");
            if (analysis.timestamps().issuedAt() != null) {
                sb.append("  Issued At:  ").append(analysis.timestamps().issuedAt()).append("\n");
            }
            if (analysis.timestamps().expiresAt() != null) {
                sb.append("  Expires At: ").append(analysis.timestamps().expiresAt()).append("\n");
            }
            if (analysis.timestamps().notBefore() != null) {
                sb.append("  Not Before: ").append(analysis.timestamps().notBefore()).append("\n");
            }
            sb.append("  Expired:    ").append(analysis.timestamps().isExpired() ? "YES" : "No").append("\n");

            if (!analysis.warnings().isEmpty()) {
                sb.append("\n--- Warnings ---\n");
                for (String warning : analysis.warnings()) {
                    sb.append("  [!] ").append(warning).append("\n");
                }
            }

            outputBox.setText(sb.toString());
        } catch (Exception e) {
            showError("JWT decode failed: " + e.getMessage());
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
