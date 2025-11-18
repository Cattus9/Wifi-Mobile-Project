package com.project.inet_mobile.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class PaymentStatusResponseData {

    @SerializedName("invoice_id")
    private long invoiceId;

    @SerializedName("invoice_status")
    private String invoiceStatus;

    @SerializedName("invoice_amount")
    private double invoiceAmount;

    @SerializedName("paid_at")
    @Nullable
    private String paidAt;

    private PaymentInfo payment;

    public long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceStatus() {
        return invoiceStatus;
    }

    public double getInvoiceAmount() {
        return invoiceAmount;
    }

    @Nullable
    public String getPaidAt() {
        return paidAt;
    }

    public PaymentInfo getPayment() {
        return payment;
    }

    public static class PaymentInfo {
        @SerializedName("payment_id")
        private long paymentId;

        @SerializedName("order_id")
        private String orderId;

        private String status;

        @SerializedName("payment_type")
        private String paymentType;

        @SerializedName("preferred_channel")
        private String preferredChannel;

        @SerializedName("transaction_id")
        private String transactionId;

        @SerializedName("settlement_time")
        @Nullable
        private String settlementTime;

        @SerializedName("expires_at")
        @Nullable
        private String expiresAt;

        @SerializedName("va_numbers")
        @Nullable
        private List<VirtualAccount> vaNumbers;

        @SerializedName("qr_string")
        @Nullable
        private String qrString;

        @SerializedName("deeplink_url")
        @Nullable
        private String deeplinkUrl;

        @SerializedName("metadata")
        @Nullable
        private Map<String, Object> metadata;

        public long getPaymentId() {
            return paymentId;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getStatus() {
            return status;
        }

        public String getPaymentType() {
            return paymentType;
        }

        public String getPreferredChannel() {
            return preferredChannel;
        }

        public String getTransactionId() {
            return transactionId;
        }

        @Nullable
        public String getSettlementTime() {
            return settlementTime;
        }

        @Nullable
        public String getExpiresAt() {
            return expiresAt;
        }

        @Nullable
        public List<VirtualAccount> getVaNumbers() {
            return vaNumbers;
        }

        @Nullable
        public String getQrString() {
            return qrString;
        }

        @Nullable
        public String getDeeplinkUrl() {
            return deeplinkUrl;
        }

        @Nullable
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    public static class VirtualAccount {
        private String bank;
        @SerializedName("va_number")
        private String vaNumber;

        public String getBank() {
            return bank;
        }

        public String getVaNumber() {
            return vaNumber;
        }
    }
}
