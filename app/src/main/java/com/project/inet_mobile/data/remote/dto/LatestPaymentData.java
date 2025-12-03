package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class LatestPaymentData {
    @SerializedName("payment_id")
    private int paymentId;

    @SerializedName("invoice_id")
    private int invoiceId;

    @SerializedName("order_id")
    private String orderId;

    @SerializedName("status")
    private String status; // pending, settlement, expire, cancel, deny

    @SerializedName("preferred_channel")
    private String preferredChannel;

    @SerializedName("payment_type")
    private String paymentType;

    @SerializedName("redirect_url")
    private String redirectUrl;

    @SerializedName("snap_token")
    private String snapToken;

    @SerializedName("expires_at")
    private String expiresAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("va_numbers")
    private List<VaNumber> vaNumbers;

    @SerializedName("qr_string")
    private String qrString;

    @SerializedName("bill_key")
    private String billKey;

    @SerializedName("biller_code")
    private String billerCode;

    // Getters and Setters
    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreferredChannel() {
        return preferredChannel;
    }

    public void setPreferredChannel(String preferredChannel) {
        this.preferredChannel = preferredChannel;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getSnapToken() {
        return snapToken;
    }

    public void setSnapToken(String snapToken) {
        this.snapToken = snapToken;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<VaNumber> getVaNumbers() {
        return vaNumbers;
    }

    public void setVaNumbers(List<VaNumber> vaNumbers) {
        this.vaNumbers = vaNumbers;
    }

    public String getQrString() {
        return qrString;
    }

    public void setQrString(String qrString) {
        this.qrString = qrString;
    }

    public String getBillKey() {
        return billKey;
    }

    public void setBillKey(String billKey) {
        this.billKey = billKey;
    }

    public String getBillerCode() {
        return billerCode;
    }

    public void setBillerCode(String billerCode) {
        this.billerCode = billerCode;
    }

    // Helper methods for status checking
    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isSettlement() {
        return "settlement".equalsIgnoreCase(status);
    }

    public boolean isExpired() {
        return "expire".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "cancel".equalsIgnoreCase(status);
    }

    public boolean isDenied() {
        return "deny".equalsIgnoreCase(status);
    }

    public boolean isSuccess() {
        return isSettlement();
    }

    public boolean isFailed() {
        return isExpired() || isCancelled() || isDenied();
    }
}
