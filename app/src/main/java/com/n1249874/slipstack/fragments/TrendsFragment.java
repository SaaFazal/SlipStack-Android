package com.n1249874.slipstack.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.n1249874.slipstack.ProfileActivity;
import com.n1249874.slipstack.adapters.PurchaseHistoryAdapter;
import com.n1249874.slipstack.database.LineItemDao;
import com.n1249874.slipstack.database.ReceiptRepository;
import com.n1249874.slipstack.databinding.FragmentTrendsBinding;
import com.n1249874.slipstack.models.PurchaseHistoryItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrendsFragment extends Fragment {

    private FragmentTrendsBinding binding;
    private PurchaseHistoryAdapter historyAdapter;
    private ReceiptRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentTrendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ReceiptRepository(requireActivity().getApplication());
        binding.ivProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileActivity.class)));

        historyAdapter = new PurchaseHistoryAdapter();
        binding.rvPurchaseHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPurchaseHistory.setAdapter(historyAdapter);

        binding.etProductSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    showEmptyState();
                } else {
                    searchTrends(query);
                }
            }
        });

        showEmptyState();
    }

    private void searchTrends(String query) {
        String cleanQuery = query.toLowerCase().trim();
        repository.searchTrends(cleanQuery, requireActivity().getApplication()).observe(getViewLifecycleOwner(),
                results -> {
                    if (results == null || results.isEmpty()) {

                        String secondaryQuery = cleanQuery;
                        if (cleanQuery.endsWith("ies") && cleanQuery.length() > 3) {
                            secondaryQuery = cleanQuery.substring(0, cleanQuery.length() - 3) + "y";
                        } else if (cleanQuery.endsWith("s") && cleanQuery.length() > 2) {
                            secondaryQuery = cleanQuery.substring(0, cleanQuery.length() - 1);
                        } else {
                            secondaryQuery = cleanQuery + "s";
                        }

                        if (!secondaryQuery.equals(cleanQuery)) {
                            repository.searchTrends(secondaryQuery, requireActivity().getApplication())
                                    .observe(getViewLifecycleOwner(), secondaryResults -> {
                                        if (secondaryResults == null || secondaryResults.isEmpty()) {
                                            showEmptyStateWithQuery(query);
                                        } else {
                                            updateTrendsData(query, secondaryResults);
                                        }
                                    });
                        } else {
                            showEmptyStateWithQuery(query);
                        }
                    } else {
                        updateTrendsData(query, results);
                    }
                });
    }

    private void updateTrendsData(String query, List<LineItemDao.ItemTrendDataResult> results) {
        // Build history list
        List<PurchaseHistoryItem> historyItems = new ArrayList<>();
        List<Double> last5Prices = new ArrayList<>();

        for (int i = 0; i < results.size(); i++) {
            LineItemDao.ItemTrendDataResult r = results.get(i);
            historyItems.add(new PurchaseHistoryItem(r.date, r.merchantName, r.price));
            if (i < 5) {
                last5Prices.add(r.price);
            }
        }

        // Calculate Median of last 5
        double medianValue = calculateMedian(last5Prices);

        binding.tvProductName.setVisibility(View.VISIBLE);
        binding.tvProductName.setText(capitalize(query));
        binding.tvTypicalPrice.setText(String.format("£%.2f", medianValue));
        binding.tvPriceContext.setText("Typical price (median of last 5)");

        if (!historyItems.isEmpty()) {
            binding.tvDateStart.setText(historyItems.get(historyItems.size() - 1).getDate());
            binding.tvDateEnd.setText(historyItems.get(0).getDate());
        }

        drawBarChart(historyItems);
        historyAdapter.setItems(historyItems);
    }

    private double calculateMedian(List<Double> prices) {
        if (prices.isEmpty())
            return 0;
        List<Double> sorted = new ArrayList<>(prices);
        Collections.sort(sorted);
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2;
        } else {
            return sorted.get(size / 2);
        }
    }

    private void showEmptyState() {
        binding.tvProductName.setVisibility(View.GONE);
        binding.tvTypicalPrice.setText("£0.00");
        binding.tvPriceContext.setText("Search for a product Eg: Milk");
        binding.tvDateStart.setText("--");
        binding.tvDateEnd.setText("--");
        binding.barChartContainer.removeAllViews();
        historyAdapter.setItems(new ArrayList<>());
    }

    private void showEmptyStateWithQuery(String query) {
        binding.tvProductName.setVisibility(View.VISIBLE);
        binding.tvProductName.setText(capitalize(query));
        binding.tvTypicalPrice.setText("N/A");
        binding.tvPriceContext.setText("No purchase data for \"" + query + "\"");
        binding.barChartContainer.removeAllViews();
        historyAdapter.setItems(new ArrayList<>());
    }

    private void drawBarChart(List<PurchaseHistoryItem> history) {
        binding.barChartContainer.removeAllViews();
        List<PurchaseHistoryItem> last10 = history.subList(0, Math.min(10, history.size()));
        Collections.reverse(last10); // Show chronologically left-to-right

        double maxPrice = 0;
        for (PurchaseHistoryItem item : last10) {
            if (item.getPrice() > maxPrice)
                maxPrice = item.getPrice();
        }

        int tealColor = Color.parseColor("#00ACC1");
        for (PurchaseHistoryItem item : last10) {
            float ratio = maxPrice > 0 ? (float) (item.getPrice() / maxPrice) : 0;
            int barHeightPx = (int) (ratio * 160); // 80dp in px roughly

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, barHeightPx, 1f);
            params.setMargins(6, 0, 6, 0);

            View bar = new View(getContext());
            bar.setBackgroundColor(tealColor);
            bar.setLayoutParams(params);
            binding.barChartContainer.addView(bar);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
