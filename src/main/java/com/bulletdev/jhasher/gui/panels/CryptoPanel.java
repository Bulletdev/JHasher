package com.bulletdev.jhasher.gui.panels;

import com.bulletdev.jhasher.core.crypto.CryptoKeyGenerator;
import com.bulletdev.jhasher.core.crypto.SymmetricCrypto;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * Symmetric encryption/decryption workbench panel.
 */
public class CryptoPanel extends JPanel {

    private static final String[] ALGORITHMS = {
            "aes-256-gcm", "aes-128-gcm", "aes-256-cbc",
            "aes-128-cbc", "aes-256-ctr", "chacha20-poly1305"
    };

    private static final java.util.Map<String, Integer> ALGO_KEY_BITS = java.util.Map.of(
            "aes-256-gcm", 256,
            "aes-128-gcm", 128,
            "aes-256-cbc", 256,
            "aes-128-cbc", 128,
            "aes-256-ctr", 256,
            "chacha20-poly1305", 256
    );

    private final JComboBox<String> algoCombo;
    private final JTextField keyField;
    private final RSyntaxTextArea inputArea;
    private final RSyntaxTextArea outputArea;
    private final JToggleButton encryptToggle;
    private final JToggleButton decryptToggle;

    public CryptoPanel() {
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // ============================================================
        // NORTH - config bar
        // ============================================================
        JPanel configBar = new JPanel();
        configBar.setLayout(new BoxLayout(configBar, BoxLayout.X_AXIS));
        configBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        configBar.add(new JLabel("Algorithm: "));
        algoCombo = new JComboBox<>(ALGORITHMS);
        algoCombo.setMaximumSize(new Dimension(200, 28));
        configBar.add(algoCombo);
        configBar.add(Box.createHorizontalStrut(12));

        configBar.add(new JLabel("Key: "));
        keyField = new JTextField(32);
        keyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        keyField.setMaximumSize(new Dimension(400, 28));
        configBar.add(keyField);
        configBar.add(Box.createHorizontalStrut(6));

        JButton genKeyBtn = new JButton("Generate Key");
        genKeyBtn.addActionListener(e -> generateKey());
        configBar.add(genKeyBtn);
        configBar.add(Box.createHorizontalStrut(16));

        // mode toggle
        ButtonGroup modeGroup = new ButtonGroup();
        encryptToggle = new JToggleButton("Encrypt", true);
        decryptToggle = new JToggleButton("Decrypt");
        modeGroup.add(encryptToggle);
        modeGroup.add(decryptToggle);
        configBar.add(encryptToggle);
        configBar.add(Box.createHorizontalStrut(4));
        configBar.add(decryptToggle);

        add(configBar, BorderLayout.NORTH);

        // ============================================================
        // CENTER - input / output text areas
        // ============================================================
        inputArea = createTextArea(true);
        outputArea = createTextArea(false);

        JPanel inputPanel = wrapTitled("Input", new RTextScrollPane(inputArea));
        JPanel outputPanel = wrapTitled("Output", new RTextScrollPane(outputArea));

        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, outputPanel);
        centerSplit.setResizeWeight(0.5);
        add(centerSplit, BorderLayout.CENTER);

        // ============================================================
        // SOUTH - action bar
        // ============================================================
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));

        JButton executeBtn = new JButton("Execute");
        executeBtn.addActionListener(e -> execute());
        actionBar.add(executeBtn);

        JButton swapBtn = new JButton("Swap");
        swapBtn.setToolTipText("Move output to input and clear output");
        swapBtn.addActionListener(e -> swap());
        actionBar.add(swapBtn);

        JButton copyBtn = new JButton("Copy Output");
        copyBtn.addActionListener(e -> copyOutput());
        actionBar.add(copyBtn);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> clearAll());
        actionBar.add(clearBtn);

        add(actionBar, BorderLayout.SOUTH);
    }

    // ================================================================
    // Actions
    // ================================================================

    private void execute() {
        String algo = (String) algoCombo.getSelectedItem();
        String key = keyField.getText().trim();
        String input = inputArea.getText().trim();

        if (input.isEmpty()) {
            showError("Input is empty");
            return;
        }
        if (key.isEmpty()) {
            showError("Key is empty - generate or enter a key first");
            return;
        }

        try {
            String result;
            if (encryptToggle.isSelected()) {
                result = SymmetricCrypto.encryptToBase64(algo, input, key);
            } else {
                result = SymmetricCrypto.decryptFromBase64(algo, input, key);
            }
            outputArea.setForeground(new Color(0xa9, 0xb7, 0xc6));
            outputArea.setText(result);
            outputArea.setCaretPosition(0);
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    private void generateKey() {
        String algo = (String) algoCombo.getSelectedItem();
        int bits = ALGO_KEY_BITS.getOrDefault(algo, 256);
        String hex = CryptoKeyGenerator.generateKeyHex(bits);
        keyField.setText(hex);
    }

    private void swap() {
        String out = outputArea.getText();
        inputArea.setText(out);
        inputArea.setCaretPosition(0);
        outputArea.setText("");
        outputArea.setForeground(new Color(0xa9, 0xb7, 0xc6));
    }

    private void copyOutput() {
        String text = outputArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        }
    }

    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        outputArea.setForeground(new Color(0xa9, 0xb7, 0xc6));
        keyField.setText("");
    }

    private void showError(String message) {
        outputArea.setForeground(new Color(0xCC, 0x33, 0x33));
        outputArea.setText("Error: " + message);
        outputArea.setCaretPosition(0);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static RSyntaxTextArea createTextArea(boolean editable) {
        RSyntaxTextArea area = new RSyntaxTextArea(12, 40);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        area.setEditable(editable);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(0x2b, 0x2b, 0x2b));
        area.setForeground(new Color(0xa9, 0xb7, 0xc6));
        area.setCurrentLineHighlightColor(new Color(0x32, 0x32, 0x32));
        area.setCaretColor(new Color(0xa9, 0xb7, 0xc6));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static JPanel wrapTitled(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }
}
