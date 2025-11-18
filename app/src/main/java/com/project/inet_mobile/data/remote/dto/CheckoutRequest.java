package com.project.inet_mobile.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class CheckoutRequest {

    @SerializedName("invoice_id")
    private final long invoiceId;

    @SerializedName("preferred_channel")
    private final String preferredChannel;

    @SerializedName("return_url")
    @Nullable
    private final String returnUrl;

    public CheckoutRequest(long invoiceId, String preferredChannel, @Nullable String returnUrl) {
        this.invoiceId = invoiceId;
        this.preferredChannel = preferredChannel;
        this.returnUrl = returnUrl;
    }
}
