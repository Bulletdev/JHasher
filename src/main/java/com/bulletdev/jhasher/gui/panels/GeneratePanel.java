package com.bulletdev.jhasher.gui.panels;

import com.bulletdev.jhasher.core.generate.HashGenerator;
import com.bulletdev.jhasher.core.identify.HashAlgorithm;
import com.bulletdev.jhasher.core.identify.HashRegistry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

public final class GeneratePanel extends JPanel {

    private static final Font MONO_FONT = resolveMonoFont(14f);

    private final JTextField inputField;
    private final JComboBox<String> algoCombo;
    private final JTextArea outputArea;
    private final HashGenerator generator;

    public GeneratePanel() {
        super(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        this.generator = new HashGenerator();

        // --- North: input controls ---
        JPanel inputArea = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Row 0: Input
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        inputArea.add(new JLabel("Input:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        inputField = new JTextField();
        inputField.setFont(MONO_FONT);
        inputField.addActionListener(e -> doGenerate());
        inputArea.add(inputField, gbc);

        // Row 1: Algorithm
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        inputArea.add(new JLabel("Algorithm:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.gridwidth = 2;
        algoCombo = new JComboBox<>(loadAlgorithmIds());
        inputArea.add(algoCombo, gbc);

        // Row 2: Buttons
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;

        JPanel buttonBar = new JPanel();
        JButton genBtn = new JButton("Generate");
        genBtn.addActionListener(e -> doGenerate());
        JButton genAllBtn = new JButton("Generate All");
        genAllBtn.addActionListener(e -> doGenerateAll());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> doClear());
        buttonBar.add(genBtn);
        buttonBar.add(genAllBtn);
        buttonBar.add(clearBtn);
        inputArea.add(buttonBar, gbc);

        add(inputArea, BorderLayout.NORTH);

        // --- Center: output area ---
        outputArea = new JTextArea();
        outputArea.setFont(MONO_FONT);
        outputArea.setEditable(false);
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        outputArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyToClipboard();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(outputArea);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
    }

    private void doGenerate() {
        String input = inputField.getText();
        if (input.isEmpty()) {
            return;
        }
        String algoId = (String) algoCombo.getSelectedItem();
        if (algoId == null) {
            return;
        }

        try {
            String hash = generator.generate(algoId, input, Map.of());
            outputArea.setText(hash);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Generation failed:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doGenerateAll() {
        String input = inputField.getText();
        if (input.isEmpty()) {
            return;
        }

        try {
            List<Map.Entry<String, String>> results = generator.generateAll(input);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : results) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
            }
            outputArea.setText(sb.toString());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Generation failed:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doClear() {
        inputField.setText("");
        outputArea.setText("");
    }

    private void copyToClipboard() {
        String text = outputArea.getSelectedText();
        if (text == null || text.isEmpty()) {
            text = outputArea.getText();
        }
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        }
    }

    private static String[] loadAlgorithmIds() {
        List<HashAlgorithm> all = HashRegistry.getInstance().getAll();
        return all.stream()
                .filter(a -> a.generate("test", Map.of()).isPresent())
                .map(HashAlgorithm::id)
                .toArray(String[]::new);
    }

    private static Font resolveMonoFont(float size) {
        String[] candidates = {"JetBrains Mono", "Consolas", Font.MONOSPACED};
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, (int) size);
            if (f.getFamily().equalsIgnoreCase(name) || name.equals(Font.MONOSPACED)) {
                return f.deriveFont(size);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, (int) size);
    }
}
