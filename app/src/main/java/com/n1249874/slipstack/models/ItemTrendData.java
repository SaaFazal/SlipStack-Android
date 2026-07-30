package com.n1249874.slipstack.models;

public class ItemTrendData {
    public String productName;
    public double price;
    public String date;
    public String merchantName;

    public ItemTrendData(String productName, double price, String date, String merchantName) {
        this.productName = productName;
        this.price = price;
        this.date = date;
        this.merchantName = merchantName;
    }
}
