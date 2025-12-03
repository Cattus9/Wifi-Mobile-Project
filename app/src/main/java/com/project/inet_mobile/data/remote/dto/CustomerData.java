package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CustomerData {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("email")
    private String email;

    @SerializedName("status")
    private String status; // active, suspended, inactive

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper methods
    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    public boolean isSuspended() {
        return "suspended".equalsIgnoreCase(status);
    }

    public boolean isInactive() {
        return "inactive".equalsIgnoreCase(status);
    }
}
