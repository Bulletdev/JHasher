package com.bulletdev.jhasher.gui.panels;

import com.bulletdev.jhasher.core.jwt.JwtAttacker;
import com.bulletdev.jhasher.core.jwt.JwtAttacker.AttackResult;
import com.bulletdev.jhasher.core.jwt.JwtDecoder;
import com.bulletdev.jhasher.core.jwt.JwtDecoder.JwtAnalysis;
import com.bulletdev.jhasher.core.jwt.JwtDecoder.JwtTimestamps;
import com.bulletdev.jhasher.core.jwt.JwtForger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JWT Explorer panel with Decode, Attack, and Forge tabs.
 */
public class JwtPanel extends JPanel {

    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private final JwtDecoder decoder = new JwtDecoder();
    private final JwtAttacker attacker = new JwtAttacker();
    private final JwtForger forger = new JwtForger();

    public JwtPanel() {
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Decode", createDecodeTab());
        tabs.addTab("Attack", createAttackTab());
        tabs.addTab("Forge", createForgeTab());
        add(tabs, BorderLayout.CENTER);
    }

    // ================================================================
    // Decode Tab
    // ================================================================

    private JPanel createDecodeTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // -- TOP: token input
        JPanel topBar = new JPanel(new BorderLayout(6, 0));
        topBar.add(new JLabel("JWT Token:"), BorderLayout.WEST);
        JTextField tokenField = new JTextField();
        tokenField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        topBar.add(tokenField, BorderLayout.CENTER);
        JButton decodeBtn = new JButton("Decode");
        topBar.add(decodeBtn, BorderLayout.EAST);
        panel.add(topBar, BorderLayout.NORTH);

        // -- CENTER: header / payload / signature
        RSyntaxTextArea headerArea = createJsonArea(6, false);
        RSyntaxTextArea payloadArea = createJsonArea(6, false);

        JPanel sigPanel = new JPanel();
        sigPanel.setLayout(new BoxLayout(sigPanel, BoxLayout.Y_AXIS));
        sigPanel.setBorder(BorderFactory.createTitledBorder("Signature"));
        JLabel algLabel = new JLabel("Algorithm: -");
        JLabel sigLabel = new JLabel("Signature: -");
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        sigPanel.add(algLabel);
        sigPanel.add(Box.createVerticalStrut(4));
        sigPanel.add(sigLabel);
        sigPanel.add(Box.createVerticalStrut(4));
        sigPanel.add(statusLabel);
        sigPanel.add(Box.createVerticalGlue());

        JPanel headerWrap = wrapTitled("Header", new RTextScrollPane(headerArea));
        JPanel payloadWrap = wrapTitled("Payload", new RTextScrollPane(payloadArea));

        JSplitPane leftSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, headerWrap, payloadWrap);
        leftSplit.setResizeWeight(0.5);
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, sigPanel);
        centerSplit.setResizeWeight(0.75);

        // -- BOTTOM: timestamps + warnings
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 4));

        JPanel tsPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        tsPanel.setBorder(BorderFactory.createTitledBorder("Timestamps"));
        JLabel iatLabel = new JLabel("iat: -");
        JLabel expLabel = new JLabel("exp: -");
        JLabel nbfLabel = new JLabel("nbf: -");
        tsPanel.add(iatLabel);
        tsPanel.add(expLabel);
        tsPanel.add(nbfLabel);
        bottomPanel.add(tsPanel, BorderLayout.NORTH);

        DefaultListModel<String> warningsModel = new DefaultListModel<>();
        JList<String> warningsList = new JList<>(warningsModel);
        warningsList.setCellRenderer(new WarningCellRenderer());
        JScrollPane warnScroll = new JScrollPane(warningsList);
        warnScroll.setBorder(BorderFactory.createTitledBorder("Warnings"));
        warnScroll.setPreferredSize(new Dimension(0, 80));
        bottomPanel.add(warnScroll, BorderLayout.CENTER);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplit, bottomPanel);
        mainSplit.setResizeWeight(0.65);
        panel.add(mainSplit, BorderLayout.CENTER);

        // -- action
        decodeBtn.addActionListener(e -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) return;

            try {
                JwtAnalysis analysis = decoder.decode(token);

                headerArea.setText(prettyJson(analysis.header()));
                headerArea.setCaretPosition(0);
                payloadArea.setText(prettyJson(analysis.payload()));
                payloadArea.setCaretPosition(0);

                algLabel.setText("Algorithm: " + analysis.algorithm());

                String sig = analysis.signatureBase64();
                String sigDisplay = sig.length() > 40 ? sig.substring(0, 40) + "..." : sig;
                sigLabel.setText("Signature: " + sigDisplay);
                sigLabel.setToolTipText(sig);

                JwtTimestamps ts = analysis.timestamps();
                if (ts.isExpired()) {
                    statusLabel.setText("EXPIRED");
                    statusLabel.setForeground(new Color(0xCC, 0x33, 0x33));
                } else {
                    statusLabel.setText("UNVERIFIED");
                    statusLabel.setForeground(new Color(0xCC, 0x88, 0x00));
                }

                iatLabel.setText("iat: " + formatTs(ts.issuedAt()));
                expLabel.setText("exp: " + formatTs(ts.expiresAt()));
                nbfLabel.setText("nbf: " + formatTs(ts.notBefore()));

                warningsModel.clear();
                for (String w : analysis.warnings()) {
                    warningsModel.addElement(w);
                }
            } catch (Exception ex) {
                headerArea.setText("");
                payloadArea.setText("");
                algLabel.setText("Algorithm: -");
                sigLabel.setText("Signature: -");
                statusLabel.setText("ERROR");
                statusLabel.setForeground(new Color(0xCC, 0x33, 0x33));
                warningsModel.clear();
                warningsModel.addElement("Error: " + ex.getMessage());
            }
        });

        return panel;
    }

    // ================================================================
    // Attack Tab
    // ================================================================

    private JPanel createAttackTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // -- TOP: token input
        JPanel topBar = new JPanel(new BorderLayout(6, 0));
        topBar.add(new JLabel("JWT Token:"), BorderLayout.WEST);
        JTextField tokenField = new JTextField();
        tokenField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        topBar.add(tokenField, BorderLayout.CENTER);
        JButton runBtn = new JButton("Run All Attacks");
        topBar.add(runBtn, BorderLayout.EAST);

        // -- Options row
        JPanel optPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        optPanel.setBorder(BorderFactory.createTitledBorder("Attack Options"));
        JCheckBox cbAlgNone = new JCheckBox("alg:none", true);
        JCheckBox cbNullSig = new JCheckBox("Null Signature", true);
        JCheckBox cbWeakBF = new JCheckBox("Weak Secret BF", true);
        JCheckBox cbJku = new JCheckBox("JKU Injection", true);
        JCheckBox cbKidSql = new JCheckBox("KID SQLi", true);
        optPanel.add(cbAlgNone);
        optPanel.add(cbNullSig);
        optPanel.add(cbWeakBF);
        optPanel.add(cbJku);
        optPanel.add(cbKidSql);

        JPanel northPanel = new JPanel(new BorderLayout(0, 4));
        northPanel.add(topBar, BorderLayout.NORTH);
        northPanel.add(optPanel, BorderLayout.SOUTH);
        panel.add(northPanel, BorderLayout.NORTH);

        // -- CENTER: results table
        AttackTableModel tableModel = new AttackTableModel();
        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(60);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setCellRenderer(new StatusCellRenderer());

        // double-click on forged token column to copy
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (col == 3 && row >= 0) {
                        String val = (String) tableModel.getValueAt(row, col);
                        if (val != null && !val.isEmpty()) {
                            copyToClipboard(val);
                        }
                    }
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        panel.add(tableScroll, BorderLayout.CENTER);

        // -- action
        runBtn.addActionListener(e -> {
            String token = tokenField.getText().trim();
            if (token.isEmpty()) return;

            tableModel.clear();

            SwingWorker<List<AttackResult>, Void> worker = new SwingWorker<>() {
                @Override
                protected List<AttackResult> doInBackground() {
                    List<AttackResult> results = new ArrayList<>();
                    Map<String, String> opts = new HashMap<>();

                    if (cbAlgNone.isSelected()) {
                        results.addAll(attacker.algNone(token));
                    }
                    if (cbNullSig.isSelected()) {
                        results.add(attacker.nullSignature(token));
                    }
                    if (cbWeakBF.isSelected()) {
                        results.add(attacker.runAttack("weak-secret", token, opts));
                    }
                    if (cbJku.isSelected()) {
                        results.add(attacker.jkuInjection(token,
                                "https://attacker.example.com/.well-known/jwks.json"));
                    }
                    if (cbKidSql.isSelected()) {
                        results.add(attacker.kidSqlInjection(token));
                    }
                    return results;
                }

                @Override
                protected void done() {
                    try {
                        List<AttackResult> results = get();
                        tableModel.setResults(results);
                    } catch (Exception ex) {
                        tableModel.clear();
                        tableModel.addResult(new AttackResult(
                                "Error", false, ex.getMessage(), ""));
                    }
                }
            };
            worker.execute();
        });

        return panel;
    }

    // ================================================================
    // Forge Tab
    // ================================================================

    private JPanel createForgeTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // -- Form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Algorithm
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(new JLabel("Algorithm:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JComboBox<String> algCombo = new JComboBox<>(new String[]{"HS256", "HS384", "HS512", "none"});
        form.add(algCombo, gbc);

        // Payload
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Payload:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        RSyntaxTextArea payloadArea = createJsonArea(6, true);
        payloadArea.setText("{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"iat\":1516239022}");
        try {
            payloadArea.setText(prettyJson(payloadArea.getText()));
        } catch (Exception ignored) {
            // keep original text if pretty-print fails
        }
        RTextScrollPane payloadScroll = new RTextScrollPane(payloadArea);
        form.add(payloadScroll, gbc);

        // Key
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(new JLabel("Key:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0;
        JPanel keyPanel = new JPanel(new BorderLayout(4, 0));
        JPasswordField keyField = new JPasswordField();
        keyField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JToggleButton showKeyBtn = new JToggleButton("Show");
        showKeyBtn.addActionListener(e -> {
            if (showKeyBtn.isSelected()) {
                keyField.setEchoChar((char) 0);
                showKeyBtn.setText("Hide");
            } else {
                keyField.setEchoChar('•');
                showKeyBtn.setText("Show");
            }
        });
        keyPanel.add(keyField, BorderLayout.CENTER);
        keyPanel.add(showKeyBtn, BorderLayout.EAST);
        form.add(keyPanel, gbc);

        // Forge button
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0;
        JButton forgeBtn = new JButton("Forge");
        JPanel forgeBtnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        forgeBtnWrap.add(forgeBtn);
        form.add(forgeBtnWrap, gbc);

        panel.add(form, BorderLayout.CENTER);

        // -- Output
        JPanel outputPanel = new JPanel(new BorderLayout(6, 0));
        outputPanel.setBorder(BorderFactory.createTitledBorder("Forged Token"));
        JTextField outputField = new JTextField();
        outputField.setEditable(false);
        outputField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> {
            String text = outputField.getText();
            if (!text.isEmpty()) {
                copyToClipboard(text);
            }
        });
        outputPanel.add(outputField, BorderLayout.CENTER);
        outputPanel.add(copyBtn, BorderLayout.EAST);
        panel.add(outputPanel, BorderLayout.SOUTH);

        // -- action
        forgeBtn.addActionListener(e -> {
            try {
                String alg = (String) algCombo.getSelectedItem();
                String payload = payloadArea.getText().trim();
                String key = new String(keyField.getPassword());
                String token = forger.forge(alg, payload, key);
                outputField.setText(token);
                outputField.setForeground(UIManager.getColor("TextField.foreground"));
            } catch (Exception ex) {
                outputField.setText("Error: " + ex.getMessage());
                outputField.setForeground(new Color(0xCC, 0x33, 0x33));
            }
        });

        return panel;
    }

    // ================================================================
    // Helpers
    // ================================================================

    private static RSyntaxTextArea createJsonArea(int rows, boolean editable) {
        RSyntaxTextArea area = new RSyntaxTextArea(rows, 40);
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        area.setCodeFoldingEnabled(true);
        area.setEditable(editable);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setBackground(new Color(0x2b, 0x2b, 0x2b));
        area.setForeground(new Color(0xa9, 0xb7, 0xc6));
        area.setCurrentLineHighlightColor(new Color(0x32, 0x32, 0x32));
        area.setCaretColor(new Color(0xa9, 0xb7, 0xc6));
        return area;
    }

    private static String prettyJson(Map<String, Object> map) {
        return GSON_PRETTY.toJson(map);
    }

    private static String prettyJson(String rawJson) {
        return GSON_PRETTY.toJson(JsonParser.parseString(rawJson));
    }

    private static String formatTs(Instant instant) {
        if (instant == null) return "-";
        return DATE_FMT.format(instant);
    }

    private static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    private static JPanel wrapTitled(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    // ================================================================
    // Table model for Attack results
    // ================================================================

    private static class AttackTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {"Attack", "Status", "Description", "Forged Token"};
        private final List<AttackResult> results = new ArrayList<>();

        void setResults(List<AttackResult> data) {
            results.clear();
            results.addAll(data);
            fireTableDataChanged();
        }

        void addResult(AttackResult r) {
            results.add(r);
            fireTableRowsInserted(results.size() - 1, results.size() - 1);
        }

        void clear() {
            results.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() { return results.size(); }

        @Override
        public int getColumnCount() { return COLUMNS.length; }

        @Override
        public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            AttackResult r = results.get(row);
            return switch (col) {
                case 0 -> r.attackName();
                case 1 -> r.success() ? "OK" : "FAIL";
                case 2 -> r.description();
                case 3 -> {
                    String t = r.forgedToken();
                    yield (t != null && t.length() > 60) ? t.substring(0, 60) + "..." : t;
                }
                default -> "";
            };
        }
    }

    // ================================================================
    // Cell renderers
    // ================================================================

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if ("OK".equals(value)) {
                c.setForeground(new Color(0x33, 0x99, 0x33));
            } else {
                c.setForeground(new Color(0xCC, 0x33, 0x33));
            }
            ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
            setFont(c.getFont().deriveFont(Font.BOLD));
            return c;
        }
    }

    private static class WarningCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text = String.valueOf(value);
            if (text.toLowerCase().contains("expired") || text.toLowerCase().contains("empty signature")) {
                c.setForeground(new Color(0xCC, 0x33, 0x33));
            } else {
                c.setForeground(new Color(0xCC, 0x99, 0x00));
            }
            return c;
        }
    }
}
