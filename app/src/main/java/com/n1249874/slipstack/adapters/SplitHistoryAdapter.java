package com.n1249874.slipstack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.n1249874.slipstack.R;
import com.n1249874.slipstack.database.SplitHistoryEntity;

import java.util.ArrayList;
import java.util.List;

public class SplitHistoryAdapter extends RecyclerView.Adapter<SplitHistoryAdapter.ViewHolder> {
    private List<SplitHistoryEntity> items = new ArrayList<>();
    private OnDeleteListener deleteListener;

    public interface OnDeleteListener {
        void onDelete(SplitHistoryEntity item);
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setItems(List<SplitHistoryEntity> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_split_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SplitHistoryEntity item = items.get(position);
        holder.tvMerchant.setText(item.receiptMerchant);
        holder.tvInfo.setText(String.format("£%.2f split with %d people", item.totalAmount, item.peopleCount));
        holder.tvPerPerson.setText(String.format("£%.2f ea", item.amountPerPerson));

        if (item.peopleNames != null && !item.peopleNames.isEmpty()) {
            holder.tvPeopleList.setText("Names: " + item.peopleNames);
        } else {
            holder.tvPeopleList.setText("Details: (Legacy record - names not recorded)");
        }

        holder.itemView.setOnClickListener(v -> {
            boolean isVisible = holder.llDetails.getVisibility() == View.VISIBLE;
            holder.llDetails.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMerchant, tvInfo, tvPerPerson, tvPeopleList;
        View llDetails, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMerchant = itemView.findViewById(R.id.tv_history_merchant);
            tvInfo = itemView.findViewById(R.id.tv_history_info);
            tvPerPerson = itemView.findViewById(R.id.tv_history_per_person);
            tvPeopleList = itemView.findViewById(R.id.tv_history_people_list);
            llDetails = itemView.findViewById(R.id.ll_history_details);
            btnDelete = itemView.findViewById(R.id.btn_delete_history);
        }
    }
}
