package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class CheckoutResponseData {

    @SerializedName("payment_id")
    private long paymentId;

    @SerializedName("order_id")
    private String orderId;

    @SerializedName("snap_token")
    private String snapToken;

    @SerializedName("redirect_url")
    private String redirectUrl;

    @SerializedName("gross_amount")
    private double grossAmount;

    @SerializedName("preferred_channel")
    private String preferredChannel;

    @SerializedName("expires_at")
    private String expiresAt;

    private InvoiceSummary invoice;

    public long getPaymentId() {
        return paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSnapToken() {
        return snapToken;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public double getGrossAmount() {
        return grossAmount;
    }

    public String getPreferredChannel() {
        return preferredChannel;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public InvoiceSummary getInvoice() {
        return invoice;
    }

    public static class InvoiceSummary {
        private long id;
        private String description;
        @SerializedName("due_date")
        private String dueDate;
        private String status;

        public long getId() {
            return id;
        }

        public String getDescription() {
            return description;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getStatus() {
            return status;
        }
    }
}
