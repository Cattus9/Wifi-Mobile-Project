package com.project.inet_mobile.ui.ticket;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.project.inet_mobile.data.remote.dto.ticket.TicketMessageDto;
import com.project.inet_mobile.databinding.ListItemMessageReceivedBinding;
import com.project.inet_mobile.databinding.ListItemMessageSentBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TicketMessageAdapter extends ListAdapter<TicketMessageDto, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    public TicketMessageAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public int getItemViewType(int position) {
        TicketMessageDto message = getItem(position);
        // "customer" is used for messages sent by the mobile user
        if ("customer".equalsIgnoreCase(message.getTipePenulis())) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            ListItemMessageSentBinding binding = ListItemMessageSentBinding.inflate(inflater, parent, false);
            return new SentMessageViewHolder(binding);
        } else { // VIEW_TYPE_RECEIVED
            ListItemMessageReceivedBinding binding = ListItemMessageReceivedBinding.inflate(inflater, parent, false);
            return new ReceivedMessageViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TicketMessageDto message = getItem(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(message);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    // --- VIEW HOLDERS ---

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ListItemMessageSentBinding binding;

        SentMessageViewHolder(ListItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TicketMessageDto message) {
            binding.textViewMessageBody.setText(message.getIsi());
            binding.textViewMessageTime.setText(formatTime(message.getDibuatPada()));
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ListItemMessageReceivedBinding binding;

        ReceivedMessageViewHolder(ListItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(TicketMessageDto message) {
            binding.textViewMessageBody.setText(message.getIsi());
            binding.textViewMessageTime.setText(formatTime(message.getDibuatPada()));
            // In a real app, you might get the admin's name from `penulis_id`
            binding.textViewSenderName.setText("Admin"); 
        }
    }
    
    private static String formatTime(String isoString) {
        if (isoString == null) return "";
        try {
            // Handles format like "2025-12-08T10:30:00.123456+00:00"
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", Locale.getDefault());
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inputFormat.parse(isoString);
            
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
            return date != null ? outputFormat.format(date) : "";
        } catch (ParseException e) {
             try {
                // Fallback for format like "2025-12-08T10:30:00+00:00"
                SimpleDateFormat fallbackFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
                fallbackFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = fallbackFormat.parse(isoString);
                SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
                return date != null ? outputFormat.format(date) : "";
            } catch (ParseException e2) {
                 return ""; // Return empty if all parsing fails
            }
        }
    }

    // --- DIFFUTIL ---

    private static final DiffUtil.ItemCallback<TicketMessageDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<TicketMessageDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull TicketMessageDto oldItem, @NonNull TicketMessageDto newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull TicketMessageDto oldItem, @NonNull TicketMessageDto newItem) {
            // Also check for content equality
            return oldItem.getIsi().equals(newItem.getIsi());
        }
    };
}
