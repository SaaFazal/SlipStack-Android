package com.n1249874.slipstack.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n1249874.slipstack.databinding.ItemReceiptBinding;
import com.n1249874.slipstack.models.Receipt;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder> {

    public interface OnDeleteListener {
        void onDelete(Receipt receipt, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(Receipt receipt);
    }

    private List<Receipt> receipts = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.UK);
    private OnDeleteListener deleteListener;
    private OnItemClickListener clickListener;

    public void setReceipts(List<Receipt> receipts) {
        this.receipts = receipts != null ? new ArrayList<>(receipts) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    /** Returns the receipt at a given position — used by swipe-to-delete */
    public Receipt getItemAt(int position) {
        return receipts.get(position);
    }

    /** Remove an item locally (called after DB delete confirms) */
    public void removeAt(int position) {
        receipts.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReceiptBinding binding = ItemReceiptBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ReceiptViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptViewHolder holder, int position) {
        holder.bind(receipts.get(position));
    }

    @Override
    public int getItemCount() {
        return receipts.size();
    }

    class ReceiptViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceiptBinding binding;

        public ReceiptViewHolder(ItemReceiptBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Receipt receipt) {
            binding.tvMerchantName.setText(receipt.getMerchantName());
            binding.tvDate.setText(receipt.getDate());
            binding.tvAmount.setText(currencyFormat.format(receipt.getAmount()));

            itemView.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onItemClick(receipt);
            });
        }
    }
}
