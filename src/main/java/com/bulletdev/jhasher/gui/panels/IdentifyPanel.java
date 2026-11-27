package com.bulletdev.jhasher.gui.panels;

import com.bulletdev.jhasher.core.identify.ContextDatabase;
import com.bulletdev.jhasher.core.identify.HashContext;
import com.bulletdev.jhasher.core.identify.HashIdentifier;
import com.bulletdev.jhasher.core.identify.HashIdentifier.HashMatch;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Optional;

public final class IdentifyPanel extends JPanel {

    private static final Font MONO_FONT = resolveMonoFont(16f);
    private static final Font MONO_SMALL = resolveMonoFont(13f);
    private static final Font BOLD_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);

    private final JTextField inputField;
    private final JPanel resultsContainer;
    private final HashIdentifier identifier;
    private final ContextDatabase contextDb;

    public IdentifyPanel() {
        super(new BorderLayout(0, 8));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        this.identifier = new HashIdentifier();
        this.contextDb = ContextDatabase.getInstance();

        // --- North: input area ---
        JPanel inputArea = new JPanel(new BorderLayout(6, 6));
        inputArea.setBorder(new EmptyBorder(0, 0, 4, 0));

        JLabel label = new JLabel("Hash Input:");
        inputArea.add(label, BorderLayout.NORTH);

        inputField = new JTextField();
        inputField.setFont(MONO_FONT);
        inputField.addActionListener(e -> doIdentify());
        inputArea.add(inputField, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton identifyBtn = new JButton("Identify");
        identifyBtn.addActionListener(e -> doIdentify());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> doClear());
        buttonBar.add(identifyBtn);
        buttonBar.add(clearBtn);
        inputArea.add(buttonBar, BorderLayout.SOUTH);

        add(inputArea, BorderLayout.NORTH);

        // --- Center: results area ---
        resultsContainer = new JPanel();
        resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(resultsContainer);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
    }

    private void doIdentify() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        resultsContainer.removeAll();

        try {
            List<HashMatch> matches = identifier.identify(text);

            if (matches.isEmpty()) {
                JLabel noMatch = new JLabel("No matches found");
                noMatch.setFont(noMatch.getFont().deriveFont(Font.ITALIC, 14f));
                noMatch.setBorder(new EmptyBorder(12, 4, 0, 0));
                resultsContainer.add(noMatch);
            } else {
                for (HashMatch match : matches) {
                    Optional<HashContext> ctx = contextDb.getContext(match.algorithm().id());
                    resultsContainer.add(buildResultCard(match, ctx));
                    resultsContainer.add(Box.createRigidArea(new Dimension(0, 6)));
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error during identification:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        resultsContainer.revalidate();
        resultsContainer.repaint();
    }

    private void doClear() {
        inputField.setText("");
        resultsContainer.removeAll();
        resultsContainer.revalidate();
        resultsContainer.repaint();
    }

    private JPanel buildResultCard(HashMatch match, Optional<HashContext> ctxOpt) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                new EmptyBorder(6, 8, 6, 8)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Row 1: algorithm name + confidence
        double pct = match.confidence() * 100;
        Color confColor = pct >= 90 ? new Color(80, 200, 80)
                : pct >= 70 ? new Color(220, 200, 60)
                : new Color(220, 80, 80);

        JLabel titleLabel = new JLabel(
                match.algorithm().name() + "  -  " + String.format("%.0f%%", pct)
        );
        titleLabel.setFont(BOLD_FONT);
        titleLabel.setForeground(confColor);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        card.add(titleLabel);

        // Row 2: hashcat mode / john format
        StringBuilder toolLine = new StringBuilder();
        match.algorithm().hashcatMode().ifPresent(m -> toolLine.append("Hashcat: ").append(m).append("    "));
        match.algorithm().johnFormat().ifPresent(j -> toolLine.append("John: ").append(j));
        if (!toolLine.isEmpty()) {
            JLabel toolLabel = new JLabel(toolLine.toString());
            toolLabel.setFont(MONO_SMALL);
            toolLabel.setAlignmentX(LEFT_ALIGNMENT);
            card.add(toolLabel);
        }

        // Row 3+: context info
        ctxOpt.ifPresent(ctx -> {
            if (ctx.commonSources() != null && !ctx.commonSources().isEmpty()) {
                JLabel sourcesLabel = new JLabel("Sources: " + String.join(", ", ctx.commonSources()));
                sourcesLabel.setAlignmentX(LEFT_ALIGNMENT);
                card.add(sourcesLabel);
            }

            if (ctx.strengthRating() != null) {
                Color strengthColor = switch (ctx.strengthRating().toLowerCase()) {
                    case "strong", "very strong" -> new Color(80, 200, 80);
                    case "moderate" -> new Color(220, 200, 60);
                    default -> new Color(220, 80, 80);
                };
                JLabel strengthLabel = new JLabel("Strength: " + ctx.strengthRating()
                        + (ctx.strengthNote() != null ? " - " + ctx.strengthNote() : ""));
                strengthLabel.setForeground(strengthColor);
                strengthLabel.setAlignmentX(LEFT_ALIGNMENT);
                card.add(strengthLabel);
            }

            if (ctx.attackNotes() != null && !ctx.attackNotes().isEmpty()) {
                JLabel attackLabel = new JLabel("Attack notes: " + String.join("; ", ctx.attackNotes()));
                attackLabel.setAlignmentX(LEFT_ALIGNMENT);
                card.add(attackLabel);
            }
        });

        return card;
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
