package com.project.inet_mobile;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PaketAdapter extends RecyclerView.Adapter<PaketAdapter.PaketViewHolder> {

    private List<Paket> paketList;
    private OnPaketClickListener listener;

    // Interface untuk klik item
    public interface OnPaketClickListener {
        void onPaketClick(Paket paket);
    }

    public PaketAdapter(List<Paket> paketList, OnPaketClickListener listener) {
        this.paketList = paketList;
        this.listener = listener;
    }

    public void updateData(List<Paket> newData) {
        this.paketList = newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PaketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paket, parent, false);
        return new PaketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PaketViewHolder holder, int position) {
        Paket currentItem = paketList.get(position);
        holder.bind(currentItem, position);
    }

    @Override
    public int getItemCount() {
        return paketList.size();
    }

    // Method untuk set gradient background: PUTIH ke BIRU
    private void setGradientBackground(ConstraintLayout layout) {
        // Warna: Putih (#FFFFFF) ke Biru lembut (#BBDEFB atau #2196F3 tergantung selera)
        // Gunakan format ARGB: 0xAARRGGBB
        int startColor = 0xFFFFFFFF; // Putih transparan penuh
        int endColor = 0xFFBBDEFB;   // Biru lembut (seperti light blue 100 di Material)

        // Alternatif biru lebih jelas: 0xFF2196F3 (Material Blue 500)

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{startColor, endColor}
        );
        gradient.setCornerRadius(48f);
        layout.setBackground(gradient);
    }

    // ViewHolder
    public class PaketViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama;
        TextView tvDetail;
        TextView tvHarga;
        TextView tvDuration;
        MaterialButton btnBeli;
        ConstraintLayout cardBackground;

        public PaketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tvItemPaketName);
            tvDetail = itemView.findViewById(R.id.tvItemPaketDetail);
            tvHarga = itemView.findViewById(R.id.tvItemPaketPrice);
            tvDuration = itemView.findViewById(R.id.tvItemPaketDuration);
            btnBeli = itemView.findViewById(R.id.btnBeli);
            cardBackground = itemView.findViewById(R.id.cardBackground);

            // Click listener untuk card
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onPaketClick(paketList.get(position));
                }
            });

            // Click listener untuk button Beli
            btnBeli.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onPaketClick(paketList.get(position));
                }
            });
        }

        public void bind(Paket paket, int position) {
            tvNama.setText(paket.getNama());
            tvDetail.setText(paket.getDetail());
            tvHarga.setText(paket.getHarga());

            if (paket.getDuration() != null && !paket.getDuration().isEmpty()) {
                tvDuration.setText(paket.getDuration());
                tvDuration.setVisibility(View.VISIBLE);
            } else {
                tvDuration.setVisibility(View.GONE);
            }

            // Set gradient background (tanpa posisi)
            setGradientBackground(cardBackground);
        }
    }
}