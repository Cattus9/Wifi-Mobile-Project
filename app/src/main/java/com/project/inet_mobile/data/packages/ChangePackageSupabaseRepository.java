package com.project.inet_mobile.data.packages;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.remote.SupabaseChangePackageService;
import com.project.inet_mobile.data.remote.dto.ChangePackageRequest;
import com.project.inet_mobile.data.remote.dto.SupabaseChangePackageResponse;
import com.project.inet_mobile.data.session.TokenStorage;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository implementation using Supabase Edge Functions
 * untuk fitur Change Package.
 *
 * Flow:
 * Mobile App → Edge Function → RPC Function → Database
 *
 * Endpoint: POST /functions/v1/change-package
 */
public class ChangePackageSupabaseRepository {

    private static final String TAG = "ChangePackageSupabase";
    private final SupabaseChangePackageService service;
    private final TokenStorage tokenStorage;

    public interface SubmitCallback {
        void onSuccess(String message, long ticketId);
        void onError(String message);
    }

    public ChangePackageSupabaseRepository(@NonNull Context context) {
        SupabaseApiClient.init(context);
        this.service = SupabaseApiClient.getSupabaseChangePackageService();
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    /**
     * Submit change package request via Supabase Edge Function
     * Authorization is handled automatically by AuthInterceptor
     *
     * @param targetPackageId Package ID yang ingin diubah
     * @param note Catatan dari customer (optional)
     * @param callback Callback untuk handle success/error
     */
    public void submitChangePackage(long targetPackageId, String note, SubmitCallback callback) {
        // Verify user is logged in (session check)
        AuthSession session = tokenStorage.getSession();

        // DEBUG: Detailed session logging
        if (session == null) {
            Log.e(TAG, "submitChangePackage: session is NULL");
            callback.onError("Sesi telah berakhir. Silakan login kembali.");
            return;
        }

        Log.d(TAG, "=== SESSION DEBUG ===");
        Log.d(TAG, "Session exists: true");
        Log.d(TAG, "Session expired: " + session.isExpired());
        Log.d(TAG, "Expires at: " + session.getExpiresAtMillis());
        Log.d(TAG, "Current time: " + System.currentTimeMillis());
        Log.d(TAG, "Access token exists: " + (session.getAccessToken() != null));
        Log.d(TAG, "Access token length: " + (session.getAccessToken() != null ? session.getAccessToken().length() : 0));
        Log.d(TAG, "Auth user ID: " + session.getAuthUserId());
        Log.d(TAG, "=== END SESSION DEBUG ===");

        if (session.isExpired() || session.getAccessToken() == null) {
            Log.e(TAG, "submitChangePackage: session expired or no access token");
            callback.onError("Sesi telah berakhir. Silakan login kembali.");
            return;
        }

        ChangePackageRequest body = new ChangePackageRequest(targetPackageId, note);
        long start = System.currentTimeMillis();

        Log.d(TAG, "Submitting change package request to Supabase Edge Function");
        Log.d(TAG, "Package ID: " + targetPackageId + ", Notes: " + note);

        // AuthInterceptor will automatically add Authorization header
        service.submitChangePackage(body).enqueue(new Callback<SupabaseChangePackageResponse>() {
            @Override
            public void onResponse(Call<SupabaseChangePackageResponse> call, Response<SupabaseChangePackageResponse> response) {
                long elapsed = System.currentTimeMillis() - start;

                if (response.isSuccessful() && response.body() != null) {
                    SupabaseChangePackageResponse supabaseResponse = response.body();

                    // Check success field dari Edge Function
                    if (supabaseResponse.isSuccess() && supabaseResponse.getData() != null) {
                        // Success case
                        SupabaseChangePackageResponse.ChangePackageData data = supabaseResponse.getData();
                        String message = data.getMessage();
                        long ticketId = data.getTicketId();

                        Log.d(TAG, "Submit change-package SUCCESS latencyMs=" + elapsed);
                        Log.d(TAG, "Ticket ID: " + ticketId);
                        Log.d(TAG, "Message: " + message);

                        callback.onSuccess(message, ticketId);
                    } else {
                        // Edge Function returned success=false (business validation error)
                        String errorMsg = parseErrorMessage(supabaseResponse.getMessage());
                        Log.e(TAG, "Business validation error: " + errorMsg + " latencyMs=" + elapsed);
                        callback.onError(errorMsg);
                    }
                } else {
                    // HTTP error
                    String errorBody = "HTTP " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }

                    String errorMsg = parseErrorMessage(errorBody);
                    Log.e(TAG, "HTTP error: " + response.code() + " - " + errorBody + " latencyMs=" + elapsed);
                    callback.onError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<SupabaseChangePackageResponse> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                String msg = "Jaringan bermasalah: " + t.getMessage();
                Log.e(TAG, "Network failure: " + msg + " latencyMs=" + elapsed);
                callback.onError(msg);
            }
        });
    }

    /**
     * Parse error message dari Supabase Edge Function
     * Format: "ERROR_CODE: Human message"
     */
    private String parseErrorMessage(String rawError) {
        if (rawError == null || rawError.isEmpty()) {
            return "Terjadi kesalahan. Silakan coba lagi.";
        }

        // Check for known error codes
        if (rawError.contains("OUTSTANDING_INVOICE")) {
            return "Harap selesaikan tagihan tertunggak terlebih dahulu";
        } else if (rawError.contains("PENDING_REQUEST")) {
            return "Masih ada permintaan aktif yang sedang diproses";
        } else if (rawError.contains("PACKAGE_SAME_AS_CURRENT")) {
            return "Paket yang dipilih sama dengan paket aktif saat ini";
        } else if (rawError.contains("PACKAGE_NOT_AVAILABLE")) {
            return "Paket tidak tersedia";
        } else if (rawError.contains("CUSTOMER_NOT_FOUND")) {
            return "Data customer tidak ditemukan";
        } else if (rawError.contains("UNAUTHORIZED") || rawError.contains("401")) {
            return "Sesi telah berakhir. Silakan login kembali.";
        }

        // Extract message after colon if exists
        String[] parts = rawError.split(":");
        if (parts.length > 1) {
            return parts[1].trim();
        }

        return rawError;
    }
}
