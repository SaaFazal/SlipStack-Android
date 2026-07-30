package com.n1249874.slipstack.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.n1249874.slipstack.ProfileActivity;
import com.n1249874.slipstack.R;
import com.n1249874.slipstack.auth.LoginActivity;
import com.n1249874.slipstack.database.AppDatabase;
import com.n1249874.slipstack.database.ReceiptEntity;
import com.n1249874.slipstack.databinding.FragmentSettingsBinding;
import com.n1249874.slipstack.utils.CsvGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            binding.tvUserEmail.setText(user.getEmail());
        }

        binding.cardProfileTap.setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileActivity.class)));

        // Export CSV logic with Date Range Picker
        binding.btnExportCsv.setOnClickListener(v -> showDateRangePicker());

        binding.btnClearCache.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Cache")
                .setMessage("This will remove locally cached metadata. Actual receipts are stored in the database.")
                .setPositiveButton("Clear", (d, w) -> Toast.makeText(getContext(), "Cache cleared", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel", null)
                .show());

        binding.switchWeeklyReminder.setOnCheckedChangeListener((btn, isChecked) -> Toast.makeText(getContext(),
                "Weekly reminder " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show());

        binding.btnPrivacy.setOnClickListener(v -> Toast.makeText(getContext(), "Privacy policy: Data is stored on your device and via Firebase.", Toast.LENGTH_LONG).show());

        binding.btnDeleteAccount.setOnClickListener(v -> showDeleteAccountFlow());
    }

    private void showDateRangePicker() {
        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Export Range")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            long start = selection.first;
            long end = selection.second + (86400000 - 1); // include full last day
            executeCsvExport(start, end);
        });

        picker.show(getParentFragmentManager(), "DATE_RANGE_PICKER");
    }

    private void executeCsvExport(long start, long end) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                // Use the synchronous range query to avoid LiveData threading issues
                List<ReceiptEntity> entities = db.receiptDao().getReceiptsInRangeSync(start, end);
                
                if (entities == null || entities.isEmpty()) {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "No receipts found in this range", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                
                String csv = CsvGenerator.generateReceiptsCsv(entities);
                requireActivity().runOnUiThread(() -> saveAndShareCsv(csv));
            } catch (Exception e) {
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void saveAndShareCsv(String csvData) {
        try {
            File folder = new File(requireContext().getCacheDir(), "exports");
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, "SlipStack_Export.csv");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(csvData.getBytes());
            fos.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), "com.n1249874.slipstack.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share CSV"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteAccountFlow() {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_reauth, null);
        EditText etPass = v.findViewById(R.id.et_password);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Security Check")
                .setMessage("Please enter your current password to confirm account deletion.")
                .setView(v)
                .setPositiveButton("Delete Forever", (dialog, which) -> {
                    String password = etPass.getText().toString();
                    reauthAndDelete(password);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reauthAndDelete(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.delete().addOnCompleteListener(deleteTask -> {
                                if (deleteTask.isSuccessful()) {
                                    startActivity(new Intent(getActivity(), LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                } else {
                                    Toast.makeText(getContext(), "Delete failed: " + deleteTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
