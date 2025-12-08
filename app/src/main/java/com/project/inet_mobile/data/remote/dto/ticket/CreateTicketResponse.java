package com.project.inet_mobile.data.remote.dto.ticket;

import com.google.gson.annotations.SerializedName;

// This Java class now correctly depends on the new TicketDto.java
public class CreateTicketResponse {

    @SerializedName("ticket")
    private TicketDto ticket;

    @SerializedName("whatsapp_number")
    private String whatsappNumber;

    public TicketDto getTicket() {
        return ticket;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }
}
