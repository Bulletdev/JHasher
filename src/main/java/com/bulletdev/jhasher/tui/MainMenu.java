package com.bulletdev.jhasher.tui;

import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.LinearLayout;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.Separator;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.gui2.Direction;
import com.googlecode.lanterna.TerminalSize;

import java.util.List;

/**
 * Main menu window for JHasher TUI.
 * Provides navigation to all major features.
 */
public class MainMenu extends BasicWindow {

    private final WindowBasedTextGUI textGUI;

    public MainMenu(WindowBasedTextGUI textGUI) {
        super("JHasher v2.0");
        this.textGUI = textGUI;

        setHints(List.of(Window.Hint.CENTERED));

        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new EmptySpace(new TerminalSize(40, 1)));
        panel.addComponent(new Label("  JHasher - Hash & Crypto Toolkit"));
        panel.addComponent(new EmptySpace(new TerminalSize(40, 1)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(40, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));

        panel.addComponent(new Button("[1] Identify Hash", () -> {
            textGUI.addWindowAndWait(new IdentifyWindow());
        }));

        panel.addComponent(new Button("[2] Generate Hash", () -> {
            textGUI.addWindowAndWait(new GenerateWindow());
        }));

        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(40, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));

        panel.addComponent(new Button("[3] Transform Chain", () -> {
            textGUI.addWindowAndWait(new TransformWindow());
        }));

        panel.addComponent(new Button("[4] JWT Decode", () -> {
            textGUI.addWindowAndWait(new JwtWindow());
        }));

        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(40, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));

        panel.addComponent(new Button("[5] Encrypt / Decrypt", () -> {
            textGUI.addWindowAndWait(new CryptoWindow());
        }));

        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));
        panel.addComponent(new Separator(Direction.HORIZONTAL).setPreferredSize(new TerminalSize(40, 1)));
        panel.addComponent(new EmptySpace(new TerminalSize(40, 0)));

        panel.addComponent(new Button("[6] Exit", this::close));

        panel.addComponent(new EmptySpace(new TerminalSize(40, 1)));
        panel.addComponent(new Label("Arrow keys to navigate, Enter to select"));
        panel.addComponent(new Label("Escape to go back"));

        setComponent(panel);
    }
}
