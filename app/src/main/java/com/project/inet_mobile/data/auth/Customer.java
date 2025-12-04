package com.project.inet_mobile.data.auth;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Customer implements Serializable {
    @SerializedName("id")
    private Long id;

    @SerializedName("nik")
    private String nik;

    @SerializedName("name")
    private String name;

    @SerializedName("phone")
    private String phone;

    @SerializedName("address")
    private String address;

    @SerializedName("status")
    private String status;

    @SerializedName("service_package_id")
    private Integer servicePackageId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getServicePackageId() {
        return servicePackageId;
    }

    public void setServicePackageId(Integer servicePackageId) {
        this.servicePackageId = servicePackageId;
    }
}
