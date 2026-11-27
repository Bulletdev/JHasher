package com.bulletdev.jhasher.gui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class JHasherGui {

    private JHasherGui() {}

    public static void launch() {
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    FlatDarkLaf.setup();
                } catch (Exception e) {
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception ignored) {}
                }

                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            });
        } catch (Exception e) {
            System.err.println("Failed to launch GUI: " + e.getMessage());
            return;
        }

        // Block until all windows are closed
        try {
            java.awt.EventQueue.invokeAndWait(() -> {});
            while (java.awt.Window.getWindows().length > 0) {
                boolean anyVisible = false;
                for (java.awt.Window w : java.awt.Window.getWindows()) {
                    if (w.isVisible()) {
                        anyVisible = true;
                        break;
                    }
                }
                if (!anyVisible) break;
                Thread.sleep(200);
            }
        } catch (InterruptedException ignored) {
        } catch (Exception ignored) {}
    }
}
