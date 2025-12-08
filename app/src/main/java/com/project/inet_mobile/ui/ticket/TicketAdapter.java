package com.project.inet_mobile.ui.ticket;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.inet_mobile.R; // Assuming R file and colors are available
import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;
import com.project.inet_mobile.databinding.ListItemTicketBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TicketAdapter extends ListAdapter<TicketDto, TicketAdapter.TicketViewHolder> {

    private final OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(TicketDto ticket);
    }

    public TicketAdapter(OnTicketClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<TicketDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<TicketDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull TicketDto oldItem, @NonNull TicketDto newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TicketDto oldItem, @NonNull TicketDto newItem) {
            return oldItem.getStatus().equals(newItem.getStatus()) && oldItem.getSubject().equals(newItem.getSubject());
        }
    };

    @NonNull
    @Override
    public TicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemTicketBinding binding = ListItemTicketBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new TicketViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull TicketViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class TicketViewHolder extends RecyclerView.ViewHolder {
        private final ListItemTicketBinding binding;
        private final OnTicketClickListener listener;

        TicketViewHolder(ListItemTicketBinding binding, OnTicketClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(TicketDto ticket) {
            binding.textViewTicketSubject.setText(ticket.getSubject());
            binding.textViewTicketCategory.setText(formatCategory(ticket.getKategori()));
            binding.textViewTicketDate.setText(formatDate(ticket.getCreatedAt()));
            
            setupStatusChip(binding.chipTicketStatus, ticket.getStatus());

            itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
        }
        
        private String formatCategory(String category) {
            if (category == null) return "Lainnya";
            return category.replace("_", " ").toUpperCase();
        }
        
        private String formatDate(String isoString) {
             if (isoString == null) return "-";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(isoString);

                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
                return "Dibuat pada: " + (date != null ? outputFormat.format(date) : "");
            } catch (ParseException e) {
                // Fallback for ISO 8601 without microseconds
                 try {
                    SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                    fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = fallbackFormat.parse(isoString);
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));
                    return "Dibuat pada: " + (date != null ? outputFormat.format(date) : "");
                } catch (ParseException e2) {
                     return isoString; // Return raw string if all parsing fails
                }
            }
        }
        
        private void setupStatusChip(com.google.android.material.chip.Chip chip, String status) {
            String cleanStatus = status != null ? status.toLowerCase() : "unknown";
            chip.setText(cleanStatus);

            int backgroundColorRes;
            int textColorRes;

            switch (cleanStatus) {
                case "open":
                    backgroundColorRes = R.color.status_open_bg;
                    textColorRes = R.color.status_open_text;
                    break;
                case "in_progress":
                    backgroundColorRes = R.color.status_inprogress_bg;
                    textColorRes = R.color.status_inprogress_text;
                    break;
                case "closed":
                    backgroundColorRes = R.color.status_closed_bg;
                    textColorRes = R.color.status_closed_text;
                    break;
                default:
                    backgroundColorRes = R.color.status_default_bg;
                    textColorRes = R.color.status_default_text;
            }
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(chip.getContext(), backgroundColorRes));
            chip.setTextColor(ContextCompat.getColor(chip.getContext(), textColorRes));
        }
    }
}
