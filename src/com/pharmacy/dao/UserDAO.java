package com.pharmacy.dao;

import com.pharmacy.model.UserSession;
import com.pharmacy.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public UserSession login(String username, String password) {
        String sql = "SELECT u.user_id, u.username, u.full_name, u.role, u.password_hash, " +
                     "b.branch_id, b.branch_code, b.branch_name, b.address, b.contact_no " +
                     "FROM users u " +
                     "LEFT JOIN branches b ON u.branch_id = b.branch_id " +
                     "WHERE u.username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (storedHash.equals(password)) {
                        return new UserSession(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("role"),
                            rs.getInt("branch_id"),
                            rs.getString("branch_code"),
                            rs.getString("branch_name"),
                            rs.getString("address"),
                            rs.getString("contact_no")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}