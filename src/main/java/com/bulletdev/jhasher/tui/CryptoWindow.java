package com.bulletdev.jhasher.tui;

import com.bulletdev.jhasher.core.crypto.SymmetricCrypto;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.RadioBoxList;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder;
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton;

import java.util.List;

/**
 * Encrypt/decrypt screen.
 * Supports AES (GCM, CBC, CTR) and ChaCha20-Poly1305 via the SymmetricCrypto API.
 */
public class CryptoWindow extends BasicWindow {

    private final TextBox algorithmBox;
    private final TextBox keyBox;
    private final TextBox inputBox;
    private final RadioBoxList<String> modeSelector;
    private final TextBox outputBox;

    public CryptoWindow() {
        super("Encrypt / Decrypt");
        setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Algorithm:"));
        algorithmBox = new TextBox(new TerminalSize(40, 1));
        algorithmBox.setText("aes-256-gcm");
        panel.addComponent(algorithmBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Key/Password:"));
        keyBox = new TextBox(new TerminalSize(64, 1));
        panel.addComponent(keyBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Input:"));
        inputBox = new TextBox(new TerminalSize(64, 6));
        panel.addComponent(inputBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Mode:"));
        modeSelector = new RadioBoxList<>(new TerminalSize(20, 2));
        modeSelector.addItem("Encrypt");
        modeSelector.addItem("Decrypt");
        modeSelector.setCheckedItemIndex(0);
        panel.addComponent(modeSelector);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Button("Execute", this::onExecute));

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(64, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Output:"));
        outputBox = new TextBox(new TerminalSize(64, 8));
        outputBox.setReadOnly(true);
        panel.addComponent(outputBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 1)));
        panel.addComponent(new Button("Back", this::close));

        setComponent(panel);
    }

    private void onExecute() {
        String algorithm = algorithmBox.getText().trim();
        String key = keyBox.getText();
        String input = inputBox.getText();

        if (algorithm.isEmpty()) {
            showError("Please enter an algorithm.");
            return;
        }

        if (key.isEmpty()) {
            showError("Please enter a key/password.");
            return;
        }

        if (input.isEmpty()) {
            showError("Please enter input data.");
            return;
        }

        String selectedMode = modeSelector.getCheckedItem();
        boolean encrypt = "Encrypt".equals(selectedMode);

        try {
            String result;
            if (encrypt) {
                result = SymmetricCrypto.encryptToBase64(algorithm, input, key);
            } else {
                result = SymmetricCrypto.decryptFromBase64(algorithm, input.trim(), key);
            }
            outputBox.setText(result);
        } catch (Exception e) {
            showError((encrypt ? "Encryption" : "Decryption") + " failed: " + e.getMessage());
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
