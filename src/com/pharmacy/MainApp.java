package com.pharmacy;

import com.pharmacy.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainApp {
    public static void main(String[] args) {
        try {
            // Apply native OS theme
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });
    }
}