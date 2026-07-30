package com.n1249874.slipstack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n1249874.slipstack.R;
import com.n1249874.slipstack.models.LineItem;

import java.util.ArrayList;
import java.util.List;

public class LineItemAdapter extends RecyclerView.Adapter<LineItemAdapter.ViewHolder> {

    private List<LineItem> items = new ArrayList<>();
    private OnItemInteractionListener listener;

    public interface OnItemInteractionListener {
        void onDelete(int position);
        void onEdit(int position);
    }

    public void setListener(OnItemInteractionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<LineItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<LineItem> getItems() {
        return items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_line_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LineItem item = items.get(position);
        holder.tvName.setText(item.name);
        
        if (item.price < 0) {
            holder.tvPrice.setText(String.format("-£%.2f", Math.abs(item.price)));
            holder.tvPrice.setTextColor(0xFFE53935);
        } else {
            holder.tvPrice.setText(String.format("£%.2f", item.price));
            holder.tvPrice.setTextColor(holder.tvPrice.getContext().getColor(R.color.primary_teal));
        }

        holder.btnDelete.setVisibility(listener != null ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvSuggested;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvPrice = itemView.findViewById(R.id.tv_item_price);
            tvSuggested = itemView.findViewById(R.id.tv_suggested_tag);
            btnDelete = itemView.findViewById(R.id.btn_delete_item);
        }
    }
}
