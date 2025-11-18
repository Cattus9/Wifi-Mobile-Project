package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class InvoiceListResponseData {

    private List<InvoiceItem> items;
    private Pagination pagination;

    public List<InvoiceItem> getItems() {
        return items;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public static class InvoiceItem {
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

        @SerializedName("paid_at")
        private String paidAt;

        @SerializedName("created_at")
        private String createdAt;

        @SerializedName("is_overdue")
        private boolean isOverdue;

        @SerializedName("can_pay")
        private boolean canPay;

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

        public String getPaidAt() {
            return paidAt;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public boolean isOverdue() {
            return isOverdue;
        }

        public boolean canPay() {
            return canPay;
        }
    }

    public static class Pagination {
        private int total;
        private int limit;
        private int offset;

        @SerializedName("current_page")
        private int currentPage;

        @SerializedName("total_pages")
        private int totalPages;

        public int getTotal() {
            return total;
        }

        public int getLimit() {
            return limit;
        }

        public int getOffset() {
            return offset;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public int getTotalPages() {
            return totalPages;
        }
    }
}
