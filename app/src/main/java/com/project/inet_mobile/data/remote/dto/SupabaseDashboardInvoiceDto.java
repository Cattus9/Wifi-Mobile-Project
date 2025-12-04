package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * DTO for invoices table from Supabase PostgREST
 *
 * Query: /rest/v1/invoices?customer_id=eq.{id}&status=in.(unpaid,pending)&order=due_date.desc&limit=1
 */
public class SupabaseDashboardInvoiceDto {
    @SerializedName("id")
    private int id;

    @SerializedName("customer_id")
    private int customerId;

    @SerializedName("amount")
    private int amount;

    @SerializedName("due_date")
    private String dueDate;

    @SerializedName("status")
    private String status;

    // Getters
    public int getId() {
        return id;
    }

    public String getInvoiceNumber() {
        return "INV-" + id; // Generate from ID since invoice_number column doesn't exist
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getAmount() {
        return amount;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Helper methods
    public String getFormattedAmount() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        return formatter.format(amount).replace("Rp", "Rp ");
    }

    public String getFormattedDueDate() {
        if (dueDate == null) return "-";
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID"));
            Date date = inputFormat.parse(dueDate);
            return date != null ? outputFormat.format(date) : dueDate;
        } catch (ParseException e) {
            return dueDate;
        }
    }

    public int getDaysUntilDue() {
        if (dueDate == null) return 0;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date due = format.parse(dueDate);
            Date now = new Date();
            if (due != null) {
                long diff = due.getTime() - now.getTime();
                return (int) TimeUnit.MILLISECONDS.toDays(diff);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isOverdue() {
        return getDaysUntilDue() < 0;
    }

    public boolean isUnpaid() {
        return "issued".equalsIgnoreCase(status) || "overdue".equalsIgnoreCase(status);
    }

    public boolean isIssued() {
        return "issued".equalsIgnoreCase(status);
    }

    public boolean isPaid() {
        return "paid".equalsIgnoreCase(status);
    }
}
