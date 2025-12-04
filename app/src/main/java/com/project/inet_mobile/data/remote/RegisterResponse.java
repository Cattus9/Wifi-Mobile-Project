package com.project.inet_mobile.data.remote;

import com.google.gson.annotations.SerializedName;

/**
 * Response from register_user RPC function
 */
public class RegisterResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("user_id")
    private Long userId;

    @SerializedName("customer_id")
    private Long customerId;

    @SerializedName("message")
    private String message;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getMessage() {
        return message;
    }

    // Setters
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
