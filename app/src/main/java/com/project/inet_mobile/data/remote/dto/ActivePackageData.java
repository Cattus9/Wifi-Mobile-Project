package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class ActivePackageData {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("speed")
    private String speed;

    @SerializedName("price")
    private int price;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    // Helper method for formatted price
    public String getFormattedPrice() {
        return String.format("Rp %,d", price).replace(",", ".");
    }
}
