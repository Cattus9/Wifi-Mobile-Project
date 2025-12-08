package com.project.inet_mobile.data.remote.dto.ticket;

import com.google.gson.annotations.SerializedName;

// This is the Java equivalent of the original TicketDto.kt
public class TicketDto {

    @SerializedName("id")
    private long id;

    @SerializedName("customer_id")
    private long customerId;

    @SerializedName("subject")
    private String subject;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("prioritas")
    private String prioritas;

    @SerializedName("kategori")
    private String kategori;

    @SerializedName("sumber")
    private String sumber;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("diperbarui_pada")
    private String diperbaruiPada;

    // Getters
    public long getId() { return id; }
    public long getCustomerId() { return customerId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getPrioritas() { return prioritas; }
    public String getKategori() { return kategori; }
    public String getSumber() { return sumber; }
    public String getCreatedAt() { return createdAt; }
    public String getDiperbaruiPada() { return diperbaruiPada; }
}
