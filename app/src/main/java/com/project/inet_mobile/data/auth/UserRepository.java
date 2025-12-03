package com.project.inet_mobile.data.auth;

import android.util.Log;

import com.project.inet_mobile.data.remote.SupabaseUserService;
import com.project.inet_mobile.data.session.TokenStorage; // Import TokenStorage
import com.project.inet_mobile.data.remote.UpdateProfileRequest; // Added missing import
import com.project.inet_mobile.util.conn;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private static final String TAG = "UserRepository";

    private final SupabaseUserService supabaseUserService;
    private final TokenStorage tokenStorage; // Menggunakan TokenStorage, bukan Context

    public UserRepository(SupabaseUserService supabaseUserService, TokenStorage tokenStorage) {
        this.supabaseUserService = supabaseUserService;
        this.tokenStorage = tokenStorage;
    }

    private String getSupabaseAnonKey() {
        return conn.getSupabaseKey(); // Menggunakan kunci dari conn.java
    }

    public void getUserProfile(UserProfileCallback callback) {
        AuthSession session = tokenStorage.getSession();
        String anonKey = getSupabaseAnonKey();

        if (session == null) {
            callback.onError("User not logged in.");
            return;
        }

        String authUserId = session.getAuthUserId();
        String accessToken = session.getAccessToken();

        if (authUserId == null || authUserId.isEmpty() || accessToken == null || accessToken.isEmpty()) {
            callback.onError("Invalid session data.");
            return;
        }

        // Supabase filter untuk pencocokan auth_user_id
        String authUserIdFilter = "eq." + authUserId;
        String authHeader = "Bearer " + accessToken;

        // Pilih kolom dari users (*) dan kolom 'name', 'phone' dari tabel 'customers' yang berelasi
        String selectColumns = "*,customers(name,phone)";

        supabaseUserService.getUserProfile(anonKey, authHeader, authUserIdFilter, selectColumns)
            .enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        callback.onSuccess(response.body().get(0)); // Harusnya hanya ada satu user
                    } else {
                        String error = "Failed to fetch user profile: " + response.code() + " " + response.message();
                        if (response.errorBody() != null) {
                            try {
                                error += " - " + response.errorBody().string();
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                        }
                        callback.onError(error);
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) {
                    callback.onError("Network error fetching user profile: " + t.getMessage());
                }
            });
    }

    public void updateCustomerDetails(Long customerId, String newName, String newPhone, UpdateCallback callback) {
        AuthSession session = tokenStorage.getSession();
        String anonKey = getSupabaseAnonKey();

        if (session == null) {
            callback.onError("User not logged in.");
            return;
        }
        
        String accessToken = session.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            callback.onError("Invalid session data.");
            return;
        }

        String authHeader = "Bearer " + accessToken;
        UpdateProfileRequest requestBody = new UpdateProfileRequest(customerId, newName, newPhone);

        supabaseUserService.updateCustomerDetails(anonKey, authHeader, requestBody)
            .enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        callback.onSuccess(); // RPC call was successful
                    } else {
                        String error = "Failed to update profile: " + response.code() + " " + response.message();
                        if (response.errorBody() != null) {
                            try {
                                error += " - " + response.errorBody().string();
                            } catch (Exception e) {
                                Log.e(TAG, "Error reading error body", e);
                            }
                        }
                        callback.onError(error);
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    callback.onError("Network error updating profile: " + t.getMessage());
                }
            });
    }

    public interface UserProfileCallback {
        void onSuccess(User user);
        void onError(String message);
    }
    
    // New, simpler callback for update operations that don't return data
    public interface UpdateCallback {
        void onSuccess();
        void onError(String message);
    }
}
