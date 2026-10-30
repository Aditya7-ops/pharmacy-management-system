package com.pharmacy.ui;

import com.pharmacy.dao.UserDAO;
import com.pharmacy.model.UserSession;
import com.pharmacy.util.UITheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

public class LoginFrame extends JFrame {
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JComboBox<String> storePresetCombo;
    private final UserDAO userDAO;

    public LoginFrame() {
        userDAO = new UserDAO();
        setTitle("Medico ERP - Chain Store Authentication");
        setSize(520, 470);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(12, 16));
        mainPanel.setBackground(UITheme.BIRCH_BG);
        mainPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        // Header Banner with Deep Forest Background
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        headerPanel.setBackground(UITheme.FOREST_DEEP);
        headerPanel.setBorder(new CompoundBorder(
            new LineBorder(UITheme.HERBAL_SAGE, 1, true),
            new EmptyBorder(14, 16, 14, 16)
        ));

        JLabel brandTitle = new JLabel("🌿 MEDICO HERBAL PHARMACY", SwingConstants.CENTER);
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brandTitle.setForeground(UITheme.MINT_ACCENT);

        JLabel brandSub = new JLabel("Enterprise Multi-Store Inventory & Point-of-Sale", SwingConstants.CENTER);
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brandSub.setForeground(Color.WHITE);

        headerPanel.add(brandTitle);
        headerPanel.add(brandSub);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Form with White Leaf Card Panel
        JPanel formCard = new JPanel(new GridBagLayout());
        formCard.setBackground(UITheme.LEAF_WHITE);
        formCard.setBorder(new CompoundBorder(
            new LineBorder(UITheme.MUTED_BORDER, 1, true),
            new EmptyBorder(16, 18, 16, 18)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Row 0: Store / Role Picker
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        JLabel roleLabel = new JLabel("Store Terminal / Role:");
        roleLabel.setFont(UITheme.FONT_BOLD);
        roleLabel.setForeground(UITheme.EARTH_TEXT);
        formCard.add(roleLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.65;
        String[] branches = {
            "★ Central Headquarters (Super Admin)",
            "Kharadi Store (MED-KHR)",
            "Wagholi Store (MED-WGH)",
            "Viman Nagar Store (MED-VIM)",
            "Kalyani Nagar Store (MED-KPD)",
            "Hinjawadi Phase 1 (MED-HNJ)"
        };
        storePresetCombo = new JComboBox<>(branches);
        storePresetCombo.setFont(UITheme.FONT_BODY);
        storePresetCombo.setBackground(Color.WHITE);
        storePresetCombo.addActionListener(e -> updateQuickFill());
        formCard.add(storePresetCombo, gbc);

        // Row 1: Username
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UITheme.FONT_BOLD);
        userLabel.setForeground(UITheme.EARTH_TEXT);
        formCard.add(userLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        usernameField = new JTextField("superadmin");
        usernameField.setFont(UITheme.FONT_BODY);
        usernameField.setBorder(new CompoundBorder(
            new LineBorder(UITheme.HERBAL_SAGE, 1),
            new EmptyBorder(5, 7, 5, 7)
        ));
        formCard.add(usernameField, gbc);

        // Row 2: Password
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UITheme.FONT_BOLD);
        passLabel.setForeground(UITheme.EARTH_TEXT);
        formCard.add(passLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        passwordField = new JPasswordField("medico@2026");
        passwordField.setFont(UITheme.FONT_BODY);
        passwordField.setBorder(new CompoundBorder(
            new LineBorder(UITheme.HERBAL_SAGE, 1),
            new EmptyBorder(5, 7, 5, 7)
        ));
        formCard.add(passwordField, gbc);

        mainPanel.add(formCard, BorderLayout.CENTER);

        // Bottom Action Buttons
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        actionPanel.setOpaque(false);

        JButton loginBtn = UITheme.createHerbalButton("✓ Access Terminal", UITheme.FOREST_DEEP);
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JButton exitBtn = UITheme.createSecondaryButton("✕ Exit Application");
        exitBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));

        loginBtn.addActionListener(e -> attemptLogin());
        exitBtn.addActionListener(e -> System.exit(0));

        actionPanel.add(loginBtn);
        actionPanel.add(exitBtn);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void updateQuickFill() {
        int idx = storePresetCombo.getSelectedIndex();
        switch (idx) {
            case 0 -> { usernameField.setText("superadmin"); passwordField.setText("medico@2026"); }
            case 1 -> { usernameField.setText("admin.kharadi"); passwordField.setText("kharadi123"); }
            case 2 -> { usernameField.setText("admin.wagholi"); passwordField.setText("wagholi123"); }
            case 3 -> { usernameField.setText("admin.vimannagar"); passwordField.setText("viman123"); }
            case 4 -> { usernameField.setText("admin.kalyani"); passwordField.setText("kalyani123"); }
            case 5 -> { usernameField.setText("admin.hinjawadi"); passwordField.setText("hinja123"); }
        }
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Provide username and password.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        UserSession session = userDAO.login(username, password);
        if (session != null) {
            dispose();
            if ("CENTRAL_ADMIN".equalsIgnoreCase(session.getRole())) {
                JOptionPane.showMessageDialog(null,
                    "Access Granted: Central Director!\nOpening Medico Headquarters Command Center.",
                    "HQ Clearance", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> new SuperAdminDashboardFrame(session).setVisible(true));
            } else {
                JOptionPane.showMessageDialog(null,
                    "Access Granted!\nBranch: " + session.getBranchName() + " (" + session.getBranchCode() + ")",
                    "Store Clearance", JOptionPane.INFORMATION_MESSAGE);
                SwingUtilities.invokeLater(() -> new DashboardFrame(session).setVisible(true));
            }
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials for this terminal.", "Login Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}