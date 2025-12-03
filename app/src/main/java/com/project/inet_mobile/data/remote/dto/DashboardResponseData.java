package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class DashboardResponseData {
    @SerializedName("customer")
    private CustomerData customer;

    @SerializedName("active_package")
    private ActivePackageData activePackage;

    @SerializedName("outstanding_invoice")
    private OutstandingInvoiceData outstandingInvoice;

    @SerializedName("summary")
    private SummaryData summary;

    // Getters and Setters
    public CustomerData getCustomer() {
        return customer;
    }

    public void setCustomer(CustomerData customer) {
        this.customer = customer;
    }

    public ActivePackageData getActivePackage() {
        return activePackage;
    }

    public void setActivePackage(ActivePackageData activePackage) {
        this.activePackage = activePackage;
    }

    public OutstandingInvoiceData getOutstandingInvoice() {
        return outstandingInvoice;
    }

    public void setOutstandingInvoice(OutstandingInvoiceData outstandingInvoice) {
        this.outstandingInvoice = outstandingInvoice;
    }

    public SummaryData getSummary() {
        return summary;
    }

    public void setSummary(SummaryData summary) {
        this.summary = summary;
    }
}
