package com.project.inet_mobile.data.remote.dto.ticket;

import com.google.gson.annotations.SerializedName;

public class CreateTicketRequest {

    @SerializedName("kategori")
    private final String kategori;

    @SerializedName("subject")
    private final String subject;

    @SerializedName("description")
    private final String description;

    public CreateTicketRequest(String kategori, String subject, String description) {
        this.kategori = kategori;
        this.subject = subject;
        this.description = description;
    }
}
