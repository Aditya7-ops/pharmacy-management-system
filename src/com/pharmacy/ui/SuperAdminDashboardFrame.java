package com.pharmacy.ui;

import com.pharmacy.dao.InventoryDAO;
import com.pharmacy.model.UserSession;
import com.pharmacy.util.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SuperAdminDashboardFrame extends JFrame {
    private final UserSession session;
    private final InventoryDAO inventoryDAO;

    // Global Store Filter
    private JComboBox<String> storeFilterCombo;
    private final List<Integer> branchIdList = new ArrayList<>();

    // KPI Cards
    private JLabel valuationLabel;
    private JLabel networkUnitsLabel;
    private JLabel formulationsLabel;
    private JLabel networkExpiredLabel;

    // Tables & Models
    private DefaultTableModel salesModel;
    private DefaultTableModel globalStockModel;
    private DefaultTableModel globalExpiryModel;
    private DefaultTableModel branchStaffModel;
    private JTextField stockSearchField;
    private JComboBox<String> salesPeriodCombo;

    public SuperAdminDashboardFrame(UserSession session) {
        this.session = session;
        this.inventoryDAO = new InventoryDAO();

        setTitle("MEDICO CENTRAL HEADQUARTERS - Enterprise Command Center");
        setSize(1280, 820);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BIRCH_BG);
        setLayout(new BorderLayout(10, 10));

        // 1. Header with Universal Store Filter Selector
        add(createMasterHeaderPanel(), BorderLayout.NORTH);

        // 2. Center Tabs with Clean Titles (No Broken Glyphs)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_SUBTITLE);
        tabbedPane.setBackground(UITheme.BIRCH_BG);

        tabbedPane.addTab("  1. Network Financials & Sales Analytics  ", createFinancialsPanel());
        tabbedPane.addTab("  2. Multi-Store Medicine & Stock Queries  ", createGlobalInventoryPanel());
        tabbedPane.addTab("  3. Chain Expiry Surveillance & Audits  ", createGlobalExpiryPanel());
        tabbedPane.addTab("  4. Store Directory & In-Charge Governance  ", createBranchGovernancePanel());

        add(tabbedPane, BorderLayout.CENTER);

        refreshAllHqData();
    }

    private JPanel createMasterHeaderPanel() {
        JPanel container = new JPanel(new BorderLayout(5, 5));
        container.setOpaque(false);

        // Top Brand Banner (Solid Forest Deep)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.FOREST_DEEP);
        header.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));

        JLabel title = new JLabel("MEDICO HEALTHCARE NETWORK - CENTRAL HEADQUARTERS");
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.MINT_ACCENT);

        JLabel info = new JLabel("Super Admin: " + session.getFullName() + " | All-Stores Master Governance Clearance");
        info.setFont(UITheme.FONT_BODY);
        info.setForeground(Color.WHITE);

        header.add(title, BorderLayout.WEST);
        header.add(info, BorderLayout.EAST);
        container.add(header, BorderLayout.NORTH);

        // Filter Bar (Store Scope Selector)
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        filterBar.setBackground(UITheme.MINT_ACCENT);
        filterBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.MUTED_BORDER));

        JLabel filterLabel = new JLabel("SELECT STORE VIEW / SCOPE:");
        filterLabel.setFont(UITheme.FONT_BOLD);
        filterLabel.setForeground(UITheme.FOREST_DEEP);

        storeFilterCombo = new JComboBox<>();
        storeFilterCombo.setFont(UITheme.FONT_BOLD);
        storeFilterCombo.setPreferredSize(new Dimension(340, 30));

        populateStoreFilterDropdown();
        storeFilterCombo.addActionListener(e -> refreshAllHqData());

        // Solid green button with bold white text
        JButton refreshBtn = UITheme.createHerbalButton("Refresh Live Data", UITheme.HERBAL_SAGE);
        refreshBtn.addActionListener(e -> refreshAllHqData());

        filterBar.add(filterLabel);
        filterBar.add(storeFilterCombo);
        filterBar.add(refreshBtn);
        container.add(filterBar, BorderLayout.SOUTH);

        return container;
    }

    private void populateStoreFilterDropdown() {
        storeFilterCombo.removeAllItems();
        branchIdList.clear();

        storeFilterCombo.addItem("ALL STORES (Consolidated Network)");
        branchIdList.add(0);

        List<Object[]> branches = inventoryDAO.getBranchListSimple();
        for (Object[] b : branches) {
            int bId = Integer.parseInt(b[0].toString());
            String bName = b[1].toString();
            String bCode = b[2].toString();
            storeFilterCombo.addItem(bName + " [" + bCode + "]");
            branchIdList.add(bId);
        }
    }

    private int getSelectedBranchId() {
        int idx = storeFilterCombo.getSelectedIndex();
        if (idx >= 0 && idx < branchIdList.size()) {
            return branchIdList.get(idx);
        }
        return 0;
    }

    // -------------------------------------------------------------
    // TAB 1: FINANCIALS & COMPARATIVE ANALYTICS
    // -------------------------------------------------------------
    private JPanel createFinancialsPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 15));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 4 KPI Cards
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiPanel.setOpaque(false);

        valuationLabel = new JLabel("₹0.00", SwingConstants.CENTER);
        networkUnitsLabel = new JLabel("0", SwingConstants.CENTER);
        formulationsLabel = new JLabel("0", SwingConstants.CENTER);
        networkExpiredLabel = new JLabel("0", SwingConstants.CENTER);

        kpiPanel.add(buildBotanicalHqCard("INVENTORY VALUATION", valuationLabel, UITheme.HERBAL_SAGE));
        kpiPanel.add(buildBotanicalHqCard("TOTAL PHYSICAL UNITS", networkUnitsLabel, UITheme.FOREST_DEEP));
        kpiPanel.add(buildBotanicalHqCard("ACTIVE FORMULATIONS", formulationsLabel, UITheme.BOTANICAL_FERN));
        kpiPanel.add(buildBotanicalHqCard("EXPIRED / PENDING BATCHES", networkExpiredLabel, UITheme.CLAY_TERRACOTTA));

        panel.add(kpiPanel, BorderLayout.NORTH);

        // Sales Table Container
        JPanel tableContainer = new JPanel(new BorderLayout(8, 8));
        tableContainer.setOpaque(false);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterBar.setOpaque(false);
        filterBar.add(new JLabel("Sales Period Mode:"));

        salesPeriodCombo = new JComboBox<>(new String[]{
            "Monthly Breakdown",
            "Yearly Turnover & Annual Growth"
        });
        salesPeriodCombo.addActionListener(e -> loadSalesComparison());

        JButton exportCsvBtn = UITheme.createHerbalButton("Export Financials (CSV)", UITheme.FOREST_DEEP);
        exportCsvBtn.addActionListener(e -> exportTableToCsv(salesModel, "Medico_Sales_Report.csv"));

        filterBar.add(salesPeriodCombo);
        filterBar.add(exportCsvBtn);
        tableContainer.add(filterBar, BorderLayout.NORTH);

        String[] cols = {"Fiscal Period", "Store Branch", "Invoices Billed", "Taxable Sales (₹)", "GST Realized (₹)", "Total Turnover (₹)"};
        salesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable salesTable = new JTable(salesModel);
        UITheme.formatHerbalTable(salesTable);
        tableContainer.add(new JScrollPane(salesTable), BorderLayout.CENTER);

        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------
    // TAB 2: STORE MEDICINE & STOCK QUERIES
    // -------------------------------------------------------------
    private JPanel createGlobalInventoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 12));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBar.setOpaque(false);
        stockSearchField = new JTextField(25);
        stockSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadGlobalInventory(); }
            public void removeUpdate(DocumentEvent e) { loadGlobalInventory(); }
            public void changedUpdate(DocumentEvent e) { loadGlobalInventory(); }
        });

        JButton resetBtn = UITheme.createSecondaryButton("Reset Query");
        resetBtn.addActionListener(e -> {
            stockSearchField.setText("");
            loadGlobalInventory();
        });

        JButton exportStockBtn = UITheme.createHerbalButton("Export Stock Ledger (CSV)", UITheme.HERBAL_SAGE);
        exportStockBtn.addActionListener(e -> exportTableToCsv(globalStockModel, "Medico_Stock_Query.csv"));

        searchBar.add(new JLabel("Live Filter (Medicine / Formula / Category):"));
        searchBar.add(stockSearchField);
        searchBar.add(resetBtn);
        searchBar.add(exportStockBtn);
        panel.add(searchBar, BorderLayout.NORTH);

        String[] cols = {"Store Branch", "Medicine Brand", "Generic Formulation", "Category", "Batch No", "Expiry Date", "MRP (₹)", "Stock Units", "Total Valuation (₹)"};
        globalStockModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable stockTable = new JTable(globalStockModel);
        UITheme.formatHerbalTable(stockTable);
        panel.add(new JScrollPane(stockTable), BorderLayout.CENTER);

        return panel;
    }

    // -------------------------------------------------------------
    // TAB 3: EXPIRY SURVEILLANCE & PENDING DISCARDS
    // -------------------------------------------------------------
    private JPanel createGlobalExpiryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Expiry & Quarantined Batches Audit (Filtered by Selected Store Scope)");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.CLAY_TERRACOTTA);
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Store Location", "Helpline Phone", "Medicine Brand", "Batch No", "Expiry Date", "Expired Units", "Loss at Cost (₹)", "Regulatory Status"};
        globalExpiryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable expiryTable = new JTable(globalExpiryModel);
        UITheme.formatHerbalTable(expiryTable);
        panel.add(new JScrollPane(expiryTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);
        JButton exportExpiryBtn = UITheme.createHerbalButton("Export Expiry Audit (CSV)", UITheme.CLAY_TERRACOTTA);
        exportExpiryBtn.addActionListener(e -> exportTableToCsv(globalExpiryModel, "Medico_Expiry_Audit.csv"));
        bottomBar.add(exportExpiryBtn);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // -------------------------------------------------------------
    // TAB 4: STORE DIRECTORY & IN-CHARGE GOVERNANCE
    // -------------------------------------------------------------
    private JPanel createBranchGovernancePanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Store Branch In-Charge Directory & Contact Settings");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.FOREST_DEEP);
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Branch ID", "Code", "Store Branch Name", "Area / Locality", "Helpline Phone", "Assigned Store In-Charge", "Login Username", "User ID"};
        branchStaffModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable branchTable = new JTable(branchStaffModel);
        UITheme.formatHerbalTable(branchTable);
        panel.add(new JScrollPane(branchTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        btnPanel.setOpaque(false);

        // High-contrast deep pine button with crisp white text
        JButton editInchargeBtn = UITheme.createHerbalButton("Modify In-Charge / Update Phone / Reset Password", UITheme.FOREST_DEEP);

        editInchargeBtn.addActionListener(e -> {
            int selectedRow = branchTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a branch row to modify.");
                return;
            }

            int branchId = Integer.parseInt(branchStaffModel.getValueAt(selectedRow, 0).toString());
            String branchName = branchStaffModel.getValueAt(selectedRow, 2).toString();
            String currentPhone = branchStaffModel.getValueAt(selectedRow, 4).toString();
            String currentIncharge = branchStaffModel.getValueAt(selectedRow, 5).toString();
            int userId = Integer.parseInt(branchStaffModel.getValueAt(selectedRow, 7).toString());

            JTextField phoneField = new JTextField(currentPhone);
            JTextField inchargeField = new JTextField(currentIncharge);
            JPasswordField passField = new JPasswordField("");

            JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
            form.add(new JLabel("Branch Phone:"));
            form.add(phoneField);
            form.add(new JLabel("Store In-Charge Full Name:"));
            form.add(inchargeField);
            form.add(new JLabel("Reset Password (or leave blank):"));
            form.add(passField);

            int opt = JOptionPane.showConfirmDialog(this, form, "In-Charge Reassignment: " + branchName, JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                String newPhone = phoneField.getText().trim();
                String newName = inchargeField.getText().trim();
                String newPass = new String(passField.getPassword()).trim();

                if (inventoryDAO.updateBranchAndIncharge(branchId, newPhone, userId, newName, newPass)) {
                    JOptionPane.showMessageDialog(this, "In-charge settings updated in database!");
                    refreshAllHqData();
                } else {
                    JOptionPane.showMessageDialog(this, "Update failed.", "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnPanel.add(editInchargeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBotanicalHqCard(String label, JLabel valLabel, Color barColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.LEAF_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.MUTED_BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JPanel bar = new JPanel();
        bar.setBackground(barColor);
        bar.setPreferredSize(new Dimension(10, 4));
        card.add(bar, BorderLayout.NORTH);

        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valLabel.setForeground(barColor);

        JLabel text = new JLabel(label, SwingConstants.CENTER);
        text.setFont(new Font("Segoe UI", Font.BOLD, 10));
        text.setForeground(UITheme.EARTH_TEXT);

        card.add(valLabel, BorderLayout.CENTER);
        card.add(text, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(200, 85));
        return card;
    }

    private void loadSalesComparison() {
        salesModel.setRowCount(0);
        int branchId = getSelectedBranchId();
        boolean isYearly = (salesPeriodCombo.getSelectedIndex() == 1);
        List<Object[]> rows = inventoryDAO.getFilteredSalesAnalytics(branchId, isYearly);
        for (Object[] r : rows) salesModel.addRow(r);
    }

    private void loadGlobalInventory() {
        globalStockModel.setRowCount(0);
        int branchId = getSelectedBranchId();
        String q = (stockSearchField != null) ? stockSearchField.getText().trim() : "";
        List<Object[]> rows = inventoryDAO.getFilteredInventorySearch(branchId, q);
        for (Object[] r : rows) globalStockModel.addRow(r);
    }

    private void loadGlobalExpiry() {
        globalExpiryModel.setRowCount(0);
        int branchId = getSelectedBranchId();
        List<Object[]> rows = inventoryDAO.getFilteredExpiredBatches(branchId);
        for (Object[] r : rows) globalExpiryModel.addRow(r);
    }

    private void refreshAllHqData() {
        int branchId = getSelectedBranchId();

        Map<String, Object> metrics = inventoryDAO.getFilteredHqMetrics(branchId);
        valuationLabel.setText(String.format("₹%.2f", (double) metrics.getOrDefault("valuation", 0.0)));
        networkUnitsLabel.setText(String.valueOf(metrics.getOrDefault("units", 0)));
        formulationsLabel.setText(String.valueOf(metrics.getOrDefault("formulations", 0)));
        networkExpiredLabel.setText(String.valueOf(metrics.getOrDefault("expired", 0)));

        loadSalesComparison();
        loadGlobalInventory();
        loadGlobalExpiry();

        branchStaffModel.setRowCount(0);
        List<Object[]> branches = inventoryDAO.getAllBranchesWithIncharge();
        for (Object[] b : branches) {
            int bId = Integer.parseInt(b[0].toString());
            if (branchId == 0 || branchId == bId) {
                branchStaffModel.addRow(b);
            }
        }
    }

    private void exportTableToCsv(DefaultTableModel model, String defaultName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(chooser.getSelectedFile())) {
                for (int i = 0; i < model.getColumnCount(); i++) {
                    fw.write(model.getColumnName(i) + (i == model.getColumnCount() - 1 ? "" : ","));
                }
                fw.write("\n");
                for (int r = 0; r < model.getRowCount(); r++) {
                    for (int c = 0; c < model.getColumnCount(); c++) {
                        fw.write(model.getValueAt(r, c).toString() + (c == model.getColumnCount() - 1 ? "" : ","));
                    }
                    fw.write("\n");
                }
                JOptionPane.showMessageDialog(this, "CSV exported successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}