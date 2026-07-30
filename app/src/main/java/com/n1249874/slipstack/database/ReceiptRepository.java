package com.n1249874.slipstack.database;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import com.n1249874.slipstack.models.Receipt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiptRepository {

    private final ReceiptDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ReceiptRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.receiptDao();
    }

    public LiveData<List<ReceiptEntity>> getAllReceipts() {
        return dao.getAllReceipts();
    }

    public LiveData<List<ReceiptEntity>> searchReceipts(String query) {
        return dao.searchReceipts(query);
    }

    public LiveData<List<ReceiptEntity>> getReceiptsByCategory(String category) {
        return dao.getReceiptsByCategory(category);
    }

    public void insert(ReceiptEntity receipt) {
        executor.execute(() -> dao.insert(receipt));
    }

    public void delete(ReceiptEntity receipt) {
        executor.execute(() -> dao.delete(receipt));
    }

    public void deleteAll() {
        executor.execute(dao::deleteAll);
    }

    public LiveData<List<LineItemDao.ItemTrendDataResult>> searchTrends(String query, Application app) {
        return AppDatabase.getInstance(app).lineItemDao().searchTrends(query);
    }

    // convert Receipt model → ReceiptEntity
    public static ReceiptEntity fromReceipt(Receipt receipt) {
        return new ReceiptEntity(
                receipt.getMerchantName(),
                receipt.getDate(),
                receipt.getAmount(),
                receipt.getCategory(),
                System.currentTimeMillis(),
                receipt.getImagePath());
    }

    // convert ReceiptEntity → Receipt model
    public static Receipt toReceipt(ReceiptEntity entity) {
        return new Receipt(entity.id, entity.merchantName, entity.date, entity.amount, entity.category,
                entity.imagePath);
    }
}
