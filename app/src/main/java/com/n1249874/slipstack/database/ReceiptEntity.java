package com.n1249874.slipstack.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "receipts")
public class ReceiptEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String merchantName;
    public String date;
    public double amount;
    public String category;
    public String imagePath;
    public long createdAt;

    public ReceiptEntity(String merchantName, String date, double amount, String category, long createdAt, String imagePath) {
        this.merchantName = merchantName;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.createdAt = createdAt;
        this.imagePath = imagePath;
    }
}
