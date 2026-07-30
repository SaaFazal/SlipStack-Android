package com.n1249874.slipstack.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SplitHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SplitHistoryEntity split);

    @Query("SELECT * FROM split_history ORDER BY timestamp DESC")
    LiveData<List<SplitHistoryEntity>> getAllSplits();

    @Delete
    void delete(SplitHistoryEntity split);

    @Query("DELETE FROM split_history")
    void deleteAll();
}
