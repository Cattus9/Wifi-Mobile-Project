package com.project.inet_mobile.data.remote.dto.ticket;

import com.google.gson.annotations.SerializedName;

public class CreateTicketMessageRequest {

    @SerializedName("ticket_id")
    private final long ticketId;

    @SerializedName("isi")
    private final String isi;

    @SerializedName("tipe_penulis")
    private final String tipePenulis = "customer";

    public CreateTicketMessageRequest(long ticketId, String isi) {
        this.ticketId = ticketId;
        this.isi = isi;
    }
}
