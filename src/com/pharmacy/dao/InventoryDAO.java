package com.pharmacy.dao;

import com.pharmacy.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryDAO {

    // 1. Store-Level Metrics (Branch Dashboard Cards)
    public Map<String, Integer> getBranchMetrics(int branchId) {
        Map<String, Integer> metrics = new HashMap<>();
        String sqlTotal = "SELECT COUNT(DISTINCT catalog_id) AS total_items, " +
                          "COALESCE(SUM(current_stock), 0) AS total_units FROM branch_inventory WHERE branch_id = ?";
        String sqlLow = "SELECT COUNT(*) AS low_stock FROM branch_inventory WHERE branch_id = ? AND current_stock <= reorder_threshold";
        String sqlExpired = "SELECT COUNT(*) AS expired_batches FROM branch_inventory WHERE branch_id = ? AND expiry_date <= CURRENT_DATE";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement s1 = conn.prepareStatement(sqlTotal)) {
                s1.setInt(1, branchId);
                ResultSet rs = s1.executeQuery();
                if (rs.next()) {
                    metrics.put("catalogItems", rs.getInt("total_items"));
                    metrics.put("totalUnits", rs.getInt("total_units"));
                }
            }
            try (PreparedStatement s2 = conn.prepareStatement(sqlLow)) {
                s2.setInt(1, branchId);
                ResultSet rs = s2.executeQuery();
                if (rs.next()) metrics.put("lowStock", rs.getInt("low_stock"));
            }
            try (PreparedStatement s3 = conn.prepareStatement(sqlExpired)) {
                s3.setInt(1, branchId);
                ResultSet rs = s3.executeQuery();
                if (rs.next()) metrics.put("expiredBatches", rs.getInt("expired_batches"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return metrics;
    }

    // 2. Fetch Expired & Near-Expiry Batches for a specific branch
    public List<Object[]> getExpiredAndPendingBatches(int branchId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT bi.inventory_id, mc.brand_name, mc.generic_composition, bi.batch_no, " +
                     "bi.expiry_date, bi.current_stock, " +
                     "CASE WHEN bi.expiry_date <= CURRENT_DATE THEN 'EXPIRED' " +
                     "     WHEN bi.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY) THEN 'EXPIRING SOON' " +
                     "     ELSE 'ACTIVE' END AS shelf_status " +
                     "FROM branch_inventory bi " +
                     "JOIN master_catalog mc ON bi.catalog_id = mc.catalog_id " +
                     "WHERE bi.branch_id = ? AND bi.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY) " +
                     "ORDER BY bi.expiry_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("inventory_id"),
                    rs.getString("brand_name"),
                    rs.getString("generic_composition"),
                    rs.getString("batch_no"),
                    rs.getDate("expiry_date").toString(),
                    rs.getInt("current_stock"),
                    rs.getString("shelf_status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 3. Discard or Write-Off Expired Batches
    public boolean discardOrWriteOffBatch(int inventoryId, int branchId) {
        String deleteSql = "DELETE FROM branch_inventory WHERE inventory_id = ? AND branch_id = ?";
        String zeroOutSql = "UPDATE branch_inventory SET current_stock = 0 WHERE inventory_id = ? AND branch_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setInt(1, inventoryId);
                delStmt.setInt(2, branchId);
                return delStmt.executeUpdate() > 0;
            } catch (SQLException foreignKeyEx) {
                try (PreparedStatement zeroStmt = conn.prepareStatement(zeroOutSql)) {
                    zeroStmt.setInt(1, inventoryId);
                    zeroStmt.setInt(2, branchId);
                    return zeroStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 4. Search Catalog & Stock for a specific branch
    public List<Object[]> searchCatalogAndStock(int branchId, String queryText) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT bi.inventory_id, mc.catalog_id, mc.brand_name, mc.generic_composition, mc.category, " +
                     "mc.dosage_form, bi.batch_no, bi.expiry_date, bi.selling_price, bi.current_stock " +
                     "FROM branch_inventory bi " +
                     "JOIN master_catalog mc ON bi.catalog_id = mc.catalog_id " +
                     "WHERE bi.branch_id = ? " +
                     "AND (LOWER(mc.brand_name) LIKE ? OR LOWER(mc.generic_composition) LIKE ? OR LOWER(mc.category) LIKE ?) " +
                     "ORDER BY mc.brand_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            String wildcard = "%" + queryText.toLowerCase().trim() + "%";
            stmt.setString(2, wildcard);
            stmt.setString(3, wildcard);
            stmt.setString(4, wildcard);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("inventory_id"),
                    rs.getInt("catalog_id"),
                    rs.getString("brand_name"),
                    rs.getString("generic_composition"),
                    rs.getString("category"),
                    rs.getString("dosage_form"),
                    rs.getString("batch_no"),
                    rs.getDate("expiry_date").toString(),
                    rs.getDouble("selling_price"),
                    rs.getInt("current_stock")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 5. Check Other Branches for Available Stock
    public List<Object[]> getStockInOtherBranches(int catalogId, int currentBranchId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT b.branch_name, b.area_locality, b.contact_no, bi.batch_no, bi.expiry_date, bi.current_stock " +
                     "FROM branch_inventory bi " +
                     "JOIN branches b ON bi.branch_id = b.branch_id " +
                     "WHERE bi.catalog_id = ? AND bi.branch_id != ? AND bi.current_stock > 0 AND bi.expiry_date > CURRENT_DATE " +
                     "ORDER BY bi.current_stock DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, catalogId);
            stmt.setInt(2, currentBranchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("branch_name"),
                    rs.getString("area_locality"),
                    rs.getString("contact_no"),
                    rs.getString("batch_no"),
                    rs.getDate("expiry_date").toString(),
                    rs.getInt("current_stock")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 6. Active Dispensing Stock for POS Billing (Excludes Expired Items)
    public List<Object[]> getActiveDispensingStock(int branchId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT bi.inventory_id, mc.brand_name, bi.batch_no, bi.selling_price, bi.current_stock " +
                     "FROM branch_inventory bi " +
                     "JOIN master_catalog mc ON bi.catalog_id = mc.catalog_id " +
                     "WHERE bi.branch_id = ? AND bi.current_stock > 0 AND bi.expiry_date > CURRENT_DATE " +
                     "ORDER BY mc.brand_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("inventory_id"),
                    rs.getString("brand_name"),
                    rs.getString("batch_no"),
                    rs.getDouble("selling_price"),
                    rs.getInt("current_stock")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 7. Process Statutory Compliant Bill (Returns null on SUCCESS, or error details on FAILURE)
    public String processCompliantBill(int branchId, int userId, String customerName, String customerPhone,
                                       String doctorName, String doctorRegNo, String paymentMode,
                                       List<Object[]> cartItems, double discountedBase, double cgst, double sgst,
                                       double netTotal, String invoiceNo) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            String billSql = "INSERT INTO bills (invoice_no, branch_id, user_id, customer_name, customer_mobile, doctor_name, doctor_reg_no, " +
                             "payment_mode, subtotal, tax_amount, cgst_amount, sgst_amount, net_total) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            int billId;
            try (PreparedStatement bStmt = conn.prepareStatement(billSql, Statement.RETURN_GENERATED_KEYS)) {
                bStmt.setString(1, invoiceNo);
                bStmt.setInt(2, branchId);
                bStmt.setInt(3, userId);
                bStmt.setString(4, customerName);
                bStmt.setString(5, customerPhone);
                bStmt.setString(6, doctorName == null || doctorName.trim().isEmpty() ? "Walk-In Patient / Self" : doctorName.trim());
                bStmt.setString(7, doctorRegNo == null || doctorRegNo.trim().isEmpty() ? "NOT_APPLICABLE" : doctorRegNo.trim());
                bStmt.setString(8, paymentMode);
                bStmt.setDouble(9, discountedBase);
                bStmt.setDouble(10, cgst + sgst);
                bStmt.setDouble(11, cgst);
                bStmt.setDouble(12, sgst);
                bStmt.setDouble(13, netTotal);
                bStmt.executeUpdate();

                ResultSet rk = bStmt.getGeneratedKeys();
                if (rk.next()) {
                    billId = rk.getInt(1);
                } else {
                    conn.rollback();
                    return "Database Error: Could not generate bill invoice ID.";
                }
            }

            String itemSql = "INSERT INTO bill_items (bill_id, inventory_id, quantity, unit_price, tax_percent, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
            String checkStockSql = "SELECT current_stock, branch_id FROM branch_inventory WHERE inventory_id = ?";
            String deductStockSql = "UPDATE branch_inventory SET current_stock = current_stock - ? WHERE inventory_id = ? AND branch_id = ? AND current_stock >= ?";

            for (Object[] item : cartItems) {
                int invId = Integer.parseInt(item[0].toString().trim());
                String prodName = item[1].toString();
                int qty = Integer.parseInt(item[2].toString().trim());
                double unitPrice = Double.parseDouble(item[3].toString().trim());
                double lineTotal = Double.parseDouble(item[4].toString().trim());

                // Validation check on physical stock
                try (PreparedStatement checkStmt = conn.prepareStatement(checkStockSql)) {
                    checkStmt.setInt(1, invId);
                    ResultSet rs = checkStmt.executeQuery();
                    if (!rs.next()) {
                        conn.rollback();
                        return "Item Not Found: Inventory record #" + invId + " (" + prodName + ") does not exist in branch inventory.";
                    }
                    int itemBranch = rs.getInt("branch_id");
                    int availableStock = rs.getInt("current_stock");

                    if (itemBranch != branchId) {
                        conn.rollback();
                        return "Branch Mismatch: '" + prodName + "' belongs to branch " + itemBranch + ", but current login is branch " + branchId + ".";
                    }

                    if (availableStock < qty) {
                        conn.rollback();
                        return "Insufficient Stock: '" + prodName + "' has only " + availableStock + " units available (requested: " + qty + ").";
                    }
                }

                // Insert into bill_items
                try (PreparedStatement iStmt = conn.prepareStatement(itemSql)) {
                    iStmt.setInt(1, billId);
                    iStmt.setInt(2, invId);
                    iStmt.setInt(3, qty);
                    iStmt.setDouble(4, unitPrice);
                    iStmt.setDouble(5, 12.00);
                    iStmt.setDouble(6, lineTotal);
                    iStmt.executeUpdate();
                }

                // Deduct inventory
                try (PreparedStatement sStmt = conn.prepareStatement(deductStockSql)) {
                    sStmt.setInt(1, qty);
                    sStmt.setInt(2, invId);
                    sStmt.setInt(3, branchId);
                    sStmt.setInt(4, qty);
                    int rowsUpdated = sStmt.executeUpdate();
                    if (rowsUpdated == 0) {
                        conn.rollback();
                        return "Stock Update Conflict: Stock for " + prodName + " was modified by another terminal.";
                    }
                }
            }

            conn.commit();
            conn.setAutoCommit(true);
            return null; // null signifies complete success
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return "SQL Error during checkout: " + e.getMessage();
        }
    }

    // 8. Stock Inward Intake
    public boolean inwardNewStock(int branchId, int catalogId, String batchNo, String expiryDate,
                                  double costPrice, double sellingPrice, int quantity,
                                  String supplierInvoice, int userId) {
        String checkSql = "SELECT inventory_id FROM branch_inventory WHERE branch_id = ? AND catalog_id = ? AND batch_no = ?";
        String updateSql = "UPDATE branch_inventory SET current_stock = current_stock + ?, selling_price = ? WHERE inventory_id = ?";
        String insertSql = "INSERT INTO branch_inventory (branch_id, catalog_id, batch_no, expiry_date, cost_price, selling_price, current_stock, reorder_threshold) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, 15)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, branchId);
                checkStmt.setInt(2, catalogId);
                checkStmt.setString(3, batchNo);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next()) {
                    int invId = rs.getInt("inventory_id");
                    try (PreparedStatement uStmt = conn.prepareStatement(updateSql)) {
                        uStmt.setInt(1, quantity);
                        uStmt.setDouble(2, sellingPrice);
                        uStmt.setInt(3, invId);
                        uStmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement iStmt = conn.prepareStatement(insertSql)) {
                        iStmt.setInt(1, branchId);
                        iStmt.setInt(2, catalogId);
                        iStmt.setString(3, batchNo);
                        iStmt.setDate(4, java.sql.Date.valueOf(expiryDate));
                        iStmt.setDouble(5, costPrice);
                        iStmt.setDouble(6, sellingPrice);
                        iStmt.setInt(7, quantity);
                        iStmt.executeUpdate();
                    }
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 9. Master Catalog List
    public List<Object[]> getAllMasterCatalogItems() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT catalog_id, brand_name, generic_composition, dosage_form, mrp FROM master_catalog ORDER BY brand_name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("catalog_id"),
                    rs.getString("brand_name"),
                    rs.getString("generic_composition"),
                    rs.getString("dosage_form"),
                    rs.getDouble("mrp")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 10. Inter-Branch Transfer Requests
    public boolean createTransferRequest(int fromBranchId, int toBranchId, int catalogId, String batchNo, int units, int requestedBy) {
        String sql = "INSERT INTO stock_transfer_requests (from_branch_id, to_branch_id, catalog_id, batch_no, requested_units, requested_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, fromBranchId);
            stmt.setInt(2, toBranchId);
            stmt.setInt(3, catalogId);
            stmt.setString(4, batchNo);
            stmt.setInt(5, units);
            stmt.setInt(6, requestedBy);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Object[]> getBranchTransferRequests(int branchId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT tr.transfer_id, b_from.branch_name AS from_store, b_to.branch_name AS to_store, " +
                     "mc.brand_name, tr.batch_no, tr.requested_units, tr.status, tr.created_at " +
                     "FROM stock_transfer_requests tr " +
                     "JOIN branches b_from ON tr.from_branch_id = b_from.branch_id " +
                     "JOIN branches b_to ON tr.to_branch_id = b_to.branch_id " +
                     "JOIN master_catalog mc ON tr.catalog_id = mc.catalog_id " +
                     "WHERE tr.from_branch_id = ? OR tr.to_branch_id = ? " +
                     "ORDER BY tr.created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            stmt.setInt(2, branchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("transfer_id"),
                    rs.getString("from_store"),
                    rs.getString("to_store"),
                    rs.getString("brand_name"),
                    rs.getString("batch_no"),
                    rs.getInt("requested_units"),
                    rs.getString("status"),
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean processStockTransfer(int transferId, int operatorId) {
        String selectSql = "SELECT from_branch_id, to_branch_id, catalog_id, batch_no, requested_units " +
                           "FROM stock_transfer_requests WHERE transfer_id = ? AND status = 'PENDING'";
        String deductSql = "UPDATE branch_inventory SET current_stock = current_stock - ? " +
                           "WHERE branch_id = ? AND catalog_id = ? AND batch_no = ? AND current_stock >= ?";
        String addSql    = "INSERT INTO branch_inventory (branch_id, catalog_id, batch_no, expiry_date, cost_price, selling_price, current_stock, reorder_threshold) " +
                           "SELECT ?, catalog_id, batch_no, expiry_date, cost_price, selling_price, ?, reorder_threshold " +
                           "FROM branch_inventory WHERE branch_id = ? AND catalog_id = ? AND batch_no = ? " +
                           "ON DUPLICATE KEY UPDATE current_stock = current_stock + VALUES(current_stock)";
        String updateStatus = "UPDATE stock_transfer_requests SET status = 'COMPLETED' WHERE transfer_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            int fromBranch, toBranch, catId, units;
            String batchNo;

            try (PreparedStatement sStmt = conn.prepareStatement(selectSql)) {
                sStmt.setInt(1, transferId);
                ResultSet rs = sStmt.executeQuery();
                if (!rs.next()) { conn.rollback(); return false; }
                fromBranch = rs.getInt("from_branch_id");
                toBranch = rs.getInt("to_branch_id");
                catId = rs.getInt("catalog_id");
                batchNo = rs.getString("batch_no");
                units = rs.getInt("requested_units");
            }

            try (PreparedStatement dStmt = conn.prepareStatement(deductSql)) {
                dStmt.setInt(1, units);
                dStmt.setInt(2, fromBranch);
                dStmt.setInt(3, catId);
                dStmt.setString(4, batchNo);
                dStmt.setInt(5, units);
                if (dStmt.executeUpdate() == 0) { conn.rollback(); return false; }
            }

            try (PreparedStatement aStmt = conn.prepareStatement(addSql)) {
                aStmt.setInt(1, toBranch);
                aStmt.setInt(2, units);
                aStmt.setInt(3, fromBranch);
                aStmt.setInt(4, catId);
                aStmt.setString(5, batchNo);
                aStmt.executeUpdate();
            }

            try (PreparedStatement uStmt = conn.prepareStatement(updateStatus)) {
                uStmt.setInt(1, transferId);
                uStmt.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            e.printStackTrace();
            return false;
        }
    }

    // 11. Store Analytics (Monthly)
    public List<Object[]> getMonthlySalesPerformance(int branchId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') AS sales_month, " +
                     "COUNT(bill_id) AS total_invoices, " +
                     "COALESCE(SUM(subtotal), 0) AS total_taxable, " +
                     "COALESCE(SUM(tax_amount), 0) AS total_gst, " +
                     "COALESCE(SUM(net_total), 0) AS net_revenue " +
                     "FROM bills WHERE branch_id = ? " +
                     "GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY sales_month DESC LIMIT 12";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, branchId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("sales_month"),
                    rs.getInt("total_invoices"),
                    rs.getDouble("total_taxable"),
                    rs.getDouble("total_gst"),
                    rs.getDouble("net_revenue")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 12. Super Admin Filtered Methods
    public Map<String, Object> getFilteredHqMetrics(int branchId) {
        Map<String, Object> metrics = new HashMap<>();
        String whereClause = (branchId == 0) ? "" : "WHERE bi.branch_id = " + branchId + " ";
        String whereExpired = (branchId == 0) ? "WHERE expiry_date <= CURRENT_DATE AND current_stock > 0"
                                              : "WHERE branch_id = " + branchId + " AND expiry_date <= CURRENT_DATE AND current_stock > 0";

        String sql = "SELECT COUNT(DISTINCT bi.catalog_id) AS total_formulations, " +
                     "COALESCE(SUM(bi.current_stock), 0) AS total_units, " +
                     "COALESCE(SUM(bi.current_stock * bi.selling_price), 0) AS total_valuation, " +
                     "(SELECT COUNT(*) FROM branch_inventory " + whereExpired + ") AS expired_count " +
                     "FROM branch_inventory bi " + whereClause;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                metrics.put("formulations", rs.getInt("total_formulations"));
                metrics.put("units", rs.getInt("total_units"));
                metrics.put("valuation", rs.getDouble("total_valuation"));
                metrics.put("expired", rs.getInt("expired_count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return metrics;
    }

    public List<Object[]> getFilteredSalesAnalytics(int branchId, boolean isYearly) {
        List<Object[]> list = new ArrayList<>();
        String periodFormat = isYearly ? "%Y" : "%Y-%m";
        String where = (branchId == 0) ? "" : "WHERE b.branch_id = " + branchId + " ";

        String sql = "SELECT DATE_FORMAT(b.created_at, '" + periodFormat + "') AS period, " +
                     "br.branch_name, COUNT(b.bill_id) AS invoice_count, " +
                     "COALESCE(SUM(b.subtotal), 0) AS taxable_sales, " +
                     "COALESCE(SUM(b.tax_amount), 0) AS gst_collected, " +
                     "COALESCE(SUM(b.net_total), 0) AS net_revenue " +
                     "FROM bills b JOIN branches br ON b.branch_id = br.branch_id " +
                     where + "GROUP BY period, br.branch_name ORDER BY period DESC, net_revenue DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("period"),
                    rs.getString("branch_name"),
                    rs.getInt("invoice_count"),
                    rs.getDouble("taxable_sales"),
                    rs.getDouble("gst_collected"),
                    rs.getDouble("net_revenue")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> getFilteredInventorySearch(int branchId, String query) {
        List<Object[]> list = new ArrayList<>();
        String branchClause = (branchId == 0) ? "" : "AND bi.branch_id = " + branchId + " ";

        String sql = "SELECT br.branch_name, mc.brand_name, mc.generic_composition, mc.category, " +
                     "bi.batch_no, bi.expiry_date, bi.selling_price, bi.current_stock, " +
                     "(bi.current_stock * bi.selling_price) AS batch_valuation " +
                     "FROM branch_inventory bi " +
                     "JOIN branches br ON bi.branch_id = br.branch_id " +
                     "JOIN master_catalog mc ON bi.catalog_id = mc.catalog_id " +
                     "WHERE (LOWER(mc.brand_name) LIKE ? OR LOWER(mc.generic_composition) LIKE ? OR LOWER(br.branch_name) LIKE ?) " +
                     branchClause + "ORDER BY mc.brand_name ASC, br.branch_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String wildcard = "%" + query.toLowerCase().trim() + "%";
            stmt.setString(1, wildcard);
            stmt.setString(2, wildcard);
            stmt.setString(3, wildcard);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("branch_name"),
                    rs.getString("brand_name"),
                    rs.getString("generic_composition"),
                    rs.getString("category"),
                    rs.getString("batch_no"),
                    rs.getDate("expiry_date").toString(),
                    rs.getDouble("selling_price"),
                    rs.getInt("current_stock"),
                    rs.getDouble("batch_valuation")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> getFilteredExpiredBatches(int branchId) {
        List<Object[]> list = new ArrayList<>();
        String branchClause = (branchId == 0) ? "" : "AND bi.branch_id = " + branchId + " ";

        String sql = "SELECT br.branch_name, br.contact_no, mc.brand_name, bi.batch_no, " +
                     "bi.expiry_date, bi.current_stock, " +
                     "(bi.current_stock * bi.cost_price) AS loss_at_cost, " +
                     "CASE WHEN bi.expiry_date <= CURRENT_DATE THEN 'EXPIRED' " +
                     "     ELSE 'EXPIRING IN 60 DAYS' END AS audit_status " +
                     "FROM branch_inventory bi " +
                     "JOIN branches br ON bi.branch_id = br.branch_id " +
                     "JOIN master_catalog mc ON bi.catalog_id = mc.catalog_id " +
                     "WHERE bi.expiry_date <= DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY) AND bi.current_stock > 0 " +
                     branchClause + "ORDER BY bi.expiry_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("branch_name"),
                    rs.getString("contact_no"),
                    rs.getString("brand_name"),
                    rs.getString("batch_no"),
                    rs.getDate("expiry_date").toString(),
                    rs.getInt("current_stock"),
                    rs.getDouble("loss_at_cost"),
                    rs.getString("audit_status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> getBranchListSimple() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT branch_id, branch_name, branch_code FROM branches ORDER BY branch_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("branch_id"),
                    rs.getString("branch_name"),
                    rs.getString("branch_code")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Object[]> getAllBranchesWithIncharge() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT b.branch_id, b.branch_code, b.branch_name, b.area_locality, b.contact_no, " +
                     "u.user_id, u.full_name, u.username " +
                     "FROM branches b " +
                     "LEFT JOIN users u ON b.branch_id = u.branch_id AND u.role = 'STORE_ADMIN' " +
                     "ORDER BY b.branch_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("branch_id"),
                    rs.getString("branch_code"),
                    rs.getString("branch_name"),
                    rs.getString("area_locality"),
                    rs.getString("contact_no"),
                    rs.getString("full_name"),
                    rs.getString("username"),
                    rs.getInt("user_id")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateBranchAndIncharge(int branchId, String branchContact, int userId, String inchargeName, String newPassword) {
        String bSql = "UPDATE branches SET contact_no = ? WHERE branch_id = ?";
        String uSql = "UPDATE users SET full_name = ?" + (newPassword.isEmpty() ? "" : ", password_hash = ?") + " WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement bs = conn.prepareStatement(bSql)) {
                bs.setString(1, branchContact);
                bs.setInt(2, branchId);
                bs.executeUpdate();
            }
            try (PreparedStatement us = conn.prepareStatement(uSql)) {
                us.setString(1, inchargeName);
                if (!newPassword.isEmpty()) {
                    us.setString(2, newPassword);
                    us.setInt(3, userId);
                } else {
                    us.setInt(2, userId);
                }
                us.executeUpdate();
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}