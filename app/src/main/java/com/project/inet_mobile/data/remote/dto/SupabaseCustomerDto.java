package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO for customers table from Supabase PostgREST
 */
public class SupabaseCustomerDto {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("status")
    private String status;

    @SerializedName("service_package_id")
    private Integer servicePackageId;

    @SerializedName("service_packages")
    private SupabaseServicePackageDto servicePackage;

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Integer getServicePackageId() {
        return servicePackageId;
    }

    public SupabaseServicePackageDto getServicePackage() {
        return servicePackage;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setServicePackageId(Integer servicePackageId) {
        this.servicePackageId = servicePackageId;
    }

    public void setServicePackage(SupabaseServicePackageDto servicePackage) {
        this.servicePackage = servicePackage;
    }

    // Helper methods
    public boolean isActive() {
        return "active".equalsIgnoreCase(status);
    }

    public String getStatusDisplay() {
        if (status == null) return "Unknown";
        switch (status.toLowerCase()) {
            case "active":
                return "Aktif";
            case "suspended":
                return "Ditangguhkan";
            case "inactive":
                return "Nonaktif";
            default:
                return status;
        }
    }
}
