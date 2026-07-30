package com.n1249874.slipstack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.n1249874.slipstack.MainActivity;
import com.n1249874.slipstack.R;
import com.n1249874.slipstack.databinding.ActivitySignUpBinding;
import com.n1249874.slipstack.utils.AuthValidator;

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Setup click listeners
        setupClickListeners();

        // Setup text watchers for real-time validation
        setupTextWatchers();
    }

    private void setupClickListeners() {
        binding.btnCreateAccount.setOnClickListener(v -> performSignUp());

        binding.tvBackToLogin.setOnClickListener(v -> {
            finish(); // Go back to login
        });
    }

    private void setupTextWatchers() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        binding.etEmail.addTextChangedListener(validationWatcher);
        binding.etPassword.addTextChangedListener(validationWatcher);
        binding.etConfirmPassword.addTextChangedListener(validationWatcher);
    }

    private void validateInputs() {
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        // Clear previous errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        boolean isValid = true;

        // Validate email
        if (!AuthValidator.isEmpty(email) && !AuthValidator.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        // Validate password length
        if (!AuthValidator.isEmpty(password) && !AuthValidator.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            isValid = false;
        }

        // Validate password match
        if (!AuthValidator.isEmpty(confirmPassword) &&
                !AuthValidator.isEmpty(password) &&
                !AuthValidator.passwordsMatch(password, confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            isValid = false;
        }

        // Enable/disable button
        binding.btnCreateAccount.setEnabled(
                !AuthValidator.isEmpty(email) &&
                        !AuthValidator.isEmpty(password) &&
                        !AuthValidator.isEmpty(confirmPassword) &&
                        isValid);
    }

    private void performSignUp() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        // Final validation
        if (AuthValidator.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            return;
        }

        if (!AuthValidator.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        if (AuthValidator.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_empty_password));
            return;
        }

        if (!AuthValidator.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            return;
        }

        if (!AuthValidator.passwordsMatch(password, confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            return;
        }

        // Show loading state
        binding.btnCreateAccount.setEnabled(false);
        binding.btnCreateAccount.setText("Creating account...");

        // Create Firebase account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.btnCreateAccount.setEnabled(true);
                    binding.btnCreateAccount.setText(R.string.create_account);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Show success banner
                        binding.cardSuccess.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Account created successfully!",
                                Toast.LENGTH_SHORT).show();

                        // Navigate to home after short delay
                        binding.getRoot().postDelayed(this::navigateToHome, 1500);

                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = getString(R.string.error_signup_failed);

                        // Handle specific errors
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("already in use")) {
                                    errorMessage = "This email is already registered";
                                } else if (exceptionMessage.contains("network")) {
                                    errorMessage = getString(R.string.error_network);
                                }
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
