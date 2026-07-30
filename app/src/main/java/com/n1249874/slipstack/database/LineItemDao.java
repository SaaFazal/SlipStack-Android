package com.n1249874.slipstack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LineItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LineItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LineItemEntity> items);

    @Query("SELECT * FROM line_items WHERE receiptId = :receiptId")
    List<LineItemEntity> getByReceiptId(int receiptId);

    @Query("SELECT li.productName, li.price, r.date, r.merchantName FROM line_items li " +
            "JOIN receipts r ON li.receiptId = r.id " +
            "WHERE li.productName LIKE '%' || :query || '%' " +
            "GROUP BY r.id, li.productName " +
            "ORDER BY r.createdAt DESC")
    LiveData<List<ItemTrendDataResult>> searchTrends(String query);

    static class ItemTrendDataResult {
        public String productName;
        public double price;
        public String date;
        public String merchantName;
    }
}
