package com.project.inet_mobile.data.auth;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class User implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("auth_user_id")
    private String authUserId;

    @SerializedName("customer_id")
    private Long customerId;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("full_name")
    private String fullName; // Kept for potential future use or for admin users

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("phone")
    private String phone;
    
    // Nested Customer object for the JOIN result
    @SerializedName("customers")
    private Customer customer;

    // Constructor updated to include Customer
    public User(Long id, String authUserId, Long customerId, String email, String role, String fullName, String avatarUrl, String phone, Customer customer) {
        this.id = id;
        this.authUserId = authUserId;
        this.customerId = customerId;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.customer = customer;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public Customer getCustomer() {
        return customer;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
