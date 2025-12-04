package com.project.inet_mobile.data.remote;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for register_user RPC function
 */
public class RegisterRequest {
    @SerializedName("p_auth_user_id")
    private String authUserId;

    @SerializedName("p_email")
    private String email;

    @SerializedName("p_phone")
    private String phone;

    @SerializedName("p_name")
    private String name;

    @SerializedName("p_address")
    private String address;

    public RegisterRequest(String authUserId, String email, String phone, String name, String address) {
        this.authUserId = authUserId;
        this.email = email;
        this.phone = phone;
        this.name = name;
        this.address = address;
    }

    // Getters
    public String getAuthUserId() {
        return authUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    // Setters
    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
