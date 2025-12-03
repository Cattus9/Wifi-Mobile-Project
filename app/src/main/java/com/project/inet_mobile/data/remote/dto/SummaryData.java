package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SummaryData {
    @SerializedName("total_outstanding")
    private int totalOutstanding;

    @SerializedName("outstanding_count")
    private int outstandingCount;

    @SerializedName("has_pending_payment")
    private boolean hasPendingPayment;

    // Getters and Setters
    public int getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(int totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }

    public int getOutstandingCount() {
        return outstandingCount;
    }

    public void setOutstandingCount(int outstandingCount) {
        this.outstandingCount = outstandingCount;
    }

    public boolean isHasPendingPayment() {
        return hasPendingPayment;
    }

    public void setHasPendingPayment(boolean hasPendingPayment) {
        this.hasPendingPayment = hasPendingPayment;
    }

    // Helper method for formatted total
    public String getFormattedTotalOutstanding() {
        return String.format("Rp %,d", totalOutstanding).replace(",", ".");
    }
}
