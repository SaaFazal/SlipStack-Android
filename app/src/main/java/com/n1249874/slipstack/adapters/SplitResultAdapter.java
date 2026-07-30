package com.n1249874.slipstack.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n1249874.slipstack.databinding.ItemSplitResultBinding;

import java.util.ArrayList;
import java.util.List;

public class SplitResultAdapter extends RecyclerView.Adapter<SplitResultAdapter.ViewHolder> {

    private List<SplitEntry> entries = new ArrayList<>();

    public void setEntries(List<SplitEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSplitResultBinding binding = ItemSplitResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SplitEntry entry = entries.get(position);
        holder.binding.tvPersonName.setText(entry.name);
        holder.binding.tvPersonAmount.setText(String.format("£%.2f", entry.amount));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemSplitResultBinding binding;

        ViewHolder(ItemSplitResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public static class SplitEntry {
        public String name;
        public double amount;

        public SplitEntry(String name, double amount) {
            this.name = name;
            this.amount = amount;
        }
    }
}
