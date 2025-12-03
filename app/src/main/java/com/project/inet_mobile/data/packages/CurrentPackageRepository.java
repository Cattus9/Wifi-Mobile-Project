package com.project.inet_mobile.data.packages;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.session.TokenStorage;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/**
 * Repository untuk get current package ID dari user yang sedang login
 */
public class CurrentPackageRepository {

    private static final String TAG = "CurrentPackageRepo";
    private final CurrentPackageService service;
    private final TokenStorage tokenStorage;

    public interface CurrentPackageCallback {
        void onSuccess(Integer packageId);
        void onError(String message);
    }

    public CurrentPackageRepository(@NonNull Context context) {
        SupabaseApiClient.init(context);
        this.service = SupabaseApiClient.getCurrentPackageService();
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    /**
     * Get current package ID dari user yang sedang login
     */
    public void getCurrentPackageId(@NonNull CurrentPackageCallback callback) {
        AuthSession session = tokenStorage.getSession();

        // DEBUG: Log session state
        if (session == null) {
            Log.e(TAG, "getCurrentPackageId: session is NULL");
            callback.onError("User not logged in.");
            return;
        }

        Log.d(TAG, "Session found - Expired: " + session.isExpired());
        Log.d(TAG, "Session - Access Token exists: " + (session.getAccessToken() != null));
        Log.d(TAG, "Session - Auth User ID: " + session.getAuthUserId());

        if (session.isExpired() || session.getAccessToken() == null) {
            Log.e(TAG, "getCurrentPackageId: session expired or no access token");
            callback.onError("User not logged in.");
            return;
        }

        String authUserId = session.getAuthUserId();

        if (authUserId == null || authUserId.isEmpty()) {
            callback.onError("User ID not found in session");
            return;
        }

        Log.d(TAG, "Fetching current package for user: " + authUserId);

        // Query users table untuk get customer_id, lalu join ke customers untuk get service_package_id
        // AuthInterceptor will automatically add Authorization header
        // PostgREST filter format: auth_user_id=eq.{uuid}
        service.getCurrentPackage("eq." + authUserId, "customer_id,customers(service_package_id)")
            .enqueue(new Callback<List<UserWithCustomer>>() {
                @Override
                public void onResponse(Call<List<UserWithCustomer>> call, Response<List<UserWithCustomer>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        UserWithCustomer user = response.body().get(0);

                        if (user.customer != null && user.customer.servicePackageId != null) {
                            Integer packageId = user.customer.servicePackageId.intValue();
                            Log.d(TAG, "Current package ID: " + packageId);
                            callback.onSuccess(packageId);
                        } else {
                            Log.w(TAG, "User has no active package");
                            callback.onSuccess(null);
                        }
                    } else {
                        String errorMsg = "Failed to get current package: " + response.code();
                        Log.e(TAG, errorMsg);
                        callback.onError(errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<List<UserWithCustomer>> call, Throwable t) {
                    String errorMsg = "Network error: " + t.getMessage();
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }
            });
    }

    // ===========================
    // Retrofit Service Interface
    // ===========================

    public interface CurrentPackageService {
        /**
         * Get user dengan customer info (embedded join)
         * Query: users?auth_user_id=eq.xxx&select=customer_id,customers(service_package_id)
         * Authorization header will be added automatically by AuthInterceptor
         */
        @GET("rest/v1/users")
        Call<List<UserWithCustomer>> getCurrentPackage(
            @Query("auth_user_id") String authUserId,
            @Query("select") String select
        );
    }

    // ===========================
    // DTOs
    // ===========================

    public static class UserWithCustomer {
        @SerializedName("customer_id")
        public Long customerId;

        @SerializedName("customers")
        public CustomerInfo customer;
    }

    public static class CustomerInfo {
        @SerializedName("service_package_id")
        public Long servicePackageId;
    }
}
