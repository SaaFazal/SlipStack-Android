package com.n1249874.slipstack.models;

import java.io.Serializable;

public class LineItem implements Serializable {
    public String name;
    public double price;
    public boolean isSuggested;

    public LineItem(String name, double price, boolean isSuggested) {
        this.name = name;
        this.price = price;
        this.isSuggested = isSuggested;
    }
}
