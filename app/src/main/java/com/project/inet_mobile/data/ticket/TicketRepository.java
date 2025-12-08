package com.project.inet_mobile.data.ticket;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.remote.SupabaseTicketService;
import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketMessageRequest;
import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketRequest;
import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketResponse;
import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;
import com.project.inet_mobile.data.remote.dto.ticket.TicketMessageDto;
import com.project.inet_mobile.data.session.TokenStorage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import retrofit2.Response;

// Pure Java version of the repository
public class TicketRepository {

    private static final String TAG = "TicketRepository";
    private SupabaseTicketService ticketService; // To be initialized later
    private final TokenStorage tokenStorage;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TicketRepository(Context context) {
        // Service will be initialized after SupabaseApiClient is ready.
        // We defer initialization to the methods themselves to be safe.
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    private SupabaseTicketService getTicketService(Context context) {
        if (ticketService == null) {
            SupabaseApiClient.init(context); // Ensure client is initialized
            this.ticketService = SupabaseApiClient.getSupabaseTicketService();
        }
        return ticketService;
    }

    // --- CALLBACK INTERFACES ---

    public interface GetTicketsCallback {
        void onSuccess(List<TicketDto> tickets);
        void onError(String message);
    }

    public interface GetMessagesCallback {
        void onSuccess(List<TicketMessageDto> messages);
        void onError(String message);
    }

    public interface GetTicketCallback {
        void onSuccess(TicketDto ticket);
        void onError(String message);
    }

    public interface CreateTicketCallback {
        void onSuccess(CreateTicketResponse response);
        void onError(String message);
    }

    public interface CreateMessageCallback {
        void onSuccess(TicketMessageDto newMessage);
        void onError(String message);
    }


    // --- PUBLIC METHODS ---

    public void getTickets(Context context, @Nullable String statusFilter, GetTicketsCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isUserLoggedIn(callback)) return;

                Response<List<TicketDto>> response = getTicketService(context)
                        .getTickets("*", "created_at.desc", statusFilter)
                        .execute();

                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else {
                    handleError(response, "Gagal memuat daftar tiket", callback);
                }
            } catch (IOException e) {
                handleException("Network error: " + e.getMessage(), callback);
            }
        });
    }

    public void getTicketById(Context context, long ticketId, GetTicketCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isUserLoggedIn(callback)) return;
                String ticketIdFilter = "eq." + ticketId;

                Response<List<TicketDto>> response = getTicketService(context)
                        .getTicketById(ticketIdFilter, "*")
                        .execute();

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    mainHandler.post(() -> callback.onSuccess(response.body().get(0)));
                } else {
                    if (response.body() != null && response.body().isEmpty()) {
                        handleException("Ticket tidak ditemukan.", callback);
                    } else {
                        handleError(response, "Gagal memuat detail tiket", callback);
                    }
                }
            } catch (IOException e) {
                handleException("Network error: " + e.getMessage(), callback);
            }
        });
    }

    public void getTicketMessages(Context context, long ticketId, GetMessagesCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isUserLoggedIn(callback)) return;
                String ticketIdFilter = "eq." + ticketId;

                Response<List<TicketMessageDto>> response = getTicketService(context)
                        .getTicketMessages(ticketIdFilter, "*", "dibuat_pada.asc")
                        .execute();

                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else {
                    handleError(response, "Gagal memuat pesan tiket", callback);
                }
            } catch (IOException e) {
                handleException("Network error: " + e.getMessage(), callback);
            }
        });
    }

    public void createTicket(Context context, String kategori, String subject, String description, CreateTicketCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isUserLoggedIn(callback)) return;

                CreateTicketRequest requestBody = new CreateTicketRequest(kategori, subject, description);
                Response<CreateTicketResponse> response = getTicketService(context).createTicket(requestBody).execute();

                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else {
                    handleError(response, "Gagal membuat tiket baru", callback);
                }
            } catch (IOException e) {
                handleException("Network error: " + e.getMessage(), callback);
            }
        });
    }

    public void createTicketMessage(Context context, long ticketId, String isi, CreateMessageCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isUserLoggedIn(callback)) return;

                CreateTicketMessageRequest requestBody = new CreateTicketMessageRequest(ticketId, isi);
                Response<TicketMessageDto> response = getTicketService(context).createTicketMessage(requestBody).execute();

                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else {
                    handleError(response, "Gagal mengirim pesan", callback);
                }
            } catch (IOException e) {
                handleException("Network error: " + e.getMessage(), callback);
            }
        });
    }

    // --- HELPER METHODS ---

    private boolean isUserLoggedIn(Object callback) {
        if (tokenStorage.getSession() == null) {
            handleException("User not logged in.", callback);
            return false;
        }
        return true;
    }

    private void handleError(Response<?> response, String defaultMessage, Object callback) {
        String errorMsg = defaultMessage;
        try {
            if (response.errorBody() != null) {
                errorMsg = defaultMessage + ": " + response.code() + " " + response.errorBody().string();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading error body", e);
        }
        handleException(errorMsg, callback);
    }

    private void handleException(String message, Object callback) {
        Log.e(TAG, message);
        if (callback instanceof GetTicketsCallback) {
            mainHandler.post(() -> ((GetTicketsCallback) callback).onError(message));
        } else if (callback instanceof GetTicketCallback) {
            mainHandler.post(() -> ((GetTicketCallback) callback).onError(message));
        } else if (callback instanceof GetMessagesCallback) {
            mainHandler.post(() -> ((GetMessagesCallback) callback).onError(message));
        } else if (callback instanceof CreateTicketCallback) {
            mainHandler.post(() -> ((CreateTicketCallback) callback).onError(message));
        } else if (callback instanceof CreateMessageCallback) {
            mainHandler.post(() -> ((CreateMessageCallback) callback).onError(message));
        }
    }
    
    public void shutdown() {
        executorService.shutdownNow();
    }
}
