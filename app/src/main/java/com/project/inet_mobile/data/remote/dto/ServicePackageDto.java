package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

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
