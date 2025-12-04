package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for users table joined with customers and service_packages from Supabase PostgREST
 *
 * Query: /rest/v1/users?auth_user_id=eq.{uuid}&select=customer_id,customers(id,name,status,service_package_id,service_packages(id,name,speed,quota))
 */
public class SupabaseDashboardUserDto {
    @SerializedName("customer_id")
    private Integer customerId;

    @SerializedName("customers")
    private SupabaseCustomerDto customer;

    // Getters
    public Integer getCustomerId() {
        return customerId;
    }

    public SupabaseCustomerDto getCustomer() {
        return customer;
    }

    // Setters
    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
    }

    public void setCustomer(SupabaseCustomerDto customer) {
        this.customer = customer;
    }
}
