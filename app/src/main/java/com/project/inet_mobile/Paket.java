package com.project.inet_mobile;

public class Paket {

    private String nama;
    private String detail;
    private String harga;
    private boolean isPopuler;
    private String duration; // FIELD BARU

    // Constructor lama (untuk backward compatibility)
    public Paket(String nama, String detail, String harga, boolean isPopuler) {
        this.nama = nama;
        this.detail = detail;
        this.harga = harga;
        this.isPopuler = isPopuler;
        this.duration = "";
    }

    // Constructor baru dengan duration
    public Paket(String nama, String detail, String harga, boolean isPopuler, String duration) {
        this.nama = nama;
        this.detail = detail;
        this.harga = harga;
        this.isPopuler = isPopuler;
        this.duration = duration;
    }

    // Getter dan Setter
    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getHarga() {
        return harga;
    }

    public void setHarga(String harga) {
        this.harga = harga;
    }

    public boolean isPopuler() {
        return isPopuler;
    }

    public void setPopuler(boolean populer) {
        isPopuler = populer;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}