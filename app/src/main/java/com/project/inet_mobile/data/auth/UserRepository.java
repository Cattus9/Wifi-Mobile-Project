package com.project.inet_mobile.data.auth;

import android.util.Log;

import com.project.inet_mobile.data.remote.SupabaseUserService;
import com.project.inet_mobile.data.session.TokenStorage; // Import TokenStorage
import com.project.inet_mobile.data.remote.UpdateProfileRequest; // Added missing import
import com.project.inet_mobile.data.remote.RegisterRequest;
import com.project.inet_mobile.data.remote.RegisterResponse;
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

    /**
     * Register new user by creating customer and user records in database
     * Should be called AFTER Supabase Auth signup
     *
     * @param packageId Selected service package ID (can be null)
     */
    public void registerUser(String authUserId, String email, String phone, String name, String address, Long packageId, RegisterCallback callback) {
        String anonKey = getSupabaseAnonKey();

        RegisterRequest requestBody = new RegisterRequest(authUserId, email, phone, name, address, packageId);

        Log.d(TAG, "Sending registerUser RPC with packageId: " + packageId);

        supabaseUserService.registerUser(anonKey, requestBody)
                .enqueue(new Callback<RegisterResponse>() {
                    @Override
                    public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                        Log.d(TAG, "RPC Response Code: " + response.code());

                        if (response.isSuccessful()) {
                            RegisterResponse registerResponse = response.body();
                            Log.d(TAG, "Response Body: " + (registerResponse != null ? "Not null" : "NULL"));

                            if (registerResponse != null) {
                                Log.d(TAG, "Success: " + registerResponse.isSuccess());
                                Log.d(TAG, "Message: " + registerResponse.getMessage());

                                if (registerResponse.isSuccess()) {
                                    callback.onSuccess(registerResponse.getMessage());
                                } else {
                                    callback.onError("Registrasi gagal: " + registerResponse.getMessage());
                                }
                            } else {
                                Log.e(TAG, "Response body is null despite successful response");
                                callback.onError("Response body is null");
                            }
                        } else {
                            String error = "Failed to register user: " + response.code() + " " + response.message();
                            if (response.errorBody() != null) {
                                try {
                                    String errorBody = response.errorBody().string();
                                    Log.e(TAG, "Error body: " + errorBody);
                                    error += " - " + errorBody;
                                } catch (Exception e) {
                                    Log.e(TAG, "Error reading error body", e);
                                }
                            }
                            callback.onError(error);
                        }
                    }

                    @Override
                    public void onFailure(Call<RegisterResponse> call, Throwable t) {
                        Log.e(TAG, "Network failure during registration", t);
                        callback.onError("Network error during registration: " + t.getMessage());
                    }
                });
    }

    /**
     * Backward compatibility method - register without package
     * @deprecated Use registerUser with packageId parameter instead
     */
    @Deprecated
    public void registerUser(String authUserId, String email, String phone, String name, String address, RegisterCallback callback) {
        registerUser(authUserId, email, phone, name, address, null, callback);
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

    // Callback for registration
    public interface RegisterCallback {
        void onSuccess(String message);
        void onError(String message);
    }
}
