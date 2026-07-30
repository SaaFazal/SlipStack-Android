package com.n1249874.slipstack;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.n1249874.slipstack.adapters.LineItemAdapter;
import com.n1249874.slipstack.database.AppDatabase;
import com.n1249874.slipstack.database.LineItemEntity;
import com.n1249874.slipstack.database.ReceiptEntity;
import com.n1249874.slipstack.databinding.ActivityScanResultBinding;
import com.n1249874.slipstack.models.LineItem;
import com.n1249874.slipstack.ocr.ReceiptParser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ScanResultActivity extends AppCompatActivity {

    private ActivityScanResultBinding binding;
    private List<LineItem> displayItems = new ArrayList<>();
    private String capturedImagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupInputs();

        // Get image path or URI
        String imagePath = getIntent().getStringExtra("image_path");
        String imageUriStr = getIntent().getStringExtra("image_uri");
        this.capturedImagePath = (imagePath != null) ? imagePath : imageUriStr;

        if (imagePath != null) {
            File file = new File(imagePath);
            binding.ivReceiptPreview.setImageURI(Uri.fromFile(file));
            runOcrOnFile(file);
        } else if (imageUriStr != null) {
            Uri uri = Uri.parse(imageUriStr);
            binding.ivReceiptPreview.setImageURI(uri);
            runOcrOnUri(uri);
        }

        binding.btnSave.setOnClickListener(v -> saveReceipt());
        binding.btnAddItem.setOnClickListener(v -> showAddItemDialog());

        // Full-screen preview trigger
        binding.ivReceiptPreview.setOnClickListener(v -> {
            Intent intent = new Intent(this, ImagePreviewActivity.class);
            if (imagePath != null)
                intent.putExtra("image_path", imagePath);
            else if (imageUriStr != null)
                intent.putExtra("image_uri", imageUriStr);
            startActivity(intent);
        });
    }

    private void setupToolbar() {
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {

    }

    private void setupInputs() {
        // Date Picker
        binding.etDate.setOnClickListener(v -> showDatePicker());

        // Real-time validation
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                validateAndSum();
            }
        };
        binding.etMerchant.addTextChangedListener(watcher);
        binding.etTotal.addTextChangedListener(watcher);
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            String date = new SimpleDateFormat("dd MMMM yyyy", Locale.UK).format(selected.getTime());
            binding.etDate.setText(date);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void runOcrOnFile(File file) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Downscale for performance
                InputImage image = InputImage.fromFilePath(this, Uri.fromFile(file));
                runOnUiThread(() -> runOcr(image));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    binding.tvProcessing.setText("Error loading file");
                    binding.processingContainer.setVisibility(View.GONE);
                });
            }
        });
    }

    private void runOcrOnUri(Uri uri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                InputImage image = InputImage.fromFilePath(this, uri);
                runOnUiThread(() -> runOcr(image));
            } catch (IOException e) {
                runOnUiThread(() -> {
                    binding.tvProcessing.setText("Error loading URI");
                    binding.processingContainer.setVisibility(View.GONE);
                });
            }
        });
    }

    private void runOcr(InputImage image) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    // Check for "Image not clear" per documentation
                    int wordCount = 0;
                    for (com.google.mlkit.vision.text.Text.TextBlock b : text.getTextBlocks()) {
                        for (com.google.mlkit.vision.text.Text.Line l : b.getLines()) {
                            wordCount += l.getText().split("\\s+").length;
                        }
                    }

                    if (wordCount < 10) {
                        showImageNotClearDialog(text);
                    } else {
                        processOcrResult(text);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.tvProcessing.setText("OCR failed: " + e.getMessage());
                });
    }

    private void populateFields(ReceiptParser.ParsedReceipt parsed) {
        binding.etMerchant.setText(parsed.merchantName);
        String extractedDate = (parsed.date != null) ? parsed.date.trim() : "";
        binding.etDate.setText(extractedDate.isEmpty()
                ? new SimpleDateFormat("dd MMMM yyyy", Locale.UK).format(new Date())
                : extractedDate);
        binding.etTotal.setText(parsed.total > 0
                ? String.format(Locale.UK, "%.2f", parsed.total)
                : "");

        String category = "Groceries";
        String merchantLower = parsed.merchantName.toLowerCase();
        if (merchantLower.contains("petrol") || merchantLower.contains("shell") || merchantLower.contains("bp ") ||
                merchantLower.contains("esso") || merchantLower.contains("texaco") || merchantLower.contains("fuel")) {
            category = "Fuel";
        } else if (merchantLower.contains("coffee") || merchantLower.contains("cafe")
                || merchantLower.contains("starbucks") ||
                merchantLower.contains("costa") || merchantLower.contains("restaurant")
                || merchantLower.contains("dining")) {
            category = "Dining";
        }
        binding.etCategory.setText(category);

        displayItems.clear();
        for (ReceiptParser.LineItem pi : parsed.lineItems) {
            displayItems.add(new LineItem(pi.name, pi.price, true));
        }
        renderItems();

        if (displayItems.isEmpty()) {
            binding.tvNoItems.setVisibility(View.VISIBLE);
        } else {
            binding.tvNoItems.setVisibility(View.GONE);
        }
        validateAndSum();
    }

    private void renderItems() {
        binding.llLineItems.removeAllViews();
        for (int i = 0; i < displayItems.size(); i++) {
            final int index = i;
            LineItem item = displayItems.get(i);
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_line_item, binding.llLineItems, false);

            TextView tvName = itemView.findViewById(R.id.tv_item_name);
            TextView tvPrice = itemView.findViewById(R.id.tv_item_price);
            View btnDelete = itemView.findViewById(R.id.btn_delete_item);
            View tvSuggested = itemView.findViewById(R.id.tv_suggested_tag);

            tvName.setText(item.name);
            tvPrice.setText(String.format(Locale.UK, "£%.2f", item.price));
            tvSuggested.setVisibility(item.isSuggested ? View.VISIBLE : View.GONE);

            btnDelete.setOnClickListener(v -> {
                displayItems.remove(index);
                renderItems();
                validateAndSum();
            });

            itemView.setOnClickListener(v -> showEditItemDialog(index));

            binding.llLineItems.addView(itemView);
        }
    }

    private void showImageNotClearDialog(com.google.mlkit.vision.text.Text ocrText) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Image not clear?")
                .setMessage(
                        "We detected very little text. The photo might be blurry or the lighting poor.\n\nTips:\n• Keep the receipt flat\n• Ensure good lighting\n• Avoid glare")
                .setPositiveButton("Continue Anyway", (d, w) -> processOcrResult(ocrText))
                .setNegativeButton("Retake", (d, w) -> finish())
                .show();
    }

    private void processOcrResult(com.google.mlkit.vision.text.Text text) {
        ReceiptParser.ParsedReceipt parsed = ReceiptParser.parse(text);
        populateFields(parsed);
        binding.processingContainer.setVisibility(View.GONE);
    }

    private void validateAndSum() {
        String merchant = binding.etMerchant.getText().toString().trim();
        String totalStr = binding.etTotal.getText().toString().trim();
        double total = 0;
        try {
            total = Double.parseDouble(totalStr);
        } catch (NumberFormatException ignored) {
        }

        double sum = 0;
        for (LineItem item : displayItems) {
            sum += item.price;
        }

        binding.tvSumInfo.setText(String.format("Items total: £%.2f", sum));

        // Show warning if sum differs from manually entered total by more than 1p
        boolean mismatch = Math.abs(sum - total) > 0.01 && total > 0;
        binding.ivSumWarning.setVisibility(mismatch ? View.VISIBLE : View.GONE);
        binding.tvSumInfo.setTextColor(mismatch ? 0xFFFF9800 : 0xFF666666);

        // Validation for save button
        binding.btnSave.setEnabled(!merchant.isEmpty() && total > 0);
    }

    private void showAddItemDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_item, null);
        EditText etName = dialogView.findViewById(R.id.et_item_name);
        EditText etPrice = dialogView.findViewById(R.id.et_item_price);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add Item")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String priceStr = etPrice.getText().toString().trim();
                    if (!name.isEmpty()) {
                        double price = 0;
                        try {
                            price = Double.parseDouble(priceStr);
                        } catch (Exception ignored) {
                        }
                        displayItems.add(new LineItem(name, price, false));
                        renderItems();
                        binding.tvNoItems.setVisibility(View.GONE);
                        validateAndSum();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditItemDialog(int position) {
        LineItem item = displayItems.get(position);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_item, null);
        EditText etName = dialogView.findViewById(R.id.et_item_name);
        EditText etPrice = dialogView.findViewById(R.id.et_item_price);

        etName.setText(item.name);
        etPrice.setText(String.format(Locale.UK, "%.2f", item.price));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Item")
                .setView(dialogView)
                .setPositiveButton("Update", (dialog, which) -> {
                    item.name = etName.getText().toString().trim();
                    String pStr = etPrice.getText().toString().trim();
                    try {
                        item.price = Double.parseDouble(pStr);
                    } catch (Exception ignored) {
                    }
                    item.isSuggested = false; // Mark as human-accepted/edited
                    renderItems();
                    validateAndSum();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveReceipt() {
        final String merchant = binding.etMerchant.getText().toString().trim();
        final String date = binding.etDate.getText().toString().trim();
        double t = 0;
        try {
            t = Double.parseDouble(binding.etTotal.getText().toString().trim());
        } catch (Exception ignored) {
        }
        final double total = t;

        final String category = binding.etCategory.getText().toString().trim();
        final List<LineItem> finalItems = new ArrayList<>(displayItems); // Snapshot

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Ensure image is persisted to internal storage
                String persistentPath = persistImageToInternal();

                AppDatabase db = AppDatabase.getInstance(this);
                ReceiptEntity entity = new ReceiptEntity(merchant, date, total, category, System.currentTimeMillis(),
                        persistentPath);
                long receiptId = db.receiptDao().insert(entity);

                List<LineItemEntity> lineEntities = new ArrayList<>();
                for (LineItem li : finalItems) {
                    lineEntities.add(new LineItemEntity((int) receiptId, li.name, li.price));
                }
                db.lineItemDao().insertAll(lineEntities);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Receipt saved!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error saving receipt: " + e.getMessage(), Toast.LENGTH_LONG)
                        .show());
            }
        });
    }

    private String persistImageToInternal() {
        if (capturedImagePath == null)
            return null;

        try {
            File internalDir = new File(getFilesDir(), "receipts");
            if (!internalDir.exists())
                internalDir.mkdirs();

            String fileName = "receipt_" + System.currentTimeMillis() + ".jpg";
            File destFile = new File(internalDir, fileName);

            java.io.InputStream in;
            if (capturedImagePath.startsWith("content://")) {
                in = getContentResolver().openInputStream(Uri.parse(capturedImagePath));
            } else {
                in = new java.io.FileInputStream(new File(capturedImagePath));
            }

            if (in != null) {
                java.io.OutputStream out = new java.io.FileOutputStream(destFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
                return destFile.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return capturedImagePath; // Fallback to original path if copy fails
    }
}
