package com.n1249874.slipstack.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.n1249874.slipstack.MainActivity;
import com.n1249874.slipstack.R;
import com.n1249874.slipstack.databinding.ActivityLoginBinding;
import com.n1249874.slipstack.utils.AuthValidator;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate: Starting LoginActivity");

            binding = ActivityLoginBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
            Log.d(TAG, "onCreate: View binding successful");

            // Initialize Firebase Auth
            try {
                mAuth = FirebaseAuth.getInstance();
                Log.d(TAG, "onCreate: Firebase Auth initialized");
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Failed to initialize Firebase Auth", e);
                Toast.makeText(this, "Failed to initialize authentication: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Setup click listeners
            setupClickListeners();
            Log.d(TAG, "onCreate: Click listeners set up");

            // Setup text watchers for real-time validation
            setupTextWatchers();
            Log.d(TAG, "onCreate: Text watchers set up");

            Log.d(TAG, "onCreate: LoginActivity created successfully");
        } catch (Exception e) {
            Log.e(TAG, "onCreate: Fatal error in onCreate", e);
            Toast.makeText(this, "Error starting app: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            // Check if user is already signed in
            if (mAuth != null) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    navigateToHome();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "onStart: Error checking current user", e);
        }
    }

    private void setupClickListeners() {
        binding.btnLogin.setOnClickListener(v -> performLogin());

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        binding.tvCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
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
    }

    private void validateInputs() {
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();

        // Clear previous errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean isValid = true;

        // Validate email
        if (!AuthValidator.isEmpty(email) && !AuthValidator.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        // Enable/disable login button
        binding.btnLogin.setEnabled(
                !AuthValidator.isEmpty(email) &&
                        !AuthValidator.isEmpty(password) &&
                        isValid);
    }

    private void performLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

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

        // Show loading state
        binding.btnLogin.setEnabled(false);
        binding.btnLogin.setText("Logging in...");

        // Perform Firebase sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.btnLogin.setEnabled(true);
                    binding.btnLogin.setText(R.string.login_button);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMessage = getString(R.string.error_login_failed);

                        // Handle specific errors
                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("network")) {
                                    errorMessage = getString(R.string.error_network);
                                }
                            }
                        }

                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                        binding.tilPassword.setError(" ");
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
