package com.n1249874.slipstack.models;

public class Receipt {
    private int id;
    private String merchantName;
    private String date;
    private double amount;
    private String category;
    private String imagePath;

    public Receipt(int id, String merchantName, String date, double amount, String category, String imagePath) {
        this.id = id;
        this.merchantName = merchantName;
        this.date = date;
        this.amount = amount;
        this.category = category;
        this.imagePath = imagePath;
    }

    public int getId() {
        return id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public String getDate() {
        return date;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public String getImagePath() {
        return imagePath;
    }
}
