package com.n1249874.slipstack;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.n1249874.slipstack.adapters.LineItemAdapter;
import com.n1249874.slipstack.database.AppDatabase;
import com.n1249874.slipstack.database.LineItemEntity;
import com.n1249874.slipstack.database.ReceiptEntity;
import com.n1249874.slipstack.databinding.ActivityReceiptDetailBinding;
import com.n1249874.slipstack.models.LineItem;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ReceiptDetailActivity extends AppCompatActivity {

    private ActivityReceiptDetailBinding binding;
    private LineItemAdapter itemAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReceiptDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int receiptId = getIntent().getIntExtra("receipt_id", -1);
        if (receiptId == -1) {
            Toast.makeText(this, "Error: Receipt ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadReceiptData(receiptId);
    }

    private void setupUI() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        itemAdapter = new LineItemAdapter();

        binding.rvDetailItems.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDetailItems.setAdapter(itemAdapter);
    }

    private void loadReceiptData(int receiptId) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                ReceiptEntity receipt = db.receiptDao().getByIdSync(receiptId);
                List<LineItemEntity> items = db.lineItemDao().getByReceiptId(receiptId);

                if (receipt != null) {
                    runOnUiThread(() -> populateUI(receipt, items));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Receipt not found", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void populateUI(ReceiptEntity receipt, List<LineItemEntity> items) {
        try {
            binding.tvDetailMerchant.setText(receipt.merchantName);
            binding.tvDetailDate.setText(receipt.date);
            binding.chipDetailCategory.setText(receipt.category);
            binding.tvDetailAmount.setText(NumberFormat.getCurrencyInstance(Locale.UK).format(receipt.amount));

            // Show diagnostic toast
            int count = (items != null) ? items.size() : 0;
            Toast.makeText(this, "Loaded " + count + " items", Toast.LENGTH_SHORT).show();

            // Load image safely
            if (receipt.imagePath != null && !receipt.imagePath.isEmpty()) {
                File file = new File(receipt.imagePath);
                if (file.exists()) {
                    binding.ivDetailReceipt.setImageURI(Uri.fromFile(file));
                } else if (receipt.imagePath.startsWith("content://")) {
                    binding.ivDetailReceipt.setImageURI(Uri.parse(receipt.imagePath));
                }
            }

            // Populate items
            List<LineItem> displayItems = new ArrayList<>();
            if (items != null) {
                for (LineItemEntity ie : items) {
                    displayItems.add(new LineItem(ie.productName, ie.price, false));
                }
            }
            itemAdapter.setItems(displayItems);
        } catch (Exception e) {
            Toast.makeText(this, "Error displaying details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
