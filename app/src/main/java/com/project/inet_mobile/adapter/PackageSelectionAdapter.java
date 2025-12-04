package com.project.inet_mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.project.inet_mobile.Paket;
import com.project.inet_mobile.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying packages in PackageSelectionActivity
 */
public class PackageSelectionAdapter extends RecyclerView.Adapter<PackageSelectionAdapter.ViewHolder> {

    private List<Paket> packages = new ArrayList<>();
    private OnPackageSelectedListener listener;

    public interface OnPackageSelectedListener {
        void onPackageSelected(Paket paket);
    }

    public PackageSelectionAdapter(OnPackageSelectedListener listener) {
        this.listener = listener;
    }

    public void setPackages(List<Paket> packages) {
        this.packages = packages != null ? packages : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_paket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Paket paket = packages.get(position);
        holder.bind(paket);
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvDetail;
        private TextView tvDuration;
        private TextView tvPrice;
        private MaterialButton btnBeli;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemPaketName);
            tvDetail = itemView.findViewById(R.id.tvItemPaketDetail);
            tvDuration = itemView.findViewById(R.id.tvItemPaketDuration);
            tvPrice = itemView.findViewById(R.id.tvItemPaketPrice);
            btnBeli = itemView.findViewById(R.id.btnBeli);
        }

        void bind(Paket paket) {
            tvName.setText(paket.getName());
            tvDetail.setText(paket.getDescription());

            // Format duration
            String duration = paket.getDuration();
            if (duration != null && !duration.isEmpty()) {
                tvDuration.setText(duration);
                tvDuration.setVisibility(View.VISIBLE);
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            // Format price
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            String formattedPrice = formatter.format(paket.getPrice()).replace("Rp", "Rp ");
            tvPrice.setText(formattedPrice + "/bulan");

            // Button action - changed text to "Pilih"
            btnBeli.setText("Pilih");
            btnBeli.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPackageSelected(paket);
                }
            });
        }
    }
}
