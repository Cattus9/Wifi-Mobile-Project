package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class VaNumber {
    @SerializedName("bank")
    private String bank;

    @SerializedName("va_number")
    private String vaNumber;

    // Getters and Setters
    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getVaNumber() {
        return vaNumber;
    }

    public void setVaNumber(String vaNumber) {
        this.vaNumber = vaNumber;
    }
}
