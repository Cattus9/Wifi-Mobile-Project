package com.project.inet_mobile.data.remote;

import com.google.gson.annotations.SerializedName;

// Kelas ini merepresentasikan body JSON untuk panggilan RPC
public class UpdateProfileRequest {
    @SerializedName("target_customer_id")
    private Long customerId;

    @SerializedName("new_name")
    private String name;

    @SerializedName("new_phone")
    private String phone;

    public UpdateProfileRequest(Long customerId, String name, String phone) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
    }
}
