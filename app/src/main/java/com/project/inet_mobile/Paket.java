package com.project.inet_mobile;

public class Paket {

    private int id;
    private String name;
    private String description;
    private String speed;
    private double price;
    private boolean isActive;
    private String duration;

    // Field tambahan untuk UI
    private boolean isPopuler;
    private String quota;
    private String phone;
    private String hargaAsli;

    // Constructor dari database
    public Paket(int id, String name, String description, String speed,
                 double price, boolean isActive, String duration) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.speed = speed;
        this.price = price;
        this.isActive = isActive;
        this.duration = duration;
        this.isPopuler = false;
        this.quota = "Unlimited";
        this.phone = "";
        this.hargaAsli = "";
    }

    // Constructor sederhana (backward compatibility)
    public Paket(String nama, String detail, String harga, boolean isPopuler) {
        this.name = nama;
        this.description = detail;
        this.price = parsePrice(harga);
        this.isPopuler = isPopuler;
        this.duration = "";
        this.id = 0;
        this.speed = "";
        this.quota = "Unlimited";
        this.phone = "";
        this.hargaAsli = "";
        this.isActive = true;
    }

    // Constructor dengan duration (backward compatibility)
    public Paket(String nama, String detail, String harga, boolean isPopuler, String duration) {
        this.name = nama;
        this.description = detail;
        this.price = parsePrice(harga);
        this.isPopuler = isPopuler;
        this.duration = duration;
        this.id = 0;
        this.speed = "";
        this.quota = "Unlimited";
        this.phone = "";
        this.hargaAsli = "";
        this.isActive = true;
    }

    // Helper untuk parse harga dari string
    private double parsePrice(String harga) {
        try {
            return Double.parseDouble(harga.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    // Getters sesuai database
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSpeed() {
        return speed;
    }

    public double getPrice() {
        return price;
    }

    public boolean isActive() {
        return isActive;
    }

    public String getDuration() {
        return duration;
    }

    // Getters untuk backward compatibility
    public String getNama() {
        return name;
    }

    public String getDetail() {
        return description;
    }

    public String getDeskripsi() {
        return description;
    }

    public String getHarga() {
        return formatRupiah(price);
    }

    public String getHargaAsli() {
        return hargaAsli != null && !hargaAsli.isEmpty() ? hargaAsli : "";
    }

    public String getQuota() {
        return quota != null ? quota : "Unlimited";
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public boolean isPopuler() {
        return isPopuler;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setPopuler(boolean populer) {
        isPopuler = populer;
    }

    public void setQuota(String quota) {
        this.quota = quota;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setHargaAsli(String hargaAsli) {
        this.hargaAsli = hargaAsli;
    }

    // Helper untuk format harga ke Rupiah
    private String formatRupiah(double amount) {
        return "Rp" + String.format("%,.0f", amount).replace(",", ".");
    }
}