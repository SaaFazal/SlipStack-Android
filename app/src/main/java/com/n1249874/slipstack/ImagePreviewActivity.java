package com.n1249874.slipstack;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class ImagePreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView ivFull = findViewById(R.id.iv_full_receipt);
        ImageButton btnClose = findViewById(R.id.btn_close);

        String imagePath = getIntent().getStringExtra("image_path");
        String imageUriStr = getIntent().getStringExtra("image_uri");

        if (imagePath != null) {
            ivFull.setImageURI(Uri.fromFile(new File(imagePath)));
        } else if (imageUriStr != null) {
            ivFull.setImageURI(Uri.parse(imageUriStr));
        }

        btnClose.setOnClickListener(v -> finish());
    }
}
