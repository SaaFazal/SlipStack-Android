package com.n1249874.slipstack.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.n1249874.slipstack.ProfileActivity;
import com.n1249874.slipstack.adapters.SplitHistoryAdapter;
import com.n1249874.slipstack.adapters.SplitResultAdapter;
import com.n1249874.slipstack.database.AppDatabase;
import com.n1249874.slipstack.database.ReceiptEntity;
import com.n1249874.slipstack.database.ReceiptRepository;
import com.n1249874.slipstack.database.SplitHistoryEntity;
import com.n1249874.slipstack.databinding.FragmentSplitBinding;
import com.n1249874.slipstack.models.Receipt;

import java.util.ArrayList;
import java.util.List;

public class SplitFragment extends Fragment {

    private FragmentSplitBinding binding;
    private SplitResultAdapter splitResultAdapter;
    private SplitHistoryAdapter historyAdapter;
    private final List<String> people = new ArrayList<>();
    private final List<Receipt> receipts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentSplitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.ivProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileActivity.class)));

        AppDatabase.getInstance(requireContext())
                .receiptDao()
                .getAllReceipts()
                .observe(getViewLifecycleOwner(), entities -> {
                    receipts.clear();
                    if (entities != null) {
                        for (ReceiptEntity e : entities) {
                            receipts.add(ReceiptRepository.toReceipt(e));
                        }
                    }
                    setupSpinner();
                });

        splitResultAdapter = new SplitResultAdapter();
        binding.rvSplitResult.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSplitResult.setAdapter(splitResultAdapter);

        historyAdapter = new SplitHistoryAdapter();
        binding.rvSplitHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvSplitHistory.setAdapter(historyAdapter);

        historyAdapter.setOnDeleteListener(item -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Split Record?")
                    .setMessage("Are you sure you want to remove this record from history?")
                    .setPositiveButton("Delete", (d, w) -> {
                        new Thread(() -> {
                            AppDatabase.getInstance(requireContext()).splitHistoryDao().delete(item);
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        AppDatabase.getInstance(requireContext())
                .splitHistoryDao()
                .getAllSplits()
                .observe(getViewLifecycleOwner(), splits -> {
                    if (splits != null && !splits.isEmpty()) {
                        historyAdapter.setItems(splits);
                        binding.tvNoHistory.setVisibility(View.GONE);
                    } else {
                        historyAdapter.setItems(new java.util.ArrayList<>());
                        binding.tvNoHistory.setVisibility(View.VISIBLE);
                    }
                });

        binding.btnAddPerson.setOnClickListener(v -> {
            String name = binding.etPersonName.getText() != null
                    ? binding.etPersonName.getText().toString().trim()
                    : "";
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Enter a name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (people.contains(name)) {
                Toast.makeText(getContext(), name + " already added", Toast.LENGTH_SHORT).show();
                return;
            }
            people.add(name);
            addPersonChip(name);
            binding.etPersonName.setText("");
            binding.cardResult.setVisibility(View.GONE);
        });

        // Split button
        binding.btnSplit.setOnClickListener(v -> performSplit());
    }

    private void setupSpinner() {
        List<String> receiptLabels = new ArrayList<>();
        receiptLabels.add("-- Select a receipt --");
        for (Receipt r : receipts) {
            receiptLabels.add(r.getMerchantName() + " (£" + String.format("%.2f", r.getAmount()) + ")");
        }
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, receiptLabels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerReceipt.setAdapter(spinnerAdapter);

        binding.spinnerReceipt.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Receipt selected = receipts.get(position - 1);
                    binding.tvSelectedMerchant.setText(selected.getMerchantName());
                    binding.tvSelectedDate.setText(selected.getDate());
                    binding.tvSelectedAmount.setText(String.format("£%.2f", selected.getAmount()));
                    binding.cardSelectedReceipt.setVisibility(View.VISIBLE);
                } else {
                    binding.cardSelectedReceipt.setVisibility(View.GONE);
                }
                binding.cardResult.setVisibility(View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
    }

    private void addPersonChip(String name) {
        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> {
            people.remove(name);
            binding.chipGroupPeople.removeView(chip);
            binding.cardResult.setVisibility(View.GONE);
        });
        binding.chipGroupPeople.addView(chip);
    }

    private void performSplit() {
        int selectedPos = binding.spinnerReceipt.getSelectedItemPosition();
        if (selectedPos == 0) {
            Toast.makeText(getContext(), "Please select a receipt", Toast.LENGTH_SHORT).show();
            return;
        }
        if (people.isEmpty()) {
            Toast.makeText(getContext(), "Add at least one person", Toast.LENGTH_SHORT).show();
            return;
        }

        Receipt selected = receipts.get(selectedPos - 1);
        double total = selected.getAmount();
        double perPerson = total / people.size();

        List<SplitResultAdapter.SplitEntry> entries = new ArrayList<>();
        for (String person : people) {
            entries.add(new SplitResultAdapter.SplitEntry(person, perPerson));
        }

        splitResultAdapter.setEntries(entries);
        binding.tvSplitTotal.setText(String.format("£%.2f", total));
        binding.cardResult.setVisibility(View.VISIBLE);

        // Save to History DB
        long now = System.currentTimeMillis();
        String peopleStr = String.join(", ", people);
        SplitHistoryEntity historyEntity = new SplitHistoryEntity(
                selected.getMerchantName(),
                selected.getDate(),
                total,
                people.size(),
                perPerson,
                peopleStr,
                now
        );
        new Thread(() -> {
            AppDatabase.getInstance(requireContext()).splitHistoryDao().insert(historyEntity);
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
