package com.project.inet_mobile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PaketAdapter extends RecyclerView.Adapter<PaketAdapter.PaketViewHolder> {

    private List<Paket> paketList;
    private OnPaketClickListener listener; // Untuk menangani klik pada item

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
        notifyDataSetChanged(); // Beri tahu RecyclerView bahwa data telah berubah
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
        holder.bind(currentItem);
    }

    @Override
    public int getItemCount() {
        return paketList.size();
    }

    // ViewHolder
    public class PaketViewHolder extends RecyclerView.ViewHolder {
        TextView tvNama;
        TextView tvDetail;
        TextView tvHarga;
        TextView tvBadgePopuler;

        public PaketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNama = itemView.findViewById(R.id.tvItemPaketName);
            tvDetail = itemView.findViewById(R.id.tvItemPaketDetail);
            tvHarga = itemView.findViewById(R.id.tvItemPaketPrice);
            tvBadgePopuler = itemView.findViewById(R.id.tvItemBadgePopuler);

            // Tambahkan click listener ke view holder
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onPaketClick(paketList.get(position));
                }
            });
        }

        public void bind(Paket paket) {
            tvNama.setText(paket.getNama());
            tvDetail.setText(paket.getDetail());
            tvHarga.setText(paket.getHarga());

            // Atur visibility badge populer
            if (paket.isPopuler()) {
                tvBadgePopuler.setVisibility(View.VISIBLE);
            } else {
                tvBadgePopuler.setVisibility(View.GONE);
            }
        }
    }
}