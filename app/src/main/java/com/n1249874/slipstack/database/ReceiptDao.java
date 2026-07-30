package com.n1249874.slipstack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ReceiptEntity receipt);

    @Delete
    void delete(ReceiptEntity receipt);

    @Query("DELETE FROM receipts WHERE merchantName = :merchant AND date = :date AND amount = :amount")
    void deleteByMerchantAndDate(String merchant, String date, double amount);

    @Query("SELECT * FROM receipts WHERE id = :id")
    ReceiptEntity getByIdSync(int id);

    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    LiveData<List<ReceiptEntity>> getAllReceipts();

    @Query("SELECT * FROM receipts WHERE merchantName LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    LiveData<List<ReceiptEntity>> searchReceipts(String query);

    @Query("SELECT * FROM receipts WHERE category = :category ORDER BY createdAt DESC")
    LiveData<List<ReceiptEntity>> getReceiptsByCategory(String category);

    @Query("SELECT * FROM receipts ORDER BY createdAt DESC")
    LiveData<List<ReceiptEntity>> getAllReceiptsList();

    @Query("DELETE FROM receipts")
    void deleteAll();

    @Query("SELECT * FROM receipts WHERE createdAt >= :start AND createdAt <= :end ORDER BY createdAt DESC")
    List<ReceiptEntity> getReceiptsInRangeSync(long start, long end);

    @Query("SELECT COUNT(*) FROM receipts")
    int getCount();
}
