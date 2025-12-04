package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.auth.User;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

import retrofit2.http.POST; // Added for RPC call
import com.project.inet_mobile.data.remote.UpdateProfileRequest; // Added request body model
import com.project.inet_mobile.data.remote.RegisterRequest;
import com.project.inet_mobile.data.remote.RegisterResponse;

public interface SupabaseUserService {

    // Method to fetch user profile by auth_user_id
    // @Headers are crucial for Supabase REST API interaction
    @Headers({
        "Accept: application/json",
        "Content-Type: application/json"
    })
    @GET("rest/v1/users")
    Call<List<User>> getUserProfile(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authHeader, // Format: "Bearer <access_token>"
        @Query("auth_user_id") String authUserIdFilter, // Format: "eq.<auth_user_id>"
        @Query("select") String selectColumns // e.g., "*,customers(name,phone)"
    );

    // NEW Method to call the 'update_customer_details' RPC function
    @Headers({
        "Accept: application/json",
        "Content-Type: application/json"
    })
    @POST("rest/v1/rpc/update_customer_details")
    Call<Void> updateCustomerDetails(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authHeader,
        @Body UpdateProfileRequest body
    );

    @Headers({
        "Accept: application/json",
        "Content-Type: application/json"
    })
    @PATCH("rest/v1/users")
    Call<List<User>> updateAvatarUrl(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authHeader,
        @Header("Prefer") String prefer,
        @Query("id") String userIdFilter,
        @Body java.util.Map<String, Object> payload // expects { "avatar_url": "<url or null>" }
    );

    // NEW Method to call the 'register_user' RPC function
    @Headers({
        "Accept: application/json",
        "Content-Type: application/json"
    })
    @POST("rest/v1/rpc/register_user")
    Call<RegisterResponse> registerUser(
        @Header("apikey") String apiKey,
        @Body RegisterRequest body
    );
}
