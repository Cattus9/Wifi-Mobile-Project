package com.project.inet_mobile;

public class Paket {

    private String nama;
    private String detail;
    private String harga;
    private boolean isPopuler;

    // Constructor
    public Paket(String nama, String detail, String harga, boolean isPopuler) {
        this.nama = nama;
        this.detail = detail;
        this.harga = harga;
        this.isPopuler = isPopuler;
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
}