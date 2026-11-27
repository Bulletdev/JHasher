package com.bulletdev.jhasher.tui;

import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.SeparateTextGUIThread;
import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

/**
 * Main entry point for the JHasher TUI (Terminal UI).
 * Uses Lanterna to create a full-screen terminal GUI with multiple windows.
 */
public final class JHasherTui {

    private JHasherTui() {}

    /**
     * Launch the TUI application.
     * Creates a terminal, screen, and GUI thread, then shows the main menu.
     */
    public static void launch() throws Exception {
        Terminal terminal = null;
        Screen screen = null;
        try {
            terminal = new DefaultTerminalFactory().createTerminal();
            screen = new TerminalScreen(terminal);
            screen.startScreen();

            MultiWindowTextGUI gui = new MultiWindowTextGUI(
                    new SeparateTextGUIThread.Factory(),
                    screen
            );

            ((SeparateTextGUIThread) gui.getGUIThread()).start();

            MainMenu mainMenu = new MainMenu(gui);
            gui.addWindowAndWait(mainMenu);

            ((SeparateTextGUIThread) gui.getGUIThread()).stop();
        } finally {
            if (screen != null) {
                try {
                    screen.stopScreen();
                } catch (Exception ignored) {}
            }
            if (terminal != null) {
                try {
                    terminal.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
