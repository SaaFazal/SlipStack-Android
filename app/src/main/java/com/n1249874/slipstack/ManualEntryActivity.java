package com.n1249874.slipstack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.n1249874.slipstack.database.AppDatabase;
import com.n1249874.slipstack.database.LineItemEntity;
import com.n1249874.slipstack.database.ReceiptEntity;
import com.n1249874.slipstack.databinding.ActivityManualEntryBinding;
import com.n1249874.slipstack.databinding.ItemManualEntryRowBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ManualEntryActivity extends AppCompatActivity {

    private ActivityManualEntryBinding binding;
    private final List<ItemManualEntryRowBinding> itemRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityManualEntryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        // Set today's date as default
        String today = new SimpleDateFormat("dd/MM/yyyy", Locale.UK).format(new Date());
        binding.etDate.setText(today);

        // Add Item button
        binding.btnAddItem.setOnClickListener(v -> addItemRow());

        // Save button
        binding.btnSave.setOnClickListener(v -> saveReceipt());
    }

    private void addItemRow() {
        ItemManualEntryRowBinding rowBinding = ItemManualEntryRowBinding.inflate(
                LayoutInflater.from(this), binding.containerItems, false);

        rowBinding.btnRemoveItem.setOnClickListener(v -> {
            binding.containerItems.removeView(rowBinding.getRoot());
            itemRows.remove(rowBinding);
        });

        binding.containerItems.addView(rowBinding.getRoot());
        itemRows.add(rowBinding);
    }

    private void saveReceipt() {
        String merchant = getText(binding.etMerchant);
        String date = getText(binding.etDate);
        String totalStr = getText(binding.etTotal);
        String category = getText(binding.etCategory);

        if (merchant.isEmpty()) {
            Toast.makeText(this, "Please enter a merchant name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (date.isEmpty()) {
            Toast.makeText(this, "Please enter a date", Toast.LENGTH_SHORT).show();
            return;
        }

        double total;
        try {
            total = totalStr.isEmpty() ? 0.0 : Double.parseDouble(totalStr);
        } catch (NumberFormatException e) {
            total = 0.0;
        }

        if (category.isEmpty())
            category = "Other";

        // Collect items
        List<String> itemNames = new ArrayList<>();
        List<Double> itemPrices = new ArrayList<>();
        double itemsTotal = 0;

        for (ItemManualEntryRowBinding row : itemRows) {
            String name = getText(row.etItemName);
            String priceStr = getText(row.etItemPrice);
            if (!name.isEmpty()) {
                double price = 0;
                try {
                    price = Double.parseDouble(priceStr);
                } catch (NumberFormatException ignored) {
                }
                itemNames.add(name);
                itemPrices.add(price);
                itemsTotal += price;
            }
        }

        // If no total entered but items have prices, sum them
        if (total == 0.0 && itemsTotal > 0) {
            total = itemsTotal;
        }

        final double finalTotal = total;
        final String finalMerchant = merchant;
        final String finalDate = date;
        final String finalCategory = category;

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            ReceiptEntity entity = new ReceiptEntity(
                    finalMerchant, finalDate, finalTotal, finalCategory,
                    System.currentTimeMillis(), null);
            long receiptId = db.receiptDao().insert(entity);

            // Insert line items
            if (!itemNames.isEmpty()) {
                List<LineItemEntity> lineEntities = new ArrayList<>();
                for (int i = 0; i < itemNames.size(); i++) {
                    lineEntities.add(new LineItemEntity((int) receiptId, itemNames.get(i), itemPrices.get(i)));
                }
                db.lineItemDao().insertAll(lineEntities);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "Receipt saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
