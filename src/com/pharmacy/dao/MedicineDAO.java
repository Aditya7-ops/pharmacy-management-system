    package com.pharmacy.dao;

import com.pharmacy.model.Medicine;
import com.pharmacy.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    public List<Medicine> getAllMedicines() {
        List<Medicine> list = new ArrayList<>();
        String sql = "SELECT * FROM medicines ORDER BY medicine_id ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Medicine m = new Medicine(
                    rs.getInt("medicine_id"),
                    rs.getString("name"),
                    rs.getString("generic_name"),
                    rs.getInt("supplier_id"),
                    rs.getString("batch_no"),
                    rs.getDate("expiry_date").toLocalDate(),
                    rs.getDouble("cost_price"),
                    rs.getDouble("selling_price"),
                    rs.getInt("stock_qty"),
                    rs.getInt("reorder_level")
                );
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean addMedicine(Medicine m) {
        String sql = "INSERT INTO medicines (name, generic_name, supplier_id, batch_no, expiry_date, cost_price, selling_price, stock_qty, reorder_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, m.getName());
            stmt.setString(2, m.getGenericName());
            stmt.setInt(3, m.getSupplierId());
            stmt.setString(4, m.getBatchNo());
            stmt.setDate(5, Date.valueOf(m.getExpiryDate()));
            stmt.setDouble(6, m.getCostPrice());
            stmt.setDouble(7, m.getSellingPrice());
            stmt.setInt(8, m.getStockQty());
            stmt.setInt(9, m.getReorderLevel());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteMedicine(int id) {
        String sql = "DELETE FROM medicines WHERE medicine_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateStock(int medicineId, int qtyDelta) {
        String sql = "UPDATE medicines SET stock_qty = stock_qty + ? WHERE medicine_id = ? AND (stock_qty + ?) >= 0";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, qtyDelta);
            stmt.setInt(2, medicineId);
            stmt.setInt(3, qtyDelta);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}