# 📘 UI/UX Review — Halaman Riwayat Pembayaran

**Versi Dokumen:** 1.0  
**Tujuan:** Panduan perbaikan desain UI/UX untuk halaman _Riwayat Pembayaran_ aplikasi mobile ISP.

---

## 📌 1. Ringkasan

Halaman _Riwayat Pembayaran_ secara keseluruhan sudah modern, jelas, dan nyaman digunakan. Namun terdapat beberapa area yang dapat ditingkatkan untuk memperbaiki konsistensi, kejelasan status, dan mengurangi kebingungan pengguna terutama pada konteks filter transaksi.

Dokumen ini merangkum seluruh rekomendasi perbaikan UI/UX agar implementasi lebih terarah dan konsisten.

---

## ✅ 2. Hal yang Sudah Baik

- **Visual hierarchy jelas**: Judul besar, card utama dominan, dan struktur informasi rapi.

- **Struktur informasi mudah dipahami**: Total bayar, status layanan, tunggakan.

- **Filter kategori riwayat efektif**: Rapi dan mudah disentuh.

- **Desain modern & ramah pengguna**: Warna soft dan layout bersih.

- **Navigasi bawah konsisten** dengan standar aplikasi ISP/fintech.


---

## ⚠️ 3. Masalah & Rekomendasi Peningkatan

### \### 3.1 Redundant Information

Beberapa pesan menyampaikan hal yang sama:

- “Belum ada data untuk ditampilkan”

- “Belum ada pembayaran”

- “0 transaksi”


**Rekomendasi:** Gunakan satu pesan empty state:

> “Kamu belum memiliki riwayat pembayaran.”

---

### 3.2 Status Layanan Kurang Menonjol

“Menunggu Aktivasi” kurang terlihat padahal sangat penting.

**Rekomendasi:**

- Tambahkan ikon (⏳)

- Gunakan badge warna kuning soft untuk highlight


---

### 3.3 Readability Card Biru

Teks terlalu lembut untuk background gradient.

**Rekomendasi:**

- Gunakan warna teks lebih gelap (Dark navy)

- Tambahkan sedikit bold


---

### 3.4 Empty State Terlalu Kosong

Area konten kosong terlalu besar sehingga tampilan terasa hampa.

**Rekomendasi:**  
Tambahkan ilustrasi atau ikon empty-state:

> 📄 “Belum ada transaksi pembayaran.”

---

### 3.5 Filter Selected Kurang Menonjol

Checklist kecil kurang kuat sebagai indikator selected.

**Rekomendasi:**

- Background ungu muda (#EAE4FF)

- Teks ungu gelap (#622AFF)

- Border tipis


---

### 3.6 Badge "Belum Ada Pembayaran" Tidak Relevan

Badge oval tidak cocok untuk informasi besar.

**Rekomendasi:**  
Ganti dengan teks biasa atau badge kecil di posisi yang tidak dominan.

---

### 3.7 Ikon Bottom Navigation Perlu Dikhususkan

Ikon tab “Riwayat” sedikit kecil dibanding elemen lain.

**Rekomendasi:**  
Perbesar ikon 2–3dp.

---

### 3.8 ⚠️ _Paling Penting:_ Filter “Pending” Tidak Boleh Menampilkan “Menunggu Aktivasi”

**Menunggu Aktivasi = status layanan**  
**Pending = status transaksi pembayaran**

Keduanya berbeda domain data.

**Masalah:**  
Saat user memilih filter _Pending_, card berubah menjadi “Menunggu Aktivasi”.  
Ini **tidak relevan** dan dapat membingungkan pengguna.

**Rekomendasi:**

- Untuk filter Pending, tampilkan status pembayaran:

    - “Menunggu pembayaran”

    - “Menunggu konfirmasi”

    - “Transaksi sedang diproses”

- Jika tidak ada transaksi pending:

  > “Tidak ada transaksi pending.”

- **Card tidak boleh berubah ke status layanan saat berada di filter transaksi.**


---

## ⭐ 4. Rekomendasi Struktur Card Baru

yaml

Salin kode

`Total Terbayar (Tahun Ini) Rp0  Status Layanan: ⏳ Menunggu Aktivasi  Tunggakan: Rp0  Belum ada riwayat pembayaran.`

Lebih jelas, tidak repetitif, dan tidak bercampur antar domain status.

---

## 📂 5. Catatan Implementasi

- Pastikan setiap filter (Semua, Lunas, Pending, Overdue, Batal) memiliki empty state-nya sendiri.

- Pastikan card atas tidak berubah konteks karena dipengaruhi filter transaksi.

- Gunakan dynamic empty-state UI untuk pengalaman lebih human-friendly.


---

## 📄 6. Versi & Revisi

- **V1.0** — Dokumen pertama, berisi seluruh evaluasi dan rekomendasi.