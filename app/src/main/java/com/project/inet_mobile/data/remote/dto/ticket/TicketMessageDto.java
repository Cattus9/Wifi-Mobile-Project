package com.project.inet_mobile.data.remote.dto.ticket;

import com.google.gson.annotations.SerializedName;

// This is the Java equivalent of the original TicketMessageDto.kt
public class TicketMessageDto {

    @SerializedName("id")
    private long id;

    @SerializedName("ticket_id")
    private long ticketId;

    @SerializedName("tipe_penulis")
    private String tipePenulis;

    @SerializedName("penulis_id")
    private Long penulisId;

    @SerializedName("isi")
    private String isi;

    @SerializedName("internal")
    private boolean internal;

    @SerializedName("dibuat_pada")
    private String dibuatPada;

    // Getters
    public long getId() { return id; }
    public long getTicketId() { return ticketId; }
    public String getTipePenulis() { return tipePenulis; }
    public Long getPenulisId() { return penulisId; }
    public String getIsi() { return isi; }
    public boolean isInternal() { return internal; }
    public String getDibuatPada() { return dibuatPada; }
}
