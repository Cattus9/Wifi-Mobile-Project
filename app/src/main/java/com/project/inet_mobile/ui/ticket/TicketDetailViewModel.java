package com.project.inet_mobile.ui.ticket;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;
import com.project.inet_mobile.data.remote.dto.ticket.TicketMessageDto;
import com.project.inet_mobile.data.ticket.TicketRepository;

import java.util.List;
import android.os.Handler;
import android.os.Looper;

public class TicketDetailViewModel extends AndroidViewModel {

    private final TicketRepository ticketRepository;

    // Polling mechanism
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private Runnable pollingRunnable;
    private static final long POLLING_INTERVAL_MS = 5000; // 5 seconds

    // --- State for Message List ---
    public enum ListStatus { LOADING, SUCCESS, ERROR, EMPTY }
    public static class MessagesState {
        public final ListStatus status;
        public final List<TicketMessageDto> messages;
        public final String errorMessage;

        private MessagesState(ListStatus status, List<TicketMessageDto> messages, String errorMessage) {
            this.status = status;
            this.messages = messages;
            this.errorMessage = errorMessage;
        }
        public static MessagesState loading() { return new MessagesState(ListStatus.LOADING, null, null); }
        public static MessagesState success(List<TicketMessageDto> messages) { return new MessagesState(ListStatus.SUCCESS, messages, null); }
        public static MessagesState error(String message) { return new MessagesState(ListStatus.ERROR, null, message); }
        public static MessagesState empty() { return new MessagesState(ListStatus.EMPTY, null, null); }
    }
    private final MutableLiveData<MessagesState> _messagesState = new MutableLiveData<>();
    public final LiveData<MessagesState> messagesState = _messagesState;

    // --- State for Ticket Details (for the header) ---
    private final MutableLiveData<TicketDto> _ticketDetails = new MutableLiveData<>();
    public final LiveData<TicketDto> ticketDetails = _ticketDetails;

    // --- State for Sending a New Message ---
    public enum SendingStatus { IDLE, SENDING, SUCCESS, ERROR }
    public static class SendingState {
        public final SendingStatus status;
        public final String errorMessage;

        private SendingState(SendingStatus status, String errorMessage) {
            this.status = status;
            this.errorMessage = errorMessage;
        }
        public static SendingState idle() { return new SendingState(SendingStatus.IDLE, null); }
        public static SendingState sending() { return new SendingState(SendingStatus.SENDING, null); }
        public static SendingState success() { return new SendingState(SendingStatus.SUCCESS, null); }
        public static SendingState error(String message) { return new SendingState(SendingStatus.ERROR, message); }
    }
    private final MutableLiveData<SendingState> _sendingState = new MutableLiveData<>(SendingState.idle());
    public final LiveData<SendingState> sendingState = _sendingState;

    public TicketDetailViewModel(@NonNull Application application) {
        super(application);
        this.ticketRepository = new TicketRepository(application.getApplicationContext());
        
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                // Ensure ticketId is available before polling
                if (_ticketDetails.getValue() != null) {
                    fetchTicketMessages(_ticketDetails.getValue().getId(), true); // Pass true for isPolling
                }
                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        };
    }

    public void fetchTicketDetails(long ticketId) {
        ticketRepository.getTicketById(getApplication().getApplicationContext(), ticketId, new TicketRepository.GetTicketCallback() {
            @Override
            public void onSuccess(TicketDto ticket) {
                _ticketDetails.setValue(ticket);
            }

            @Override
            public void onError(String message) {
                // Also post an error to the main message list state to show something went wrong
                _messagesState.setValue(MessagesState.error("Gagal memuat detail tiket: " + message));
            }
        });
    }

    public void fetchTicketMessages(long ticketId) {
        fetchTicketMessages(ticketId, false); // Default to not polling
    }

    public void fetchTicketMessages(long ticketId, boolean isPolling) {
        if (!isPolling) {
            _messagesState.setValue(MessagesState.loading());
        }
        ticketRepository.getTicketMessages(getApplication().getApplicationContext(), ticketId, new TicketRepository.GetMessagesCallback() {
            @Override
            public void onSuccess(List<TicketMessageDto> messages) {
                if (messages == null || messages.isEmpty()) {
                    _messagesState.setValue(MessagesState.empty());
                } else {
                    _messagesState.setValue(MessagesState.success(messages));
                }
            }

            @Override
            public void onError(String message) {
                _messagesState.setValue(MessagesState.error(message));
            }
        });
    }

    public void sendNewMessage(long ticketId, String message) {
        if (message == null || message.trim().isEmpty()) {
            _sendingState.setValue(SendingState.error("Pesan tidak boleh kosong."));
            return;
        }
        _sendingState.setValue(SendingState.sending());

        ticketRepository.createTicketMessage(getApplication().getApplicationContext(), ticketId, message, new TicketRepository.CreateMessageCallback() {
            @Override
            public void onSuccess(TicketMessageDto newMessage) {
                _sendingState.setValue(SendingState.success());
                // After successfully sending, refresh the message list
                fetchTicketMessages(ticketId);
            }

            @Override
            public void onError(String message) {
                _sendingState.setValue(SendingState.error(message));
            }
        });
    }

    public void resetSendingState() {
        _sendingState.setValue(SendingState.idle());
    }

    public void startPolling() {
        pollingHandler.post(pollingRunnable);
    }

    public void stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopPolling(); // Ensure polling stops when ViewModel is destroyed
        ticketRepository.shutdown();
    }
}
