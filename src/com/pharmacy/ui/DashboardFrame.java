package com.pharmacy.ui;

import com.pharmacy.dao.InventoryDAO;
import com.pharmacy.model.UserSession;
import com.pharmacy.util.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class DashboardFrame extends JFrame {
    private final UserSession session;
    private final InventoryDAO inventoryDAO;

    private JTabbedPane tabbedPane;

    // KPI Labels
    private JLabel totalUnitsLabel;
    private JLabel catalogItemsLabel;
    private JLabel lowStockLabel;
    private JLabel expiredBatchesLabel;

    // Tables & Models
    private JTable expiryTable;
    private DefaultTableModel expiryModel;
    private JTable catalogTable;
    private DefaultTableModel catalogModel;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JTable transferTable;
    private DefaultTableModel transferModel;
    private JTextField searchField;
    private JLabel catalogHeaderTitle;

    // Statutory Rx & Billing controls
    private JTextField customerNameField;
    private JTextField customerPhoneField;
    private JTextField doctorNameField;
    private JTextField doctorRegField;
    private JComboBox<String> paymentCombo;
    private JSpinner discountSpinner;
    private JLabel subtotalLabel;
    private JLabel discountAmountLabel;
    private JLabel cgstLabel;
    private JLabel sgstLabel;
    private JLabel netTotalLabel;
    private final List<Object[]> cartItems = new ArrayList<>();

    public DashboardFrame(UserSession session) {
        this.session = session;
        this.inventoryDAO = new InventoryDAO();

        setTitle("MEDICO HERBAL & APOTHECARY ERP - " + session.getBranchName() + " [" + session.getBranchCode() + "]");
        setSize(1240, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BIRCH_BG);
        setLayout(new BorderLayout(10, 10));

        add(createBotanicalHeader(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_SUBTITLE);
        tabbedPane.setBackground(UITheme.BIRCH_BG);

        tabbedPane.addTab("  1. Executive Dashboard & Surveillance  ", createStockAndExpiryPanel());
        tabbedPane.addTab("  2. Formulation Library & Store Stocks  ", createCatalogSearchPanel());
        tabbedPane.addTab("  3. Inter-Branch Stock Transfers  ", createTransferRequestsPanel());
        tabbedPane.addTab("  4. POS Billing & Tax Invoicing  ", createBillingPanel());

        add(tabbedPane, BorderLayout.CENTER);
        refreshAllViews();
    }

    private JPanel createBotanicalHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UITheme.FOREST_DEEP);
        header.setBorder(BorderFactory.createEmptyBorder(12, 22, 12, 22));

        JLabel title = new JLabel("MEDICO HERBAL PHARMACY - " + session.getBranchName().toUpperCase());
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(UITheme.MINT_ACCENT);

        JLabel info = new JLabel("In-Charge: " + session.getFullName() + " | Helpline: " + session.getBranchPhone() + " | License: MH-PUN-2026-X81");
        info.setFont(UITheme.FONT_BODY);
        info.setForeground(Color.WHITE);

        header.add(title, BorderLayout.WEST);
        header.add(info, BorderLayout.EAST);
        return header;
    }

    private JPanel createStockAndExpiryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        kpiPanel.setOpaque(false);

        totalUnitsLabel = new JLabel("0", SwingConstants.CENTER);
        catalogItemsLabel = new JLabel("0", SwingConstants.CENTER);
        lowStockLabel = new JLabel("0", SwingConstants.CENTER);
        expiredBatchesLabel = new JLabel("0", SwingConstants.CENTER);

        kpiPanel.add(buildBotanicalKpiCard("TOTAL STORE UNITS", totalUnitsLabel, UITheme.HERBAL_SAGE, () -> {
            tabbedPane.setSelectedIndex(1);
            searchField.setText("");
            executeCatalogSearch();
        }));

        kpiPanel.add(buildBotanicalKpiCard("ACTIVE FORMULATIONS", catalogItemsLabel, UITheme.BOTANICAL_FERN, () -> {
            tabbedPane.setSelectedIndex(1);
            searchField.setText("");
            executeCatalogSearch();
        }));

        kpiPanel.add(buildBotanicalKpiCard("CRITICAL REORDER (<=20)", lowStockLabel, UITheme.CLAY_TERRACOTTA, () -> {
            tabbedPane.setSelectedIndex(1);
            filterCatalogLowStock();
        }));

        kpiPanel.add(buildBotanicalKpiCard("EXPIRED / TO DISCARD", expiredBatchesLabel, UITheme.CLAY_TERRACOTTA.darker(), () -> {
            tabbedPane.setSelectedIndex(0);
        }));

        panel.add(kpiPanel, BorderLayout.NORTH);

        String[] cols = {"ID", "Medicine Name", "Formula / Generic", "Batch No", "Expiry Date", "Stock Units", "Status"};
        expiryModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        expiryTable = new JTable(expiryModel);
        UITheme.formatHerbalTable(expiryTable);

        JPanel tableWrapper = new JPanel(new BorderLayout(5, 8));
        tableWrapper.setOpaque(false);
        JLabel tableTitle = new JLabel("Quarantine Clearance & Batch Surveillance (Expiring within 60 Days / Expired)");
        tableTitle.setFont(UITheme.FONT_SUBTITLE);
        tableTitle.setForeground(UITheme.FOREST_DEEP);
        tableWrapper.add(tableTitle, BorderLayout.NORTH);
        tableWrapper.add(new JScrollPane(expiryTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        actionPanel.setOpaque(false);

        JButton salesAnalyticsBtn = UITheme.createHerbalButton("Sales & Monthly Ledger", UITheme.HERBAL_SAGE);
        salesAnalyticsBtn.addActionListener(e -> openSalesAnalyticsModal());

        JButton inwardStockBtn = UITheme.createHerbalButton("+ Inward New Batch Stock", UITheme.FOREST_DEEP);
        inwardStockBtn.addActionListener(e -> openStockInwardModal());

        JButton discardBtn = UITheme.createHerbalButton("Write-Off / Discard Batch", UITheme.CLAY_TERRACOTTA);
        discardBtn.addActionListener(e -> handleDiscardExpired());

        actionPanel.add(salesAnalyticsBtn);
        actionPanel.add(inwardStockBtn);
        actionPanel.add(discardBtn);
        tableWrapper.add(actionPanel, BorderLayout.SOUTH);

        panel.add(tableWrapper, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBotanicalKpiCard(String label, JLabel valueLabel, Color barColor, Runnable onClickAction) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.LEAF_WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UITheme.MUTED_BORDER, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel bar = new JPanel();
        bar.setBackground(barColor);
        bar.setPreferredSize(new Dimension(10, 4));
        card.add(bar, BorderLayout.NORTH);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(barColor);

        JLabel text = new JLabel(label, SwingConstants.CENTER);
        text.setFont(new Font("Segoe UI", Font.BOLD, 10));
        text.setForeground(UITheme.EARTH_TEXT);

        JLabel hint = new JLabel("(Click to view)", SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 9));
        hint.setForeground(Color.GRAY);

        JPanel bottom = new JPanel(new GridLayout(2, 1));
        bottom.setBackground(UITheme.LEAF_WHITE);
        bottom.add(text);
        bottom.add(hint);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        card.setPreferredSize(new Dimension(180, 85));

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (onClickAction != null) onClickAction.run(); }
            @Override public void mouseEntered(MouseEvent e) { card.setBackground(UITheme.MINT_ACCENT); bottom.setBackground(UITheme.MINT_ACCENT); }
            @Override public void mouseExited(MouseEvent e) { card.setBackground(UITheme.LEAF_WHITE); bottom.setBackground(UITheme.LEAF_WHITE); }
        });
        return card;
    }

    private JPanel createCatalogSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        catalogHeaderTitle = new JLabel("Formulation Library & Store Catalog (Double-click any row to Quick-Bill)");
        catalogHeaderTitle.setFont(UITheme.FONT_SUBTITLE);
        catalogHeaderTitle.setForeground(UITheme.FOREST_DEEP);

        JPanel searchBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchBarPanel.setOpaque(false);
        searchField = new JTextField(20);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { executeCatalogSearch(); }
            public void removeUpdate(DocumentEvent e) { executeCatalogSearch(); }
            public void changedUpdate(DocumentEvent e) { executeCatalogSearch(); }
        });

        JButton resetBtn = UITheme.createSecondaryButton("Reset Filter");
        JButton lowStockFilterBtn = UITheme.createHerbalButton("Critical Reorders", UITheme.CLAY_TERRACOTTA);
        JButton checkNearbyBtn = UITheme.createHerbalButton("Check Other Branches Stock", UITheme.FOREST_DEEP);

        resetBtn.addActionListener(e -> {
            searchField.setText("");
            executeCatalogSearch();
        });
        lowStockFilterBtn.addActionListener(e -> filterCatalogLowStock());
        checkNearbyBtn.addActionListener(e -> handleCheckNearbyStoresAndTransfer());

        searchBarPanel.add(new JLabel("Search Formula/Brand:"));
        searchBarPanel.add(searchField);
        searchBarPanel.add(resetBtn);
        searchBarPanel.add(lowStockFilterBtn);
        searchBarPanel.add(checkNearbyBtn);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.setOpaque(false);
        topContainer.add(catalogHeaderTitle, BorderLayout.NORTH);
        topContainer.add(searchBarPanel, BorderLayout.SOUTH);
        panel.add(topContainer, BorderLayout.NORTH);

        String[] cols = {"Inv ID", "Cat ID", "Brand Name", "Generic Composition", "Category", "Form", "Batch No", "Expiry", "MRP (₹)", "Stock Units"};
        catalogModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        catalogTable = new JTable(catalogModel);
        UITheme.formatHerbalTable(catalogTable);

        catalogTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = catalogTable.getSelectedRow();
                    if (row != -1) {
                        int invId = Integer.parseInt(catalogModel.getValueAt(row, 0).toString());
                        String brand = catalogModel.getValueAt(row, 2).toString();
                        double price = Double.parseDouble(catalogModel.getValueAt(row, 8).toString());
                        int stock = Integer.parseInt(catalogModel.getValueAt(row, 9).toString());
                        promptQuickBill(invId, brand, price, stock);
                    }
                }
            }
        });

        panel.add(new JScrollPane(catalogTable), BorderLayout.CENTER);
        return panel;
    }

    private void handleCheckNearbyStoresAndTransfer() {
        int selectedRow = catalogTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a medicine row first to check other stores.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int catalogId = Integer.parseInt(catalogModel.getValueAt(selectedRow, 1).toString());
        String medName = catalogModel.getValueAt(selectedRow, 2).toString();
        List<Object[]> otherBranches = inventoryDAO.getStockInOtherBranches(catalogId, session.getBranchId());

        if (otherBranches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No other branches in Pune have available stock for: " + medName);
            return;
        }

        String[] cols = {"Branch Name", "Locality", "Phone", "Batch No", "Expiry", "Available Stock"};
        DefaultTableModel bModel = new DefaultTableModel(cols, 0);
        for (Object[] b : otherBranches) bModel.addRow(b);

        JTable bTable = new JTable(bModel);
        UITheme.formatHerbalTable(bTable);
        JScrollPane scroll = new JScrollPane(bTable);
        scroll.setPreferredSize(new Dimension(680, 200));

        JPanel dialogPanel = new JPanel(new BorderLayout(8, 8));
        dialogPanel.add(scroll, BorderLayout.CENTER);

        JButton requestTransferBtn = UITheme.createHerbalButton("Initiate Stock Transfer Request From Selected Branch", UITheme.FOREST_DEEP);
        requestTransferBtn.addActionListener(ev -> {
            int bRow = bTable.getSelectedRow();
            if (bRow == -1) {
                JOptionPane.showMessageDialog(this, "Select the supplying branch row inside the table first.");
                return;
            }

            String fromBranchName = bModel.getValueAt(bRow, 0).toString();
            String batch = bModel.getValueAt(bRow, 3).toString();
            int maxStock = Integer.parseInt(bModel.getValueAt(bRow, 5).toString());

            String qtyStr = JOptionPane.showInputDialog(this, "Requesting: " + medName + "\nSupplying Branch: " + fromBranchName + "\nEnter units to transfer (Max: " + maxStock + "):", "10");
            if (qtyStr != null && !qtyStr.trim().isEmpty()) {
                try {
                    int units = Integer.parseInt(qtyStr.trim());
                    if (units <= 0 || units > maxStock) {
                        JOptionPane.showMessageDialog(this, "Invalid quantity.");
                        return;
                    }

                    int fromBranchId = getBranchIdFromName(fromBranchName);
                    if (inventoryDAO.createTransferRequest(fromBranchId, session.getBranchId(), catalogId, batch, units, session.getUserId())) {
                        JOptionPane.showMessageDialog(this, "Transfer Request Submitted Successfully!\nThe supplying store can now dispatch these units.");
                        refreshTransferTable();
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to submit transfer request.");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Please enter an integer quantity.");
                }
            }
        });

        dialogPanel.add(requestTransferBtn, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, dialogPanel, "Network Stock Lookup: " + medName, JOptionPane.PLAIN_MESSAGE);
    }

    private int getBranchIdFromName(String name) {
        if (name.contains("Kharadi")) return 1;
        if (name.contains("Wagholi")) return 2;
        if (name.contains("Viman Nagar")) return 3;
        if (name.contains("Kalyani")) return 4;
        if (name.contains("Hinjawadi")) return 5;
        return 1;
    }

    private JPanel createTransferRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Inter-Branch Inventory Transfer Orders (Outgoing Dispatches & Inward Receipts)");
        title.setFont(UITheme.FONT_SUBTITLE);
        title.setForeground(UITheme.FOREST_DEEP);
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {"Transfer ID", "Supplying Branch", "Receiving Branch", "Medicine", "Batch No", "Units", "Status", "Timestamp"};
        transferModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        transferTable = new JTable(transferModel);
        UITheme.formatHerbalTable(transferTable);
        panel.add(new JScrollPane(transferTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        bottomBar.setOpaque(false);

        JButton refreshTransfersBtn = UITheme.createSecondaryButton("Refresh Orders");
        refreshTransfersBtn.addActionListener(e -> refreshTransferTable());

        JButton executeTransferBtn = UITheme.createHerbalButton("Approve & Complete Selected Transfer", UITheme.FOREST_DEEP);
        executeTransferBtn.addActionListener(e -> {
            int row = transferTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a transfer order to approve & execute.");
                return;
            }

            int tId = Integer.parseInt(transferModel.getValueAt(row, 0).toString());
            String status = transferModel.getValueAt(row, 6).toString();
            if (!"PENDING".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this, "This transfer is already " + status);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                "Execute atomic inter-branch transfer #" + tId + "?\nStock will be subtracted from supplying branch and added here.",
                "Confirm Transfer", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (inventoryDAO.processStockTransfer(tId, session.getUserId())) {
                    JOptionPane.showMessageDialog(this, "Transfer processed successfully! Stock balances updated.");
                    refreshAllViews();
                } else {
                    JOptionPane.showMessageDialog(this, "Transfer failed. Insufficient stock at supplying branch.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        bottomBar.add(refreshTransfersBtn);
        bottomBar.add(executeTransferBtn);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private void refreshTransferTable() {
        transferModel.setRowCount(0);
        List<Object[]> list = inventoryDAO.getBranchTransferRequests(session.getBranchId());
        for (Object[] r : list) transferModel.addRow(r);
    }

    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(UITheme.BIRCH_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setPreferredSize(new Dimension(440, 500));
        leftPanel.setOpaque(false);

        JPanel customerInfo = new JPanel(new GridLayout(6, 2, 5, 8));
        customerInfo.setBackground(UITheme.LEAF_WHITE);
        customerInfo.setBorder(BorderFactory.createTitledBorder("Patient & Statutory Rx Verification"));

        customerInfo.add(new JLabel("Patient Full Name:"));
        customerNameField = new JTextField("Walk-in Patient");
        customerInfo.add(customerNameField);

        customerInfo.add(new JLabel("Patient Contact Mobile:"));
        customerPhoneField = new JTextField("+91 9800000000");
        customerInfo.add(customerPhoneField);

        customerInfo.add(new JLabel("Prescribing Doctor:"));
        doctorNameField = new JTextField("Dr. Sameer Patil, MD");
        customerInfo.add(doctorNameField);

        customerInfo.add(new JLabel("Doctor MCI Reg Number:"));
        doctorRegField = new JTextField("MCI-PUN-847291");
        customerInfo.add(doctorRegField);

        customerInfo.add(new JLabel("Payment Settlement:"));
        paymentCombo = new JComboBox<>(new String[]{"UPI", "CASH", "CARD"});
        customerInfo.add(paymentCombo);

        customerInfo.add(new JLabel("Botanical Discount (%):"));
        discountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
        discountSpinner.addChangeListener(e -> recalculateTotals());
        customerInfo.add(discountSpinner);

        leftPanel.add(customerInfo, BorderLayout.NORTH);

        // Cart Actions (Single declaration)
        JPanel cartActions = new JPanel(new GridLayout(3, 1, 5, 8));
        cartActions.setOpaque(false);
        JButton addFromStockBtn = UITheme.createHerbalButton("+ Add Medicine From Verified Stock", UITheme.HERBAL_SAGE);
        JButton removeCartBtn   = UITheme.createSecondaryButton("- Remove Selected Cart Item");
        JButton clearCartBtn    = UITheme.createHerbalButton("Clear Entire Cart", UITheme.CLAY_TERRACOTTA);

        addFromStockBtn.addActionListener(e -> openMedicinePicker());
        removeCartBtn.addActionListener(e -> removeSelectedCartItem());
        clearCartBtn.addActionListener(e -> {
            cartItems.clear();
            cartModel.setRowCount(0);
            recalculateTotals();
        });

        cartActions.add(addFromStockBtn);
        cartActions.add(removeCartBtn);
        cartActions.add(clearCartBtn);
        leftPanel.add(cartActions, BorderLayout.CENTER);

        panel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setOpaque(false);
        String[] cartCols = {"Inv ID", "Product Formulation", "Qty", "Rate (₹)", "Line Total (₹)"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        UITheme.formatHerbalTable(cartTable);
        rightPanel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // Financial Footer with CGST + SGST Split
        JPanel summaryPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        summaryPanel.setBackground(UITheme.LEAF_WHITE);
        summaryPanel.setBorder(BorderFactory.createLineBorder(UITheme.MUTED_BORDER, 1));

        JPanel totalsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 6));
        totalsPanel.setOpaque(false);
        subtotalLabel = new JLabel("Taxable: ₹0.00");
        discountAmountLabel = new JLabel("Disc: -₹0.00");
        cgstLabel = new JLabel("CGST (6%): ₹0.00");
        sgstLabel = new JLabel("SGST (6%): ₹0.00");
        netTotalLabel = new JLabel("Net: ₹0.00");
        netTotalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        netTotalLabel.setForeground(UITheme.FOREST_DEEP);

        totalsPanel.add(subtotalLabel);
        totalsPanel.add(discountAmountLabel);
        totalsPanel.add(cgstLabel);
        totalsPanel.add(sgstLabel);
        totalsPanel.add(netTotalLabel);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnPanel.setOpaque(false);

        // Single declaration for checkoutPrintBtn
        JButton checkoutPrintBtn = UITheme.createHerbalButton("Complete Sale & Print Tax Invoice Slip", UITheme.FOREST_DEEP);
        checkoutPrintBtn.addActionListener(e -> executeBillingAndPrint());

        btnPanel.add(checkoutPrintBtn);
        summaryPanel.add(totalsPanel);
        summaryPanel.add(btnPanel);

        rightPanel.add(summaryPanel, BorderLayout.SOUTH);
        panel.add(rightPanel, BorderLayout.CENTER);

        return panel;
    }

   private void openMedicinePicker() {
        List<Object[]> medicines = inventoryDAO.getActiveDispensingStock(session.getBranchId());
        if (medicines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No active stock available in this dispensary!");
            return;
        }

        String[] options = new String[medicines.size()];
        for (int i = 0; i < medicines.size(); i++) {
            Object[] m = medicines.get(i);
            // m[0] is inventory_id, m[1] is brand_name, m[2] is batch_no, m[3] is price, m[4] is stock
            options[i] = m[0] + " | " + m[1] + " (Batch: " + m[2] + ", Stock: " + m[4] + ") - ₹" + m[3];
        }

        String choice = (String) JOptionPane.showInputDialog(this, "Select Medicine to Dispense:",
                "Apothecary Dispensing Counter", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice != null) {
            int selectedInvId = Integer.parseInt(choice.split(" \\| ")[0].trim());
            Object[] selectedMed = medicines.stream()
                .filter(m -> Integer.parseInt(m[0].toString()) == selectedInvId)
                .findFirst().orElse(null);

            if (selectedMed != null) {
                promptQuickBill(selectedInvId, selectedMed[1].toString(),
                        Double.parseDouble(selectedMed[3].toString()),
                        Integer.parseInt(selectedMed[4].toString()));
            }
        }
    }
    private void promptQuickBill(int invId, String brand, double price, int availableStock) {
        if (availableStock <= 0) {
            JOptionPane.showMessageDialog(this, brand + " is completely out of stock!");
            return;
        }

        String qtyStr = JOptionPane.showInputDialog(this, "Bill: " + brand + " (MRP: ₹" + price + ")\nEnter units (Max: " + availableStock + "):", "1");
        if (qtyStr != null && !qtyStr.trim().isEmpty()) {
            try {
                int qty = Integer.parseInt(qtyStr.trim());
                if (qty <= 0 || qty > availableStock) {
                    JOptionPane.showMessageDialog(this, "Quantity must be between 1 and " + availableStock);
                    return;
                }
                double lineTotal = qty * price;
                cartItems.add(new Object[]{invId, brand, qty, price, lineTotal});
                cartModel.addRow(new Object[]{invId, brand, qty, price, lineTotal});
                recalculateTotals();
                tabbedPane.setSelectedIndex(3);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid quantity.");
            }
        }
    }

    private void removeSelectedCartItem() {
        int row = cartModel.getRowCount() - 1;
        if (row >= 0) {
            cartItems.remove(row);
            cartModel.removeRow(row);
            recalculateTotals();
        }
    }

    private void recalculateTotals() {
        double subtotal = 0;
        for (Object[] item : cartItems) subtotal += Double.parseDouble(item[4].toString());

        int discPercent = (int) discountSpinner.getValue();
        double discAmt = subtotal * (discPercent / 100.0);
        double taxableBase = subtotal - discAmt;
        double cgst = taxableBase * 0.06;
        double sgst = taxableBase * 0.06;
        double net = taxableBase + cgst + sgst;

        subtotalLabel.setText(String.format("Taxable: ₹%.2f", taxableBase));
        discountAmountLabel.setText(String.format("Disc (%d%%): -₹%.2f", discPercent, discAmt));
        cgstLabel.setText(String.format("CGST (6%%): ₹%.2f", cgst));
        sgstLabel.setText(String.format("SGST (6%%): ₹%.2f", sgst));
        netTotalLabel.setText(String.format("Net: ₹%.2f", net));
    }

   private void executeBillingAndPrint() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty.");
            return;
        }

        String customer = customerNameField.getText().trim();
        String mobile = customerPhoneField.getText().trim();
        String doctor = doctorNameField.getText().trim();
        String docReg = doctorRegField.getText().trim();
        String mode = (String) paymentCombo.getSelectedItem();

        double subtotal = 0;
        for (Object[] item : cartItems) subtotal += Double.parseDouble(item[4].toString());
        int discPercent = (int) discountSpinner.getValue();
        double discAmt = subtotal * (discPercent / 100.0);
        double taxableBase = subtotal - discAmt;
        double cgst = taxableBase * 0.06;
        double sgst = taxableBase * 0.06;
        double net = taxableBase + cgst + sgst;

        String invoiceNo = "INV-" + session.getBranchCode() + "-" + System.currentTimeMillis() % 100000;

        // Process bill and retrieve specific diagnostic error (if any)
        String errorMessage = inventoryDAO.processCompliantBill(session.getBranchId(), session.getUserId(),
                customer, mobile, doctor, docReg, mode, cartItems, taxableBase, cgst, sgst, net, invoiceNo);

        if (errorMessage == null) {
            String receiptText = generateStatutoryReceipt(invoiceNo, customer, mobile, doctor, docReg, mode, subtotal, discAmt, cgst, sgst, net);
            JTextPane pane = new JTextPane();
            pane.setFont(new Font("Monospaced", Font.PLAIN, 10));
            pane.setText(receiptText);

            int choice = JOptionPane.showConfirmDialog(this, new JScrollPane(pane), "Confirm Print Slip - " + invoiceNo, JOptionPane.OK_CANCEL_OPTION);
            if (choice == JOptionPane.OK_OPTION) {
                try {
                    pane.print(new MessageFormat("Statutory Tax Invoice"), new MessageFormat("Medico Chain ERP - Page {0}"));
                } catch (PrinterException ex) {
                    JOptionPane.showMessageDialog(this, "Printer error: " + ex.getMessage());
                }
            }

            cartItems.clear();
            cartModel.setRowCount(0);
            recalculateTotals();
            refreshAllViews();
        } else {
            // Displays the exact diagnostic message
            JOptionPane.showMessageDialog(this, errorMessage, "Billing Diagnostic Alert", JOptionPane.ERROR_MESSAGE);
        }
    }
    private String generateStatutoryReceipt(String inv, String cust, String mob, String doc, String docReg,
                                            String mode, double gross, double disc, double cgst, double sgst, double net) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

        sb.append("=========================================================\n");
        sb.append("               MEDICO HERBAL PHARMACY RETAIL             \n");
        sb.append("               ").append(session.getBranchName().toUpperCase()).append("\n");
        sb.append("   ").append(session.getBranchAddress()).append("\n");
        sb.append("   DL No: MH-PUN-2026-X81 | GSTIN: 27AABCM9876Q1Z9\n");
        sb.append("=========================================================\n");
        sb.append(String.format("Invoice No: %-18s Date: %s\n", inv, sdf.format(new Date())));
        sb.append(String.format("Patient   : %-18s Phone: %s\n", cust, mob));
        sb.append(String.format("Doctor    : %-18s Reg  : %s\n", doc, docReg));
        sb.append(String.format("Dispenser : %-18s Mode : %s\n", session.getFullName(), mode));
        sb.append("---------------------------------------------------------\n");
        sb.append(String.format("%-22s %5s %10s %12s\n", "Item Description", "Qty", "Rate(₹)", "Amount(₹)"));
        sb.append("---------------------------------------------------------\n");

        for (Object[] item : cartItems) {
            String name = item[1].toString();
            if (name.length() > 21) name = name.substring(0, 21);
            sb.append(String.format("%-22s %5d %10.2f %12.2f\n",
                    name, Integer.parseInt(item[2].toString()),
                    Double.parseDouble(item[3].toString()),
                    Double.parseDouble(item[4].toString())));
        }

        sb.append("---------------------------------------------------------\n");
        sb.append(String.format("%38s : ₹%10.2f\n", "Gross Value", gross));
        if (disc > 0) sb.append(String.format("%38s : -₹%10.2f\n", "Patient Concession", disc));
        sb.append(String.format("%38s : ₹%10.2f\n", "Central GST (CGST @ 6%)", cgst));
        sb.append(String.format("%38s : ₹%10.2f\n", "State GST (SGST @ 6%)", sgst));
        sb.append("=========================================================\n");
        sb.append(String.format("%38s : ₹%10.2f\n", "NET AMOUNT PAYABLE", net));
        sb.append("=========================================================\n");
        sb.append("   Schedule H Caution: To be sold on registered medical   \n");
        sb.append("              practitioner prescription only.             \n");
        sb.append("               THANK YOU & WISH YOU GOOD HEALTH          \n");
        return sb.toString();
    }

    private void openSalesAnalyticsModal() {
        JDialog dialog = new JDialog(this, "Branch Sales Performance - " + session.getBranchName(), true);
        dialog.setSize(800, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        String[] cols = {"Period", "Invoices", "Taxable Sales (₹)", "GST (₹)", "Net Revenue (₹)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        UITheme.formatHerbalTable(table);

        List<Object[]> rows = inventoryDAO.getMonthlySalesPerformance(session.getBranchId());
        for (Object[] r : rows) model.addRow(r);

        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void openStockInwardModal() {
        List<Object[]> catalogList = inventoryDAO.getAllMasterCatalogItems();
        if (catalogList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Master catalog is empty.");
            return;
        }

        String[] options = new String[catalogList.size()];
        for (int i = 0; i < catalogList.size(); i++) {
            Object[] r = catalogList.get(i);
            options[i] = r[0] + " | " + r[1] + " (" + r[2] + ")";
        }

        JComboBox<String> combo = new JComboBox<>(options);
        JTextField batch = new JTextField("HRB-" + (System.currentTimeMillis() % 10000));
        JTextField exp = new JTextField("2028-12-31");
        JTextField cost = new JTextField("50.00");
        JTextField sell = new JTextField("95.00");
        JTextField qty = new JTextField("100");
        JTextField inv = new JTextField("CHL-" + (System.currentTimeMillis() % 1000));

        JPanel form = new JPanel(new GridLayout(7, 2, 8, 8));
        form.add(new JLabel("Medicine:")); form.add(combo);
        form.add(new JLabel("Batch No:")); form.add(batch);
        form.add(new JLabel("Expiry (YYYY-MM-DD):")); form.add(exp);
        form.add(new JLabel("Cost (₹):")); form.add(cost);
        form.add(new JLabel("Selling MRP (₹):")); form.add(sell);
        form.add(new JLabel("Quantity:")); form.add(qty);
        form.add(new JLabel("Challan No:")); form.add(inv);

        if (JOptionPane.showConfirmDialog(this, form, "Inward Herbal Stock", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                int catId = Integer.parseInt(((String) combo.getSelectedItem()).split(" \\| ")[0]);
                inventoryDAO.inwardNewStock(session.getBranchId(), catId, batch.getText().trim(), exp.getText().trim(),
                        Double.parseDouble(cost.getText().trim()), Double.parseDouble(sell.getText().trim()),
                        Integer.parseInt(qty.getText().trim()), inv.getText().trim(), session.getUserId());
                refreshAllViews();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Input Error: " + ex.getMessage());
            }
        }
    }

    private void handleDiscardExpired() {
        int row = expiryTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an expired batch to discard.");
            return;
        }

        int invId = Integer.parseInt(expiryModel.getValueAt(row, 0).toString());
        String name = expiryModel.getValueAt(row, 1).toString();
        if (JOptionPane.showConfirmDialog(this, "Discard and quarantine " + name + "?", "Confirm Discard", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            inventoryDAO.discardOrWriteOffBatch(invId, session.getBranchId());
            refreshAllViews();
        }
    }

    private void executeCatalogSearch() {
        catalogModel.setRowCount(0);
        String q = searchField != null ? searchField.getText().trim() : "";
        List<Object[]> results = inventoryDAO.searchCatalogAndStock(session.getBranchId(), q);
        for (Object[] row : results) catalogModel.addRow(row);
    }

    private void filterCatalogLowStock() {
        catalogModel.setRowCount(0);
        List<Object[]> results = inventoryDAO.searchCatalogAndStock(session.getBranchId(), "");
        for (Object[] row : results) {
            if (Integer.parseInt(row[9].toString()) <= 20) catalogModel.addRow(row);
        }
    }

    private void refreshAllViews() {
        Map<String, Integer> metrics = inventoryDAO.getBranchMetrics(session.getBranchId());
        totalUnitsLabel.setText(String.valueOf(metrics.getOrDefault("totalUnits", 0)));
        catalogItemsLabel.setText(String.valueOf(metrics.getOrDefault("catalogItems", 0)));
        lowStockLabel.setText(String.valueOf(metrics.getOrDefault("lowStock", 0)));
        expiredBatchesLabel.setText(String.valueOf(metrics.getOrDefault("expiredBatches", 0)));

        expiryModel.setRowCount(0);
        List<Object[]> expiryList = inventoryDAO.getExpiredAndPendingBatches(session.getBranchId());
        for (Object[] r : expiryList) expiryModel.addRow(r);

        executeCatalogSearch();
        refreshTransferTable();
    }
}