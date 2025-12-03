package com.project.inet_mobile.ui.account;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.inet_mobile.R;
import com.project.inet_mobile.Paket;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChangePackageAdapter extends RecyclerView.Adapter<ChangePackageAdapter.PackageViewHolder> {

    public interface OnPackageSelectedListener {
        void onPackageSelected(Paket paket);
    }

    private final List<Paket> items = new ArrayList<>();
    private final OnPackageSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;
    private int currentPackageId = -1;

    public ChangePackageAdapter(OnPackageSelectedListener listener) {
        this.listener = listener;
    }

    public void setCurrentPackageId(int currentId) {
        this.currentPackageId = currentId;
    }

    public void setItems(List<Paket> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        selectedPosition = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PackageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_change_package, parent, false);
        return new PackageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PackageViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class PackageViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView desc;
        TextView price;
        RadioButton radio;

        PackageViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textPackageName);
            desc = itemView.findViewById(R.id.textPackageDesc);
            price = itemView.findViewById(R.id.textPackagePrice);
            radio = itemView.findViewById(R.id.radioSelect);
        }

        void bind(Paket paket, int position) {
            name.setText(paket.getNama());
            String detail = paket.getSpeed();
            if (paket.getDescription() != null && !paket.getDescription().isEmpty()) {
                detail = paket.getSpeed() + " • " + paket.getDescription();
            }
            desc.setText(detail);
            price.setText(paket.getHarga());

            boolean disabled = currentPackageId == paket.getId();
            itemView.setAlpha(disabled ? 0.5f : 1f);
            itemView.setEnabled(!disabled);
            radio.setEnabled(!disabled);
            radio.setChecked(selectedPosition == position);

            View.OnClickListener onClick = v -> {
                if (disabled) return;
                int oldPos = selectedPosition;
                selectedPosition = position;
                notifyItemChanged(position);
                if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos);
                if (listener != null) listener.onPackageSelected(paket);
            };
            itemView.setOnClickListener(onClick);
            radio.setOnClickListener(onClick);
        }

        private String formatPrice(double value) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            return nf.format(value).replace(",00", "");
        }
    }
}
