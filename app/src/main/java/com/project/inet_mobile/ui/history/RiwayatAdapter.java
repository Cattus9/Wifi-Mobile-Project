package com.project.inet_mobile.ui.history;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.inet_mobile.R;

public class RiwayatAdapter extends ListAdapter<PaymentHistoryItem, RiwayatAdapter.RiwayatViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(PaymentHistoryItem item);
    }

    private final OnItemClickListener listener;

    public RiwayatAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<PaymentHistoryItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PaymentHistoryItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull PaymentHistoryItem oldItem, @NonNull PaymentHistoryItem newItem) {
                    return oldItem.getInvoiceNumber().equals(newItem.getInvoiceNumber());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PaymentHistoryItem oldItem, @NonNull PaymentHistoryItem newItem) {
                    return oldItem.getMetaDescription().equals(newItem.getMetaDescription())
                            && oldItem.getAmountFormatted().equals(newItem.getAmountFormatted())
                            && oldItem.getStatusLabel().equals(newItem.getStatusLabel());
                }
            };

    @NonNull
    @Override
    public RiwayatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_riwayat_pembayaran, parent, false);
        return new RiwayatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RiwayatViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RiwayatViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvPaymentTitle;
        private final TextView tvPaymentMeta;
        private final TextView tvPaymentAmount;
        private final TextView tvPaymentStatus;

        RiwayatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPaymentTitle = itemView.findViewById(R.id.tvPaymentTitle);
            tvPaymentMeta = itemView.findViewById(R.id.tvPaymentMeta);
            tvPaymentAmount = itemView.findViewById(R.id.tvPaymentAmount);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);

            itemView.setOnClickListener(v -> {
                int pos = getAbsoluteAdapterPosition();
                if (listener == null || pos == RecyclerView.NO_POSITION) {
                    return;
                }
                PaymentHistoryItem item = getItem(pos);
                if (item != null) {
                    listener.onItemClick(item);
                }
            });
        }

        void bind(PaymentHistoryItem item) {
            tvPaymentTitle.setText("Pembayaran " + item.getMonthLabel());
            tvPaymentMeta.setText(item.getMetaDescription());
            tvPaymentAmount.setText(item.getAmountFormatted());
            tvPaymentStatus.setText(item.getStatusLabel());
            tvPaymentStatus.setTextColor(item.getStatusTextColor());

            Drawable background = DrawableCompat.wrap(tvPaymentStatus.getBackground().mutate());
            DrawableCompat.setTint(background, item.getStatusBackgroundColor());
            tvPaymentStatus.setBackground(background);
        }
    }
}
