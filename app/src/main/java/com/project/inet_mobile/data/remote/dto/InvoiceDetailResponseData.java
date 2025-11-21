package com.project.inet_mobile.data.remote.dto;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InvoiceDetailResponseData {

    @SerializedName("invoice_id")
    private long invoiceId;

    @SerializedName("invoice_number")
    private String invoiceNumber;

    @SerializedName("month_label")
    private String monthLabel;

    private double amount;

    @SerializedName("due_date")
    private String dueDate;

    private String status;
    private String description;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("paid_at")
    @Nullable
    private String paidAt;

    @SerializedName("is_overdue")
    private boolean isOverdue;

    @SerializedName("can_pay")
    private boolean canPay;

    private CustomerInfo customer;

    @SerializedName("payment_methods")
    private List<PaymentMethod> paymentMethods;

    @SerializedName("latest_payment")
    @Nullable
    private LatestPayment latestPayment;

    public long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public double getAmount() {
        return amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Nullable
    public String getPaidAt() {
        return paidAt;
    }

    public boolean isOverdue() {
        return isOverdue;
    }

    public boolean isCanPay() {
        return canPay;
    }

    public CustomerInfo getCustomer() {
        return customer;
    }

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    @Nullable
    public LatestPayment getLatestPayment() {
        return latestPayment;
    }

    public static class CustomerInfo {
        private String name;
        private String email;

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }

    public static class PaymentMethod {
        private String type;
        private String label;
        private String description;
        private String icon;
        private double fee;

        public String getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return icon;
        }

        public double getFee() {
            return fee;
        }
    }

    public static class LatestPayment {
        @SerializedName("payment_id")
        private long paymentId;

        @SerializedName("order_id")
        private String orderId;

        private String status;

        @SerializedName("preferred_channel")
        private String preferredChannel;

        @SerializedName("expires_at")
        private String expiresAt;

        @SerializedName("redirect_url")
        private String redirectUrl;

        @SerializedName("created_at")
        private String createdAt;

        public long getPaymentId() {
            return paymentId;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getStatus() {
            return status;
        }

        public String getPreferredChannel() {
            return preferredChannel;
        }

        public String getExpiresAt() {
            return expiresAt;
        }

        public String getRedirectUrl() {
            return redirectUrl;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }
}
