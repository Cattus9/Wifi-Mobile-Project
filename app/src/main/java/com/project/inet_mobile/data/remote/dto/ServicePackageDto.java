package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for service_packages table
 * Core fields: id, name, description, speed, price, is_active
 * Optional fields: is_popular, duration, quota, phone, original_price (may not exist in DB)
 */
public class ServicePackageDto {
    @SerializedName("id")
    public int id;

    @SerializedName("name")
    public String name;

    @SerializedName("description")
    public String description;

    @SerializedName("speed")
    public String speed;

    @SerializedName("price")
    public Double price;

    @SerializedName("is_active")
    public Boolean isActive;

    // Optional fields - may not exist in all tables
    @SerializedName("is_popular")
    public Boolean isPopular;

    @SerializedName("duration")
    public String duration;

    @SerializedName("quota")
    public String quota;

    @SerializedName("phone")
    public String phone;

    @SerializedName("original_price")
    public String originalPrice;
}
