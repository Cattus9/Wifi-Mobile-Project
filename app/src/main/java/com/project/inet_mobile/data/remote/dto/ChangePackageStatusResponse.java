package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Minimal status response for change-package ticket.
 */
public class ChangePackageStatusResponse {
    @SerializedName("ticket_id")
    private long ticketId;

    @SerializedName("status")
    private String status; // open/in_progress/closed

    @SerializedName("status_keputusan")
    private String statusKeputusan; // menunggu/disetujui/ditolak/dijadwalkan

    @SerializedName("paket_sekarang_id")
    private Long paketSekarangId;

    @SerializedName("paket_diminta_id")
    private Long paketDimintaId;

    @SerializedName("catatan_admin")
    private String catatanAdmin;

    @SerializedName("catatan_pelanggan")
    private String catatanPelanggan;

    @SerializedName("jadwal_aktivasi")
    private String jadwalAktivasi;

    @SerializedName("diterapkan_pada")
    private String diterapkanPada;

    public long getTicketId() { return ticketId; }
    public String getStatus() { return status; }
    public String getStatusKeputusan() { return statusKeputusan; }
    public Long getPaketSekarangId() { return paketSekarangId; }
    public Long getPaketDimintaId() { return paketDimintaId; }
    public String getCatatanAdmin() { return catatanAdmin; }
    public String getCatatanPelanggan() { return catatanPelanggan; }
    public String getJadwalAktivasi() { return jadwalAktivasi; }
    public String getDiterapkanPada() { return diterapkanPada; }
}
