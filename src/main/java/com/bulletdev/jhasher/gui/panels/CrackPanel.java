package com.bulletdev.jhasher.gui.panels;

import com.bulletdev.jhasher.core.crack.CrackListener;
import com.bulletdev.jhasher.core.crack.CrackResult;
import com.bulletdev.jhasher.core.crack.DictionaryCracker;
import com.bulletdev.jhasher.core.crack.WordlistSource;
import com.bulletdev.jhasher.core.identify.HashAlgorithm;
import com.bulletdev.jhasher.core.identify.HashRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Optional;

/**
 * GUI panel for the Quick Crack feature.
 */
public class CrackPanel extends JPanel {

    private final JTextField hashField;
    private final JComboBox<String> algoCombo;
    private final JTextField wordlistField;
    private final JCheckBox builtinCheckbox;
    private final JButton browseButton;
    private final JButton crackButton;
    private final JButton stopButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JTextArea outputArea;
    private final JLabel suggestionLabel;

    private SwingWorker<CrackResult, Void> currentWorker;

    public CrackPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- NORTH: Input panel ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Hash input
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Hash:"), gbc);

        hashField = new JTextField(50);
        hashField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        inputPanel.add(hashField, gbc);

        // Algorithm selector
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Algorithm:"), gbc);

        algoCombo = new JComboBox<>();
        algoCombo.addItem("auto-detect");
        for (HashAlgorithm algo : HashRegistry.getInstance().getAll()) {
            algoCombo.addItem(algo.id());
        }
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        inputPanel.add(algoCombo, gbc);

        // Wordlist selector
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(new JLabel("Wordlist:"), gbc);

        wordlistField = new JTextField(30);
        wordlistField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        inputPanel.add(wordlistField, gbc);

        browseButton = new JButton("Browse");
        browseButton.addActionListener(this::onBrowse);
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        inputPanel.add(browseButton, gbc);

        builtinCheckbox = new JCheckBox("Use built-in top-1000", true);
        builtinCheckbox.addActionListener(e -> {
            boolean useBuiltin = builtinCheckbox.isSelected();
            wordlistField.setEnabled(!useBuiltin);
            browseButton.setEnabled(!useBuiltin);
        });
        gbc.gridx = 3;
        gbc.gridy = 2;
        inputPanel.add(builtinCheckbox, gbc);
        // Initial state: disable file controls when builtin is selected
        wordlistField.setEnabled(false);
        browseButton.setEnabled(false);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        crackButton = new JButton("Crack");
        crackButton.addActionListener(this::onCrack);
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(this::onStop);
        buttonPanel.add(crackButton);
        buttonPanel.add(stopButton);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(buttonPanel, gbc);

        add(inputPanel, BorderLayout.NORTH);

        // --- CENTER: Progress and output ---
        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));

        JPanel progressPanel = new JPanel(new BorderLayout(4, 4));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressPanel.add(progressBar, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        progressPanel.add(statusLabel, BorderLayout.SOUTH);
        centerPanel.add(progressPanel, BorderLayout.NORTH);

        outputArea = new JTextArea(12, 60);
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // --- SOUTH: Suggestion ---
        suggestionLabel = new JLabel(" ");
        suggestionLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        suggestionLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(suggestionLabel, BorderLayout.SOUTH);
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Wordlist File");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            wordlistField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onCrack(ActionEvent e) {
        String hashInput = hashField.getText().trim();
        if (hashInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a hash to crack.",
                    "No Hash", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Resolve wordlist
        final WordlistSource wordlist;
        try {
            if (builtinCheckbox.isSelected()) {
                wordlist = WordlistSource.bundled("top1000");
            } else {
                String path = wordlistField.getText().trim();
                if (path.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please select a wordlist file or use the built-in list.",
                            "No Wordlist", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                wordlist = WordlistSource.fromFile(Path.of(path));
            }
        } catch (Exception ex) {
            outputArea.setText("Error loading wordlist: " + ex.getMessage());
            return;
        }

        // Resolve algorithm
        String selectedAlgo = (String) algoCombo.getSelectedItem();
        final String algoId = "auto-detect".equals(selectedAlgo) ? null : selectedAlgo;

        // Prepare UI
        crackButton.setEnabled(false);
        stopButton.setEnabled(true);
        outputArea.setText("");
        suggestionLabel.setText(" ");
        progressBar.setValue(0);
        progressBar.setString("Cracking...");
        statusLabel.setText("Starting...");

        long totalEstimate = wordlist.estimateSize();

        currentWorker = new SwingWorker<>() {
            @Override
            protected CrackResult doInBackground() {
                DictionaryCracker cracker = new DictionaryCracker();

                CrackListener listener = new CrackListener() {
                    @Override
                    public void onProgress(long attempts, long total, double speed) {
                        SwingUtilities.invokeLater(() -> {
                            long est = total > 0 ? total : totalEstimate;
                            if (est > 0) {
                                int percent = (int) ((attempts * 100) / est);
                                progressBar.setValue(percent);
                                progressBar.setString(percent + "%");
                            }
                            statusLabel.setText(String.format("Trying... %,d/%s | %s",
                                    attempts,
                                    est > 0 ? String.format("%,d", est) : "?",
                                    formatSpeed(speed)));
                        });
                    }

                    @Override
                    public void onFound(String plaintext) {
                        // Handled in onComplete
                    }

                    @Override
                    public void onComplete(CrackResult result) {
                        // Handled in done()
                    }
                };

                return cracker.crackParallel(hashInput, algoId, wordlist, listener, 4);
            }

            @Override
            protected void done() {
                crackButton.setEnabled(true);
                stopButton.setEnabled(false);

                try {
                    CrackResult result = get();
                    progressBar.setValue(100);

                    if (result.found()) {
                        progressBar.setString("FOUND!");
                        statusLabel.setText(String.format("Cracked in %,d attempts (%.1fs) | %s",
                                result.attempts(),
                                result.durationMs() / 1000.0,
                                formatSpeed(result.speed())));
                        outputArea.setText("FOUND: " + result.plaintext()
                                + "\nAlgorithm: " + result.algorithm()
                                + "\nAttempts: " + String.format("%,d", result.attempts())
                                + "\nTime: " + formatDurationMs(result.durationMs()));
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        progressBar.setString("Not found");
                        statusLabel.setText(String.format("Exhausted %,d words in %s",
                                result.attempts(), formatDurationMs(result.durationMs())));
                        outputArea.setText("NOT FOUND in wordlist.\nTried "
                                + String.format("%,d", result.attempts()) + " words.");
                        showHashcatSuggestion(result.algorithm());
                    }
                } catch (java.util.concurrent.CancellationException ex) {
                    progressBar.setString("Stopped");
                    statusLabel.setText("Stopped by user");
                    outputArea.append("\nStopped.");
                } catch (Exception ex) {
                    progressBar.setString("Error");
                    outputArea.setText("Error: " + ex.getMessage());
                }
            }
        };

        currentWorker.execute();
    }

    private void onStop(ActionEvent e) {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            stopButton.setEnabled(false);
        }
    }

    private void showHashcatSuggestion(String algorithmId) {
        if (algorithmId == null) {
            suggestionLabel.setText("Suggestion: Try hashcat or john with a larger wordlist and rules.");
            return;
        }

        Optional<Integer> hashcatMode = HashRegistry.getInstance().getById(algorithmId)
                .flatMap(HashAlgorithm::hashcatMode);

        if (hashcatMode.isPresent()) {
            suggestionLabel.setText("Suggestion: hashcat -m " + hashcatMode.get()
                    + " hash.txt wordlist.txt -r best64.rule");
        } else {
            suggestionLabel.setText("Suggestion: Try hashcat or john with a larger wordlist and rules.");
        }
    }

    private static String formatSpeed(double hashesPerSec) {
        if (hashesPerSec >= 1_000_000) {
            return String.format("%.1fM h/s", hashesPerSec / 1_000_000);
        } else if (hashesPerSec >= 1_000) {
            return String.format("%.0fK h/s", hashesPerSec / 1_000);
        } else {
            return String.format("%.0f h/s", hashesPerSec);
        }
    }

    private static String formatDurationMs(long ms) {
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60_000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            long minutes = ms / 60_000;
            long seconds = (ms % 60_000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
}
