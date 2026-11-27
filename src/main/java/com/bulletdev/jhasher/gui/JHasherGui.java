package com.bulletdev.jhasher.gui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class JHasherGui {

    private JHasherGui() {}

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatDarkLaf.setup();
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                    // fall back to default
                }
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
