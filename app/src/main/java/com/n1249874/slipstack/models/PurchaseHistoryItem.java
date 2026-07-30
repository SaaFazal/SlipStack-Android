package com.n1249874.slipstack.models;

public class PurchaseHistoryItem {
    private String date;
    private String store;
    private double price;

    public PurchaseHistoryItem(String date, String store, double price) {
        this.date = date;
        this.store = store;
        this.price = price;
    }

    public String getDate() {
        return date;
    }

    public String getStore() {
        return store;
    }

    public double getPrice() {
        return price;
    }
}
