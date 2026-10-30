package com.pharmacy.model;

public class UserSession {
    private final int userId;
    private final String username;
    private final String fullName;
    private final String role;
    private final int branchId;
    private final String branchCode;
    private final String branchName;
    private final String branchAddress;
    private final String branchPhone;

    public UserSession(int userId, String username, String fullName, String role,
                       int branchId, String branchCode, String branchName,
                       String branchAddress, String branchPhone) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.branchId = branchId;
        this.branchCode = branchCode;
        this.branchName = branchName;
        this.branchAddress = branchAddress;
        this.branchPhone = branchPhone;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public int getBranchId() { return branchId; }
    public String getBranchCode() { return branchCode; }
    public String getBranchName() { return branchName; }
    public String getBranchAddress() { return branchAddress; }
    public String getBranchPhone() { return branchPhone; }
}