package com.pharmacy.model;

import java.time.LocalDate;

public class Medicine {
    private int id;
    private String name;
    private String genericName;
    private int supplierId;
    private String batchNo;
    private LocalDate expiryDate;
    private double costPrice;
    private double sellingPrice;
    private int stockQty;
    private int reorderLevel;

    public Medicine() {}

    public Medicine(int id, String name, String genericName, int supplierId, String batchNo,
                    LocalDate expiryDate, double costPrice, double sellingPrice, int stockQty, int reorderLevel) {
        this.id = id;
        this.name = name;
        this.genericName = genericName;
        this.supplierId = supplierId;
        this.batchNo = batchNo;
        this.expiryDate = expiryDate;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.stockQty = stockQty;
        this.reorderLevel = reorderLevel;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public double getCostPrice() { return costPrice; }
    public void setCostPrice(double costPrice) { this.costPrice = costPrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getStockQty() { return stockQty; }
    public void setStockQty(int stockQty) { this.stockQty = stockQty; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public boolean isLowStock() {
        return this.stockQty <= this.reorderLevel;
    }
}