package com.bulletdev.jhasher.tui;

import com.bulletdev.jhasher.core.transform.TransformChain;
import com.bulletdev.jhasher.core.transform.TransformRegistry;
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
 * Transform chain screen.
 * Allows the user to build and execute pipe-delimited transform recipes.
 */
public class TransformWindow extends BasicWindow {

    private final TransformRegistry registry;
    private final TextBox recipeBox;
    private final TextBox inputBox;
    private final TextBox outputBox;

    public TransformWindow() {
        super("Transform Chain");
        setHints(List.of(Window.Hint.CENTERED));

        this.registry = TransformRegistry.getInstance();

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Recipe (pipe-separated):"));
        recipeBox = new TextBox(new TerminalSize(64, 1));
        recipeBox.setText("base64encode");
        panel.addComponent(recipeBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Label("Input:"));
        inputBox = new TextBox(new TerminalSize(64, 1));
        panel.addComponent(inputBox);

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        panel.addComponent(new Button("Execute", this::onExecute));

        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(64, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(64, 0)));

        // Show available transforms
        panel.addComponent(new Label("Available transforms:"));
        List<String> ids = registry.listIds();
        String transformList = String.join(", ", ids);
        // Wrap the list into a readable label (Lanterna wraps long labels automatically within the window)
        panel.addComponent(new Label(transformList).setPreferredSize(new TerminalSize(64, 3)));

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
        String recipe = recipeBox.getText().trim();
        String input = inputBox.getText();

        if (recipe.isEmpty()) {
            showError("Please enter a transform recipe.");
            return;
        }

        if (input.isEmpty()) {
            showError("Please enter input text.");
            return;
        }

        try {
            TransformChain chain = TransformChain.parse(recipe, registry);
            String result = chain.executeString(input);
            outputBox.setText(result);
        } catch (Exception e) {
            outputBox.setText("ERROR: " + e.getMessage());
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
