package com.n1249874.slipstack.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "line_items", foreignKeys = @ForeignKey(entity = ReceiptEntity.class, parentColumns = "id", childColumns = "receiptId", onDelete = ForeignKey.CASCADE), indices = {
        @Index("receiptId") })
public class LineItemEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int receiptId;
    public String productName;
    public double price;

    public LineItemEntity(int receiptId, String productName, double price) {
        this.receiptId = receiptId;
        this.productName = productName;
        this.price = price;
    }
}
