package com.n1249874.slipstack.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "split_history")
public class SplitHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String receiptMerchant;
    public String date;
    public double totalAmount;
    public int peopleCount;
    public double amountPerPerson;
    public String peopleNames;
    public long timestamp;

    public SplitHistoryEntity(String receiptMerchant, String date, double totalAmount, int peopleCount,
            double amountPerPerson, String peopleNames, long timestamp) {
        this.receiptMerchant = receiptMerchant;
        this.date = date;
        this.totalAmount = totalAmount;
        this.peopleCount = peopleCount;
        this.amountPerPerson = amountPerPerson;
        this.peopleNames = peopleNames;
        this.timestamp = timestamp;
    }
}
