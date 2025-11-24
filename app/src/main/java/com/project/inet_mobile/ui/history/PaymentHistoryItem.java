package com.project.inet_mobile.ui.history;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

/**
 * Model sederhana untuk menampilkan riwayat pembayaran di RecyclerView.
 * Menyimpan status invoice sebagai enum agar konsisten ketika data nyata dari Supabase
 * mulai digunakan di sesi berikutnya.
 */
public class PaymentHistoryItem {

    private final String monthLabel;
    private final String paymentDate;
    private final String method;
    private final String invoiceNumber;
    private final String amountFormatted;
    private final double amountValue;
    private final InvoiceStatus status;
    private final String metaDescription;
    @ColorInt
    private final int statusTextColor;
    @ColorInt
    private final int statusBackgroundColor;

    public PaymentHistoryItem(
            @NonNull String monthLabel,
            @NonNull String paymentDate,
            @NonNull String method,
            @NonNull String invoiceNumber,
            @NonNull InvoiceStatus status,
            @NonNull String amountFormatted,
            double amountValue,
            @ColorInt int statusTextColor,
            @ColorInt int statusBackgroundColor
    ) {
        this.monthLabel = monthLabel;
        this.paymentDate = paymentDate;
        this.method = method;
        this.invoiceNumber = invoiceNumber;
        this.status = status;
        this.amountFormatted = amountFormatted;
        this.amountValue = amountValue;
        this.statusTextColor = statusTextColor;
        this.statusBackgroundColor = statusBackgroundColor;
        this.metaDescription = buildMeta(paymentDate, method, invoiceNumber);
    }

    public String getMonthLabel() {
        return monthLabel;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public String getMethod() {
        return method;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public String getAmountFormatted() {
        return amountFormatted;
    }

    public double getAmountValue() {
        return amountValue;
    }

    @ColorInt
    public int getStatusTextColor() {
        return statusTextColor;
    }

    @ColorInt
    public int getStatusBackgroundColor() {
        return statusBackgroundColor;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return status.label;
    }

    private static String buildMeta(@NonNull String date, @NonNull String payMethod, @NonNull String invoiceNumber) {
        return date + " • " + payMethod + " • " + invoiceNumber;
    }

    /**
     * Enumerasi dasar status invoice yang akan disejajarkan dengan enum pada domain Supabase nantinya.
     */
    public enum InvoiceStatus {
        PAID("PAID"),
        OVERDUE("OVERDUE"),
        DRAFT("DRAFT"),
        PENDING("PENDING"),
        CANCELLED("CANCELLED"),
        UNKNOWN("UNKNOWN");

        private final String label;

        InvoiceStatus(@NonNull String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public boolean isPaid() {
            return this == PAID;
        }

        public boolean isOverdue() {
            return this == OVERDUE;
        }

        public boolean isPending() {
            return this == PENDING;
        }

        public boolean isCancelled() {
            return this == CANCELLED;
        }

        @NonNull
        public static InvoiceStatus fromRaw(@NonNull String rawStatus) {
            for (InvoiceStatus value : values()) {
                if (value.label.equalsIgnoreCase(rawStatus)) {
                    return value;
                }
            }
            return UNKNOWN;
        }
    }
}
