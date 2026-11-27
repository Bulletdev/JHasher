package com.bulletdev.jhasher.gui.panels;

import com.bulletdev.jhasher.core.transform.Transform;
import com.bulletdev.jhasher.core.transform.TransformChain;
import com.bulletdev.jhasher.core.transform.TransformRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CyberChef-style visual transform builder panel.
 * Three-column layout: available transforms (left), recipe + input/output (center),
 * step inspector (right).
 */
public class TransformPanel extends JPanel {

    private final TransformRegistry registry = TransformRegistry.getInstance();
    private final List<RecipeStep> recipeSteps = new ArrayList<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Left column - transform catalog
    private JTextField searchField;
    private JTree transformTree;
    private DefaultTreeModel treeModel;

    // Center column - input, recipe, output
    private RSyntaxTextArea inputArea;
    private RSyntaxTextArea outputArea;
    private JPanel recipeListPanel;
    private JLabel outputInfoLabel;

    // Right column - step inspector
    private JCheckBox showStepsCheckBox;
    private JList<String> stepList;
    private DefaultListModel<String> stepListModel;
    private RSyntaxTextArea stepPreviewArea;
    private JSplitPane rightSplit;

    // Execution state
    private javax.swing.Timer debounceTimer;
    private List<TransformChain.StepResult> lastStepResults;

    // Param definitions: transform id -> list of param names
    private static final Map<String, List<String>> PARAM_DEFS = Map.of(
            "hmac-sha256", List.of("key"),
            "aes-gcm-encrypt", List.of("key"),
            "aes-gcm-decrypt", List.of("key"),
            "xor", List.of("key"),
            "rotn", List.of("n"),
            "repeat", List.of("n"),
            "replace", List.of("from", "to")
    );

    public TransformPanel() {
        setLayout(new BorderLayout());

        // Debounce timer: 150ms delay, fires executeRecipe
        debounceTimer = new javax.swing.Timer(150, e -> executeRecipe());
        debounceTimer.setRepeats(false);

        // Build columns
        JPanel leftPanel = buildLeftPanel();
        JPanel centerPanel = buildCenterPanel();
        JPanel rightPanel = buildRightPanel();

        // Split panes: left | center | right
        JSplitPane innerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerPanel, rightPanel);
        innerSplit.setResizeWeight(1.0);
        innerSplit.setDividerLocation(600);
        innerSplit.setOneTouchExpandable(true);

        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, innerSplit);
        outerSplit.setResizeWeight(0.0);
        outerSplit.setDividerLocation(200);
        outerSplit.setOneTouchExpandable(true);

        add(outerSplit, BorderLayout.CENTER);
    }

    // -----------------------------------------------------------------------
    // LEFT PANEL: Available Transforms
    // -----------------------------------------------------------------------

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.setPreferredSize(new Dimension(200, 0));

        JLabel header = new JLabel("Transforms");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        panel.add(header, BorderLayout.NORTH);

        // Search field
        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Filter...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterTree(); }
            @Override public void removeUpdate(DocumentEvent e) { filterTree(); }
            @Override public void changedUpdate(DocumentEvent e) { filterTree(); }
        });

        // Transform tree
        treeModel = buildTreeModel("");
        transformTree = new JTree(treeModel);
        transformTree.setRootVisible(false);
        transformTree.setShowsRootHandles(true);
        transformTree.setCellRenderer(new TransformTreeRenderer());

        // Double-click adds transform to recipe
        transformTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSelectedTransform();
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(transformTree);

        // Add button
        JButton addBtn = new JButton("Add to Recipe");
        addBtn.addActionListener(e -> addSelectedTransform());

        JPanel topPanel = new JPanel(new BorderLayout(0, 4));
        topPanel.add(searchField, BorderLayout.NORTH);
        topPanel.add(treeScroll, BorderLayout.CENTER);
        topPanel.add(addBtn, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.CENTER);

        // Expand all nodes initially
        expandAllNodes(transformTree);

        return panel;
    }

    private DefaultTreeModel buildTreeModel(String filter) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        String lowerFilter = filter.toLowerCase();

        // Gather categories in order
        Set<String> categoryOrder = new LinkedHashSet<>();
        for (Transform t : registry.getAll()) {
            categoryOrder.add(t.category());
        }

        for (String category : categoryOrder) {
            DefaultMutableTreeNode catNode = new DefaultMutableTreeNode(capitalize(category));
            boolean hasChildren = false;

            for (Transform t : registry.getByCategory(category)) {
                if (lowerFilter.isEmpty()
                        || t.name().toLowerCase().contains(lowerFilter)
                        || t.id().toLowerCase().contains(lowerFilter)) {
                    catNode.add(new DefaultMutableTreeNode(new TransformNode(t)));
                    hasChildren = true;
                }
            }

            if (hasChildren) {
                root.add(catNode);
            }
        }

        return new DefaultTreeModel(root);
    }

    private void filterTree() {
        String filter = searchField.getText().trim();
        treeModel = buildTreeModel(filter);
        transformTree.setModel(treeModel);
        expandAllNodes(transformTree);
    }

    private void expandAllNodes(JTree tree) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    private void addSelectedTransform() {
        TreePath path = transformTree.getSelectionPath();
        if (path == null || path.getPathCount() < 3) {
            return; // No transform leaf selected
        }
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObj = node.getUserObject();
        if (userObj instanceof TransformNode tn) {
            addRecipeStep(tn.transform().id());
        }
    }

    // -----------------------------------------------------------------------
    // CENTER PANEL: Input, Recipe, Output
    // -----------------------------------------------------------------------

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        // Input section
        JPanel inputSection = buildInputSection();

        // Recipe section
        JPanel recipeSection = buildRecipeSection();

        // Output section
        JPanel outputSection = buildOutputSection();

        // Vertical split: input | recipe | output
        JSplitPane topSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inputSection, recipeSection);
        topSplit.setResizeWeight(0.3);
        topSplit.setDividerLocation(150);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, outputSection);
        mainSplit.setResizeWeight(0.6);
        mainSplit.setDividerLocation(400);

        panel.add(mainSplit, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildInputSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setBorder(new EmptyBorder(2, 2, 2, 2));

        JLabel label = new JLabel("Input");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));

        inputArea = createSyntaxArea(false);
        inputArea.setRows(5);
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleExecution(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleExecution(); }
            @Override public void changedUpdate(DocumentEvent e) { scheduleExecution(); }
        });

        RTextScrollPane inputScroll = new RTextScrollPane(inputArea);
        inputScroll.setLineNumbersEnabled(false);

        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton pasteBtn = new JButton("Paste");
        pasteBtn.addActionListener(e -> inputArea.paste());

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> inputArea.setText(""));

        JButton loadFileBtn = new JButton("Load File");
        loadFileBtn.addActionListener(e -> loadInputFile());

        toolbar.add(pasteBtn);
        toolbar.add(clearBtn);
        toolbar.add(loadFileBtn);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(label, BorderLayout.WEST);
        topRow.add(toolbar, BorderLayout.EAST);

        section.add(topRow, BorderLayout.NORTH);
        section.add(inputScroll, BorderLayout.CENTER);

        return section;
    }

    private JPanel buildRecipeSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setBorder(new EmptyBorder(2, 2, 2, 2));

        JLabel label = new JLabel("Recipe");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));

        recipeListPanel = new JPanel();
        recipeListPanel.setLayout(new BoxLayout(recipeListPanel, BoxLayout.Y_AXIS));

        JScrollPane recipeScroll = new JScrollPane(recipeListPanel);
        recipeScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Recipe toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton clearRecipeBtn = new JButton("Clear Recipe");
        clearRecipeBtn.addActionListener(e -> clearRecipe());

        JButton saveRecipeBtn = new JButton("Save Recipe");
        saveRecipeBtn.addActionListener(e -> saveRecipe());

        JButton loadRecipeBtn = new JButton("Load Recipe");
        loadRecipeBtn.addActionListener(e -> loadRecipe());

        toolbar.add(clearRecipeBtn);
        toolbar.addSeparator();
        toolbar.add(saveRecipeBtn);
        toolbar.add(loadRecipeBtn);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(label, BorderLayout.WEST);
        topRow.add(toolbar, BorderLayout.EAST);

        section.add(topRow, BorderLayout.NORTH);
        section.add(recipeScroll, BorderLayout.CENTER);

        return section;
    }

    private JPanel buildOutputSection() {
        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.setBorder(new EmptyBorder(2, 2, 2, 2));

        JLabel label = new JLabel("Output");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));

        outputArea = createSyntaxArea(true);
        outputArea.setRows(5);

        RTextScrollPane outputScroll = new RTextScrollPane(outputArea);
        outputScroll.setLineNumbersEnabled(false);

        // Toolbar
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        JButton copyBtn = new JButton("Copy");
        copyBtn.addActionListener(e -> {
            outputArea.selectAll();
            outputArea.copy();
            outputArea.setCaretPosition(0);
        });

        JButton saveBtn = new JButton("Save to File");
        saveBtn.addActionListener(e -> saveOutputFile());

        toolbar.add(copyBtn);
        toolbar.add(saveBtn);

        outputInfoLabel = new JLabel(" ");
        outputInfoLabel.setFont(outputInfoLabel.getFont().deriveFont(11f));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(label, BorderLayout.WEST);
        topRow.add(toolbar, BorderLayout.EAST);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.add(outputInfoLabel, BorderLayout.WEST);

        section.add(topRow, BorderLayout.NORTH);
        section.add(outputScroll, BorderLayout.CENTER);
        section.add(bottomRow, BorderLayout.SOUTH);

        return section;
    }

    // -----------------------------------------------------------------------
    // RIGHT PANEL: Step Inspector
    // -----------------------------------------------------------------------

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.setPreferredSize(new Dimension(250, 0));

        showStepsCheckBox = new JCheckBox("Show Steps");
        showStepsCheckBox.setFont(showStepsCheckBox.getFont().deriveFont(Font.BOLD, 12f));
        showStepsCheckBox.addActionListener(e -> toggleStepInspector());

        stepListModel = new DefaultListModel<>();
        stepList = new JList<>(stepListModel);
        stepList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        stepList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        stepList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showStepPreview();
            }
        });

        JScrollPane stepScroll = new JScrollPane(stepList);

        stepPreviewArea = createSyntaxArea(true);
        stepPreviewArea.setRows(5);
        RTextScrollPane previewScroll = new RTextScrollPane(stepPreviewArea);
        previewScroll.setLineNumbersEnabled(false);

        rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, stepScroll, previewScroll);
        rightSplit.setResizeWeight(0.5);
        rightSplit.setDividerLocation(200);
        rightSplit.setVisible(false);

        panel.add(showStepsCheckBox, BorderLayout.NORTH);
        panel.add(rightSplit, BorderLayout.CENTER);

        return panel;
    }

    private void toggleStepInspector() {
        rightSplit.setVisible(showStepsCheckBox.isSelected());
        revalidate();
        repaint();
    }

    private void showStepPreview() {
        int idx = stepList.getSelectedIndex();
        if (idx < 0 || lastStepResults == null || idx >= lastStepResults.size()) {
            stepPreviewArea.setText("");
            return;
        }
        TransformChain.StepResult result = lastStepResults.get(idx);
        String output = new String(result.output(), StandardCharsets.UTF_8);
        stepPreviewArea.setText(output);
        stepPreviewArea.setCaretPosition(0);
    }

    // -----------------------------------------------------------------------
    // RECIPE MANAGEMENT
    // -----------------------------------------------------------------------

    private void addRecipeStep(String transformId) {
        RecipeStep step = new RecipeStep(transformId);
        recipeSteps.add(step);
        rebuildRecipeUI();
        scheduleExecution();
    }

    private void removeRecipeStep(int index) {
        if (index >= 0 && index < recipeSteps.size()) {
            recipeSteps.remove(index);
            rebuildRecipeUI();
            scheduleExecution();
        }
    }

    private void moveRecipeStep(int from, int to) {
        if (from < 0 || from >= recipeSteps.size() || to < 0 || to >= recipeSteps.size()) {
            return;
        }
        RecipeStep step = recipeSteps.remove(from);
        recipeSteps.add(to, step);
        rebuildRecipeUI();
        scheduleExecution();
    }

    private void clearRecipe() {
        recipeSteps.clear();
        rebuildRecipeUI();
        scheduleExecution();
    }

    private void rebuildRecipeUI() {
        recipeListPanel.removeAll();

        for (int i = 0; i < recipeSteps.size(); i++) {
            recipeListPanel.add(createStepPanel(i));
            recipeListPanel.add(Box.createVerticalStrut(4));
        }

        if (recipeSteps.isEmpty()) {
            JLabel placeholder = new JLabel("Double-click a transform to add it here");
            placeholder.setForeground(Color.GRAY);
            placeholder.setHorizontalAlignment(SwingConstants.CENTER);
            placeholder.setBorder(new EmptyBorder(20, 0, 20, 0));
            recipeListPanel.add(placeholder);
        }

        recipeListPanel.revalidate();
        recipeListPanel.repaint();
    }

    private JPanel createStepPanel(int index) {
        RecipeStep step = recipeSteps.get(index);
        Transform transform = registry.getById(step.transformId);

        JPanel panel = new JPanel(new BorderLayout(4, 2));
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIManager.getColor("Component.borderColor") != null
                        ? UIManager.getColor("Component.borderColor") : Color.GRAY, 1),
                new EmptyBorder(4, 4, 4, 4)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        // Top row: step number, name, control buttons
        JPanel topRow = new JPanel(new BorderLayout(4, 0));

        String displayName = (transform != null) ? transform.name() : step.transformId;
        JLabel nameLabel = new JLabel((index + 1) + ". " + displayName);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        topRow.add(nameLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));

        JButton upBtn = new JButton("▲");
        upBtn.setMargin(new Insets(1, 4, 1, 4));
        upBtn.setToolTipText("Move up");
        upBtn.setEnabled(index > 0);
        int idx = index;
        upBtn.addActionListener(e -> moveRecipeStep(idx, idx - 1));

        JButton downBtn = new JButton("▼");
        downBtn.setMargin(new Insets(1, 4, 1, 4));
        downBtn.setToolTipText("Move down");
        downBtn.setEnabled(index < recipeSteps.size() - 1);
        int idxDown = index;
        downBtn.addActionListener(e -> moveRecipeStep(idxDown, idxDown + 1));

        JButton removeBtn = new JButton("X");
        removeBtn.setMargin(new Insets(1, 6, 1, 6));
        removeBtn.setToolTipText("Remove step");
        int idxRemove = index;
        removeBtn.addActionListener(e -> removeRecipeStep(idxRemove));

        buttons.add(upBtn);
        buttons.add(downBtn);
        buttons.add(removeBtn);
        topRow.add(buttons, BorderLayout.EAST);

        panel.add(topRow, BorderLayout.NORTH);

        // Params row (if needed)
        List<String> paramNames = PARAM_DEFS.get(step.transformId);
        if (paramNames != null && !paramNames.isEmpty()) {
            JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            for (String paramName : paramNames) {
                JLabel paramLabel = new JLabel("--" + paramName + ":");
                paramLabel.setFont(paramLabel.getFont().deriveFont(11f));
                JTextField paramField = new JTextField(12);
                paramField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

                // Initialize from stored params
                String existing = step.params.getOrDefault(paramName, "");
                paramField.setText(existing);

                String pName = paramName;
                paramField.getDocument().addDocumentListener(new DocumentListener() {
                    @Override public void insertUpdate(DocumentEvent e) { updateParam(); }
                    @Override public void removeUpdate(DocumentEvent e) { updateParam(); }
                    @Override public void changedUpdate(DocumentEvent e) { updateParam(); }
                    private void updateParam() {
                        step.params.put(pName, paramField.getText());
                        scheduleExecution();
                    }
                });

                paramsPanel.add(paramLabel);
                paramsPanel.add(paramField);
            }
            panel.add(paramsPanel, BorderLayout.CENTER);
        }

        return panel;
    }

    // -----------------------------------------------------------------------
    // EXECUTION
    // -----------------------------------------------------------------------

    private void scheduleExecution() {
        debounceTimer.restart();
    }

    private void executeRecipe() {
        String input = inputArea.getText();

        if (recipeSteps.isEmpty()) {
            outputArea.setForeground(getDefaultOutputForeground());
            outputArea.setText(input);
            outputInfoLabel.setText("No transforms applied");
            lastStepResults = null;
            updateStepList();
            return;
        }

        // Build recipe string
        StringBuilder recipeStr = new StringBuilder();
        for (int i = 0; i < recipeSteps.size(); i++) {
            if (i > 0) {
                recipeStr.append(" | ");
            }
            RecipeStep step = recipeSteps.get(i);
            recipeStr.append(step.transformId);
            for (Map.Entry<String, String> param : step.params.entrySet()) {
                if (!param.getValue().isEmpty()) {
                    recipeStr.append(" --").append(param.getKey()).append(" ").append(param.getValue());
                }
            }
        }

        try {
            TransformChain chain = TransformChain.parse(recipeStr.toString(), registry);
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

            if (showStepsCheckBox.isSelected()) {
                lastStepResults = chain.executeWithIntermediates(inputBytes);
                byte[] result = lastStepResults.isEmpty() ? inputBytes
                        : lastStepResults.get(lastStepResults.size() - 1).output();
                String output = new String(result, StandardCharsets.UTF_8);
                outputArea.setForeground(getDefaultOutputForeground());
                outputArea.setText(output);
                outputArea.setCaretPosition(0);
                outputInfoLabel.setText("Length: " + result.length + " bytes | " + detectType(result));
            } else {
                lastStepResults = null;
                String output = chain.executeString(input);
                outputArea.setForeground(getDefaultOutputForeground());
                outputArea.setText(output);
                outputArea.setCaretPosition(0);
                byte[] outputBytes = output.getBytes(StandardCharsets.UTF_8);
                outputInfoLabel.setText("Length: " + outputBytes.length + " bytes | " + detectType(outputBytes));
            }

            updateStepList();

        } catch (Exception ex) {
            outputArea.setForeground(new Color(0xFF, 0x44, 0x44));
            outputArea.setText("Error: " + ex.getMessage());
            outputInfoLabel.setText("Execution failed");
            lastStepResults = null;
            updateStepList();
        }
    }

    private void updateStepList() {
        stepListModel.clear();
        if (lastStepResults != null) {
            for (int i = 0; i < lastStepResults.size(); i++) {
                TransformChain.StepResult sr = lastStepResults.get(i);
                long ms = sr.durationNanos() / 1_000_000;
                String micros = (ms == 0)
                        ? String.format("%.1fus", sr.durationNanos() / 1000.0)
                        : ms + "ms";
                stepListModel.addElement("Step " + (i + 1) + ": " + sr.transformId() + " (" + micros + ")");
            }
        }
    }

    private Color getDefaultOutputForeground() {
        Color fg = UIManager.getColor("TextArea.foreground");
        return (fg != null) ? fg : new Color(0xa9, 0xb7, 0xc6);
    }

    private String detectType(byte[] data) {
        if (data.length == 0) {
            return "empty";
        }
        // Check if it looks like hex
        String s = new String(data, StandardCharsets.UTF_8);
        if (s.matches("[0-9a-fA-F]+") && s.length() % 2 == 0) {
            return "hex";
        }
        // Check if it looks like base64
        if (s.matches("[A-Za-z0-9+/]+=*") && s.length() >= 4) {
            return "base64";
        }
        // Check for JSON
        String trimmed = s.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return "json";
        }
        // Check if printable ASCII
        boolean allPrintable = true;
        for (byte b : data) {
            int v = b & 0xFF;
            if (v < 32 && v != 9 && v != 10 && v != 13) {
                allPrintable = false;
                break;
            }
        }
        return allPrintable ? "text" : "binary";
    }

    // -----------------------------------------------------------------------
    // FILE I/O
    // -----------------------------------------------------------------------

    private void loadInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Input File");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
                inputArea.setText(content);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to read file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveOutputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Output");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(),
                        outputArea.getText(), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveRecipe() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Recipe");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                if (!file.getName().endsWith(".json")) {
                    file = new File(file.getAbsolutePath() + ".json");
                }
                List<Map<String, Object>> data = new ArrayList<>();
                for (RecipeStep step : recipeSteps) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", step.transformId);
                    if (!step.params.isEmpty()) {
                        entry.put("params", new LinkedHashMap<>(step.params));
                    }
                    data.add(entry);
                }
                Files.writeString(file.toPath(), gson.toJson(data), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to save recipe: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRecipe() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Load Recipe");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON files", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String json = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
                Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                List<Map<String, Object>> data = gson.fromJson(json, listType);
                recipeSteps.clear();
                for (Map<String, Object> entry : data) {
                    String id = (String) entry.get("id");
                    RecipeStep step = new RecipeStep(id);
                    Object paramsObj = entry.get("params");
                    if (paramsObj instanceof Map) {
                        Map<String, Object> paramsMap = (Map<String, Object>) paramsObj;
                        for (Map.Entry<String, Object> p : paramsMap.entrySet()) {
                            step.params.put(p.getKey(), String.valueOf(p.getValue()));
                        }
                    }
                    recipeSteps.add(step);
                }
                rebuildRecipeUI();
                scheduleExecution();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Failed to load recipe: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // -----------------------------------------------------------------------
    // HELPERS
    // -----------------------------------------------------------------------

    private RSyntaxTextArea createSyntaxArea(boolean readOnly) {
        RSyntaxTextArea area = new RSyntaxTextArea();
        area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        area.setCodeFoldingEnabled(false);
        area.setEditable(!readOnly);

        // FlatLaf-compatible dark styling
        area.setBackground(new Color(0x2b, 0x2b, 0x2b));
        area.setForeground(new Color(0xa9, 0xb7, 0xc6));
        area.setCaretColor(new Color(0xa9, 0xb7, 0xc6));
        area.setCurrentLineHighlightColor(new Color(0x32, 0x32, 0x32));
        area.setSelectionColor(new Color(0x21, 0x42, 0x83));

        return area;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // -----------------------------------------------------------------------
    // INNER TYPES
    // -----------------------------------------------------------------------

    /**
     * Wraps a Transform for display in the JTree.
     */
    private record TransformNode(Transform transform) {
        @Override
        public String toString() {
            return transform.name();
        }
    }

    /**
     * Mutable step in the recipe being built.
     */
    private static class RecipeStep {
        final String transformId;
        final Map<String, String> params;

        RecipeStep(String transformId) {
            this.transformId = transformId;
            this.params = new LinkedHashMap<>();
        }
    }

    /**
     * Custom tree cell renderer: bold for category nodes, normal for transform leaves.
     */
    private static class TransformTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode node) {
                if (node.getUserObject() instanceof TransformNode) {
                    // Leaf transform - normal font
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                } else if (!node.isRoot()) {
                    // Category header - bold
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
            }
            return c;
        }
    }
}
