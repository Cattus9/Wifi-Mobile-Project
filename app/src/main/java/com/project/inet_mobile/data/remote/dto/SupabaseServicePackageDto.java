package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for service_packages table from Supabase PostgREST
 *
 * Schema: id, name, description, speed, price, is_active
 */
public class SupabaseServicePackageDto {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("speed")
    private String speed;

    @SerializedName("price")
    private Double price;

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSpeed() {
        return speed;
    }

    public String getQuota() {
        // Since there's no quota column, return "Unlimited" for all packages
        return "Unlimited";
    }

    public Double getPrice() {
        return price;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
