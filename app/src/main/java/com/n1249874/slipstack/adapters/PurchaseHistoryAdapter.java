package com.n1249874.slipstack.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n1249874.slipstack.databinding.ItemPurchaseHistoryBinding;
import com.n1249874.slipstack.models.PurchaseHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.ViewHolder> {

    private List<PurchaseHistoryItem> items = new ArrayList<>();

    public void setItems(List<PurchaseHistoryItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPurchaseHistoryBinding binding = ItemPurchaseHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PurchaseHistoryItem item = items.get(position);
        holder.binding.tvHistoryDate.setText(item.getDate());
        holder.binding.tvHistoryStore.setText(item.getStore());
        holder.binding.tvHistoryPrice.setText(String.format("£%.2f", item.getPrice()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemPurchaseHistoryBinding binding;

        ViewHolder(ItemPurchaseHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
