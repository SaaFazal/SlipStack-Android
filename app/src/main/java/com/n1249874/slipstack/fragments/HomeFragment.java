package com.n1249874.slipstack.fragments;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.n1249874.slipstack.AddReceiptActivity;
import com.n1249874.slipstack.ProfileActivity;
import com.n1249874.slipstack.adapters.ReceiptAdapter;
import com.n1249874.slipstack.database.AppDatabase;
import com.n1249874.slipstack.database.ReceiptEntity;
import com.n1249874.slipstack.database.ReceiptRepository;
import com.n1249874.slipstack.databinding.FragmentHomeBinding;
import com.n1249874.slipstack.models.Receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ReceiptAdapter adapter;
    private ReceiptRepository repository;
    private String activeCategory = null;
    private androidx.lifecycle.LiveData<List<ReceiptEntity>> currentLiveData;
    private android.widget.CompoundButton.OnCheckedChangeListener filterListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ReceiptRepository(requireActivity().getApplication());

        // Setup RecyclerView
        adapter = new ReceiptAdapter();
        adapter.setOnItemClickListener(receipt -> {
            Intent intent = new Intent(getActivity(), com.n1249874.slipstack.ReceiptDetailActivity.class);
            intent.putExtra("receipt_id", receipt.getId());
            startActivity(intent);
        });
        binding.rvReceipts.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvReceipts.setAdapter(adapter);

        // Swipe-to-delete
        setupSwipeToDelete();

        // Profile icon
        binding.ivProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileActivity.class)));

        // Seed sample data on first run
        seedSampleDataIfEmpty();

        // Observe all receipts initially
        observeReceipts(null, null);

        // Search bar
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                observeReceipts(s.toString().trim(), activeCategory);
            }
        });

        activeCategory = null;
        setupChipListeners();

        binding.fabAddReceipt
                .setOnClickListener(v -> startActivity(new Intent(getActivity(), AddReceiptActivity.class)));
    }

    private void setupChipListeners() {
        filterListener = (chip, isChecked) -> {
            if (!isChecked) {
                if (activeCategory != null
                        && ((com.google.android.material.chip.Chip) chip).getText().toString().equals(activeCategory)) {
                    activeCategory = null;
                }
            } else {
                String clickedCategory = ((com.google.android.material.chip.Chip) chip).getText().toString();
                activeCategory = clickedCategory;
                updateChipStates(clickedCategory);
            }
            observeReceipts(getSearchQuery(), activeCategory);
        };

        binding.chipGroceries.setOnCheckedChangeListener(filterListener);
        binding.chipDining.setOnCheckedChangeListener(filterListener);
        binding.chipFuel.setOnCheckedChangeListener(filterListener);
    }

    private void updateChipStates(String selected) {
        binding.chipGroceries.setOnCheckedChangeListener(null);
        binding.chipDining.setOnCheckedChangeListener(null);
        binding.chipFuel.setOnCheckedChangeListener(null);

        binding.chipGroceries.setChecked("Groceries".equals(selected));
        binding.chipDining.setChecked("Dining".equals(selected));
        binding.chipFuel.setChecked("Fuel".equals(selected));

        binding.chipGroceries.setOnCheckedChangeListener(filterListener);
        binding.chipDining.setOnCheckedChangeListener(filterListener);
        binding.chipFuel.setOnCheckedChangeListener(filterListener);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Receipt receipt = adapter.getItemAt(position);

                // Remove from adapter immediately for smooth UX
                adapter.removeAt(position);

                // Delete from DB on background thread using ID
                Executors.newSingleThreadExecutor().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(requireContext());
                    // Find the entity by ID
                    ReceiptEntity entity = db.receiptDao().getByIdSync(receipt.getId());
                    if (entity != null) {
                        db.receiptDao().delete(entity);
                    }
                });

                // Show undo snackbar
                Snackbar.make(binding.getRoot(),
                        receipt.getMerchantName() + " deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> {
                            // Re-insert on undo
                            Executors.newSingleThreadExecutor().execute(() -> {
                                AppDatabase db = AppDatabase.getInstance(requireContext());
                                db.receiptDao().insert(new ReceiptEntity(
                                        receipt.getMerchantName(), receipt.getDate(),
                                        receipt.getAmount(), receipt.getCategory(),
                                        System.currentTimeMillis(),
                                        receipt.getImagePath()));
                            });
                        })
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                    int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;
                Paint paint = new Paint();
                paint.setColor(Color.parseColor("#FF5252"));

                // Draw red background
                RectF background = new RectF(
                        itemView.getRight() + dX,
                        itemView.getTop(),
                        itemView.getRight(),
                        itemView.getBottom());
                c.drawRoundRect(background, 16, 16, paint);

                // Draw "Delete" text
                paint.setColor(Color.WHITE);
                paint.setTextSize(36f);
                paint.setTextAlign(Paint.Align.CENTER);
                float textX = itemView.getRight() - 80;
                float textY = itemView.getTop() + (itemView.getHeight() / 2f) + 12;
                c.drawText("Delete", textX, textY, paint);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvReceipts);
    }

    private String getSearchQuery() {
        return binding.etSearch.getText() != null
                ? binding.etSearch.getText().toString().trim()
                : "";
    }

    private void observeReceipts(String searchQuery, String category) {
        // Remove previous observer to prevent data overlap
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }

        if (category != null) {
            currentLiveData = repository.getReceiptsByCategory(category);
            currentLiveData.observe(getViewLifecycleOwner(), entities -> {
                List<Receipt> receipts = toReceiptList(entities);
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    List<Receipt> filtered = new ArrayList<>();
                    for (Receipt r : receipts) {
                        if (r.getMerchantName().toLowerCase().contains(searchQuery.toLowerCase())) {
                            filtered.add(r);
                        }
                    }
                    adapter.setReceipts(filtered);
                } else {
                    adapter.setReceipts(receipts);
                }
            });
        } else if (searchQuery != null && !searchQuery.isEmpty()) {
            currentLiveData = repository.searchReceipts(searchQuery);
            currentLiveData.observe(getViewLifecycleOwner(),
                    entities -> adapter.setReceipts(toReceiptList(entities)));
        } else {
            currentLiveData = repository.getAllReceipts();
            currentLiveData.observe(getViewLifecycleOwner(),
                    entities -> adapter.setReceipts(toReceiptList(entities)));
        }
    }

    private List<Receipt> toReceiptList(List<ReceiptEntity> entities) {
        List<Receipt> receipts = new ArrayList<>();
        if (entities != null) {
            for (ReceiptEntity e : entities)
                receipts.add(ReceiptRepository.toReceipt(e));
        }
        return receipts;
    }

    private void seedSampleDataIfEmpty() {
        android.content.SharedPreferences prefs = requireActivity().getSharedPreferences("SlipStack",
                android.content.Context.MODE_PRIVATE);
        boolean isFirstRun = prefs.getBoolean("isFirstRun", true);

        if (isFirstRun) {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.receiptDao().insert(new ReceiptEntity("Tesco Superstore", "12 Oct", 45.32, "Groceries",
                        System.currentTimeMillis() - 86400000L * 3, null));
                db.receiptDao().insert(new ReceiptEntity("Shell Petrol", "13 Nov", 60.00, "Fuel",
                        System.currentTimeMillis() - 86400000L * 2, null));
                db.receiptDao().insert(new ReceiptEntity("Costa Coffee", "15 Nov", 10.55, "Dining",
                        System.currentTimeMillis() - 86400000L, null));

                prefs.edit().putBoolean("isFirstRun", false).apply();
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
