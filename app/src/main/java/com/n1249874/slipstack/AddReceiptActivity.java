package com.n1249874.slipstack;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.n1249874.slipstack.databinding.ActivityAddReceiptBinding;

public class AddReceiptActivity extends AppCompatActivity {

    private ActivityAddReceiptBinding binding;

    // Photo picker launcher
    private final ActivityResultLauncher<String> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    Intent intent = new Intent(this, ScanResultActivity.class);
                    intent.putExtra("image_uri", uri.toString());
                    startActivity(intent);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddReceiptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Scan with Camera → launch CameraActivity
        binding.btnScanCamera.setOnClickListener(v -> startActivity(new Intent(this, CameraActivity.class)));

        // Import from Photos → open photo picker
        binding.btnImportPhotos.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        // Enter Manually → launch ManualEntryActivity
        binding.btnEnterManually.setOnClickListener(v -> startActivity(new Intent(this, ManualEntryActivity.class)));
    }
}
