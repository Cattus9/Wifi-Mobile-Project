package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class OutstandingInvoiceData {
    @SerializedName("invoice_id")
    private int invoiceId;

    @SerializedName("invoice_number")
    private String invoiceNumber;

    @SerializedName("amount")
    private int amount;

    @SerializedName("description")
    private String description;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("status")
    private String status; // issued, overdue, paid, cancelled, draft

    @SerializedName("days_until_due")
    private int daysUntilDue;

    @SerializedName("paid_at")
    private String paidAt;

    @SerializedName("paid_at_formatted")
    private String paidAtFormatted;

    @SerializedName("latest_payment")
    private LatestPaymentData latestPayment;

    // Getters and Setters
    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDaysUntilDue() {
        return daysUntilDue;
    }

    public void setDaysUntilDue(int daysUntilDue) {
        this.daysUntilDue = daysUntilDue;
    }

    public String getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }

    public String getPaidAtFormatted() {
        return paidAtFormatted;
    }

    public void setPaidAtFormatted(String paidAtFormatted) {
        this.paidAtFormatted = paidAtFormatted;
    }

    public LatestPaymentData getLatestPayment() {
        return latestPayment;
    }

    public void setLatestPayment(LatestPaymentData latestPayment) {
        this.latestPayment = latestPayment;
    }

    // Helper methods for status checking
    public boolean isIssued() {
        return "issued".equalsIgnoreCase(status);
    }

    public boolean isOverdue() {
        return "overdue".equalsIgnoreCase(status) || daysUntilDue < 0;
    }

    public boolean isPaid() {
        return "paid".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
    }

    public boolean isDraft() {
        return "draft".equalsIgnoreCase(status);
    }

    public boolean isPayable() {
        return isIssued() || isOverdue();
    }

    // Helper method for formatted amount
    public String getFormattedAmount() {
        return String.format("Rp %,d", amount).replace(",", ".");
    }

    // Helper method to check if has pending payment
    public boolean hasPendingPayment() {
        return latestPayment != null && latestPayment.isPending();
    }
}
