package com.bulletdev.jhasher.gui;

import com.bulletdev.jhasher.gui.panels.CrackPanel;
import com.bulletdev.jhasher.gui.panels.CryptoPanel;
import com.bulletdev.jhasher.gui.panels.GeneratePanel;
import com.bulletdev.jhasher.gui.panels.IdentifyPanel;
import com.bulletdev.jhasher.gui.panels.JwtPanel;
import com.bulletdev.jhasher.gui.panels.TransformPanel;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

public final class MainFrame extends JFrame {

    public MainFrame() {
        super("JHasher v2.0 - Hash & Crypto Swiss Army Knife");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(800, 500));
        centerOnScreen();

        setJMenuBar(buildMenuBar());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Identify", new IdentifyPanel());
        tabs.addTab("Generate", new GeneratePanel());
        tabs.addTab("Transform", new TransformPanel());
        tabs.addTab("JWT", new JwtPanel());
        tabs.addTab("Crypto", new CryptoPanel());
        tabs.addTab("Crack", new CrackPanel());

        getContentPane().add(tabs, BorderLayout.CENTER);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);
        bar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "JHasher v2.0\n"
                        + "Hash & Crypto Swiss Army Knife\n\n"
                        + "Identify, generate, transform, and attack hashes.\n"
                        + "Supports JWT analysis, symmetric crypto, and more.\n\n"
                        + "Built with Java 21 + FlatLaf + Picocli",
                "About JHasher",
                JOptionPane.INFORMATION_MESSAGE
        ));
        helpMenu.add(aboutItem);
        bar.add(helpMenu);

        return bar;
    }

    private static JPanel buildPlaceholder(String text) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.ITALIC, 18f));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private void centerOnScreen() {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(
                (screen.width - getWidth()) / 2,
                (screen.height - getHeight()) / 2
        );
    }
}
