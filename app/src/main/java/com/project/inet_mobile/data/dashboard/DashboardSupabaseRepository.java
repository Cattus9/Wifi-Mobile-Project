package com.project.inet_mobile.data.dashboard;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.remote.SupabaseDashboardService;
import com.project.inet_mobile.data.remote.dto.SupabaseDashboardInvoiceDto;
import com.project.inet_mobile.data.remote.dto.SupabaseDashboardUserDto;
import com.project.inet_mobile.data.session.TokenStorage;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for fetching dashboard data from Supabase
 */
public class DashboardSupabaseRepository {

    private static final String TAG = "DashboardSupaRepo";
    private final SupabaseDashboardService service;
    private final TokenStorage tokenStorage;

    public DashboardSupabaseRepository(Context context) {
        this.service = SupabaseApiClient.getSupabaseDashboardService();
        this.tokenStorage = new TokenStorage(context);
    }

    /**
     * Fetch complete dashboard data (user, package, invoice)
     */
    public void fetchDashboardData(@NonNull DashboardCallback callback) {
        // Get auth user ID from session
        com.project.inet_mobile.data.auth.AuthSession session = tokenStorage.getSession();
        if (session == null || session.getAuthUserId() == null || session.getAuthUserId().isEmpty()) {
            Log.e(TAG, "fetchDashboardData: session or authUserId is null");
            callback.onError("Sesi telah berakhir. Silakan login kembali.");
            return;
        }

        String authUserId = session.getAuthUserId();
        Log.d(TAG, "fetchDashboardData: authUserId = " + authUserId);

        // Step 1: Fetch user data with customer and package
        String select = "customer_id,customers(id,name,status,service_package_id,service_packages(id,name,description,speed,price))";
        service.getUserDashboardData("eq." + authUserId, select).enqueue(new Callback<List<SupabaseDashboardUserDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseDashboardUserDto>> call, @NonNull Response<List<SupabaseDashboardUserDto>> response) {
                if (!response.isSuccessful()) {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Cannot read error body";
                    }
                    Log.e(TAG, "getUserDashboardData: HTTP error " + response.code());
                    Log.e(TAG, "getUserDashboardData: Error body = " + errorBody);
                    Log.e(TAG, "getUserDashboardData: Request URL = " + call.request().url());
                    callback.onError("Gagal memuat data dashboard: HTTP " + response.code());
                    return;
                }

                List<SupabaseDashboardUserDto> users = response.body();
                if (users == null || users.isEmpty()) {
                    Log.e(TAG, "getUserDashboardData: No user found");
                    callback.onError("Data user tidak ditemukan");
                    return;
                }

                SupabaseDashboardUserDto userDto = users.get(0);
                if (userDto.getCustomer() == null) {
                    Log.e(TAG, "getUserDashboardData: Customer data is null");
                    callback.onError("Data customer tidak ditemukan");
                    return;
                }

                Integer customerId = userDto.getCustomer().getId();
                Log.d(TAG, "getUserDashboardData: Success. Customer ID = " + customerId);

                // Step 2: Fetch latest invoice
                fetchLatestInvoice(customerId, userDto, callback);
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseDashboardUserDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "getUserDashboardData: Request failed", t);
                callback.onError("Gagal memuat data: " + t.getMessage());
            }
        });
    }

    /**
     * Fetch latest unpaid invoice for a customer
     */
    private void fetchLatestInvoice(int customerId, SupabaseDashboardUserDto userDto, DashboardCallback callback) {
        String customerIdQuery = "eq." + customerId;
        String statusQuery = "in.(issued,overdue)"; // Valid enum values: draft, issued, overdue, paid, cancelled
        String order = "due_date.desc";

        Log.d(TAG, "fetchLatestInvoice: customerId = " + customerId);
        Log.d(TAG, "fetchLatestInvoice: Query filters - customer_id=" + customerIdQuery + ", status=" + statusQuery);

        service.getLatestInvoice(customerIdQuery, statusQuery, order, 1).enqueue(new Callback<List<SupabaseDashboardInvoiceDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<SupabaseDashboardInvoiceDto>> call, @NonNull Response<List<SupabaseDashboardInvoiceDto>> response) {
                if (!response.isSuccessful()) {
                    String errorBody = "";
                    try {
                        if (response.errorBody() != null) {
                            errorBody = response.errorBody().string();
                        }
                    } catch (Exception e) {
                        errorBody = "Cannot read error body";
                    }
                    Log.e(TAG, "getLatestInvoice: HTTP error " + response.code());
                    Log.e(TAG, "getLatestInvoice: Error body = " + errorBody);
                    Log.e(TAG, "getLatestInvoice: Request URL = " + call.request().url());
                    // Still return user data even if invoice fetch fails
                    callback.onSuccess(userDto, null);
                    return;
                }

                List<SupabaseDashboardInvoiceDto> invoices = response.body();
                Log.d(TAG, "getLatestInvoice: Response body size = " + (invoices != null ? invoices.size() : 0));

                SupabaseDashboardInvoiceDto invoice = (invoices != null && !invoices.isEmpty()) ? invoices.get(0) : null;

                if (invoice != null) {
                    Log.d(TAG, "getLatestInvoice: Found invoice " + invoice.getInvoiceNumber() + " - Amount: " + invoice.getAmount() + ", Due: " + invoice.getDueDate());
                } else {
                    Log.w(TAG, "getLatestInvoice: No unpaid/pending invoice found for customer " + customerId);
                }

                callback.onSuccess(userDto, invoice);
            }

            @Override
            public void onFailure(@NonNull Call<List<SupabaseDashboardInvoiceDto>> call, @NonNull Throwable t) {
                Log.e(TAG, "getLatestInvoice: Request failed", t);
                // Still return user data even if invoice fetch fails
                callback.onSuccess(userDto, null);
            }
        });
    }

    /**
     * Callback interface for dashboard data
     */
    public interface DashboardCallback {
        void onSuccess(@NonNull SupabaseDashboardUserDto userData, @Nullable SupabaseDashboardInvoiceDto invoice);
        void onError(String message);
    }
}
