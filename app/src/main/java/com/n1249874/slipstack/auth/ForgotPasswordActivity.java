package com.n1249874.slipstack.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.n1249874.slipstack.R;
import com.n1249874.slipstack.databinding.ActivityForgotPasswordBinding;
import com.n1249874.slipstack.utils.AuthValidator;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";
    private ActivityForgotPasswordBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Setup click listeners
        setupClickListeners();

        // Setup text watcher
        setupTextWatcher();
    }

    private void setupClickListeners() {
        binding.btnSendLink.setOnClickListener(v -> sendPasswordResetEmail());

        binding.tvBackToLogin.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void setupTextWatcher() {
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateEmail();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void validateEmail() {
        String email = binding.etEmail.getText().toString();

        // Clear previous error
        binding.tilEmail.setError(null);

        boolean isValid = true;

        // Validate email
        if (!AuthValidator.isEmpty(email) && !AuthValidator.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        // Enable/disable button
        binding.btnSendLink.setEnabled(!AuthValidator.isEmpty(email) && isValid);
    }

    private void sendPasswordResetEmail() {
        String email = binding.etEmail.getText().toString().trim();

        // Final validation
        if (AuthValidator.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }

        if (!AuthValidator.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        // Show loading state
        binding.btnSendLink.setEnabled(false);
        binding.btnSendLink.setText("Sending...");

        // Send password reset email via Firebase
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    binding.btnSendLink.setEnabled(true);
                    binding.btnSendLink.setText(R.string.send_link_button);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent successfully");

                        // Show success banner
                        binding.cardSuccess.setVisibility(View.VISIBLE);
                        Toast.makeText(this,
                                "Password reset email sent! Check your inbox.",
                                Toast.LENGTH_LONG).show();

                        // Optionally navigate back after delay
                        binding.getRoot().postDelayed(this::finish, 2000);

                    } else {
                        Log.w(TAG, "sendPasswordResetEmail:failure", task.getException());
                        String errorMessage = "Failed to send reset email";

                        // Handle specific errors
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record")) {
                                    errorMessage = "No account found with this email";
                                } else if (exceptionMessage.contains("network")) {
                                    errorMessage = getString(R.string.error_network);
                                }
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        binding.tilEmail.setError(" ");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
