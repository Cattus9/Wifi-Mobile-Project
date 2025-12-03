package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Payload for submitting change-package ticket.
 * Matches POST /api/v1/customer/change-package endpoint.
 */
public class ChangePackageRequest {
    @SerializedName("package_id")
    private final long packageId;

    @SerializedName("notes")
    private final String notes;

    public ChangePackageRequest(long packageId, String notes) {
        this.packageId = packageId;
        this.notes = notes;
    }
}
