# Laporan Uji Whitebox (Formal)

## 1. Informasi Umum
- **Proyek**: Inet-Wifi-Projects
- **Tanggal Uji**: YYYY-MM-DD
- **Penguji**: (isi nama Anda)
- **Teknik**: Whitebox (basis kontrol alur pada kode PHP)
- **Lingkungan**: PHP 8.x, Supabase (mock/offline), XAMPP lokal

---

## 2. Ringkasan Modul

| Modul | Lokasi | Status |
|-------|--------|--------|
| Registrasi Pelanggan | `public/Register.php` | PASS |
| Tambah Pelanggan Baru | `admin/customers.php` | FAIL |

---

## 3. Detail Uji Modul PASS – Registrasi Pelanggan

### 3.1 Tujuan
Memastikan validasi input sebelum proses insert ke Supabase berjalan sesuai spesifikasi.

### 3.2 Kontrol Alur yang Diobservasi
- Token CSRF diverifikasi (`public/Register.php:47`).
- Validasi kosong dan format email (`public/Register.php:66`).
- Cek email duplikat (`UserRepository::emailExists`).
- Password di-hash sebelum disimpan (`public/Register.php:112`).
- Contoh validasi panjang password (ditambahkan di laporan whitebox harian).

### 3.3 Data Uji & Ekspektasi

| Test ID | Input Utama | Ekspektasi | Hasil |
|---------|-------------|------------|-------|
| REG-01 | Email: `invalid@`, password valid | Validasi menolak; error “Format email tidak valid.” | PASS |
| REG-02 | Email sudah terdaftar | Validasi menolak; error “Email sudah terdaftar.” | PASS |
| REG-03 | Password `< 8` karakter | Validasi menolak; error “Password minimal 8 karakter.” | PASS |
| REG-04 | Semua field valid | Data diterima, hash password dibuat, siap insert | PASS |

### 3.4 Catatan
Hash password menggunakan `password_hash` memastikan keamanan sebelum insert. Modul ini dinyatakan **PASS**.

---

## 4. Detail Uji Modul FAIL – Tambah Pelanggan Baru

### 4.1 Tujuan
Menguji implementasi tombol “Tambah Pelanggan Baru” yang masih tahap eksperimen.

### 4.2 Kontrol Alur yang Diobservasi
- Form eksperimen terbuka melalui collapse card (`admin/customers.php:104`).
- Aksi `add_customer` memvalidasi input dasar (nama, email, paket).
- Payload ke Supabase menggunakan field salah (`customer_name`, `service_package`) – `admin/customers.php:173`.
- Respons Supabase (400) ditangani dengan melempar `RuntimeException`.
- Flash error ditampilkan dan form retaining sesi (`$_SESSION['add_customer_form']`).

### 4.3 Data Uji & Ekspektasi

| Test ID | Input Utama | Ekspektasi | Hasil |
|---------|-------------|------------|-------|
| ADD-01 | Email kosong | Validasi menolak; error “Nama dan email wajib diisi.” | PASS |
| ADD-02 | Email tidak valid | Validasi menolak; error “Format email tidak valid.” | PASS |
| ADD-03 | Paket kosong | Validasi menolak; error “Paket layanan belum dipilih.” | PASS |
| ADD-04 | Semua field valid | **Ekspektasi (ideal)**: insert pelanggan sukses <br> **Implementasi sekarang**: Supabase menolak karena field salah, flash error tampil | **FAIL** |

### 4.4 Potongan Kode Penyebab Gagal
```php
$response = $client->post('customers', [
    'headers' => ['Prefer' => 'return=representation'],
    'json' => [
        'customer_name' => $fullName,        // field seharusnya 'name'
        'service_package' => (int) $packageId, // field seharusnya 'service_package_id'
        'status' => 'new',
    ],
]);
```

### 4.5 Rekomendasi
Perbaiki payload Supabase dengan field yang benar (`name`, `service_package_id`) dan tambahkan repositori resmi untuk CRUD pelanggan admin.

---

## 5. Kesimpulan
- Modul Registrasi Pelanggan: **PASS** – seluruh validasi berjalan sesuai ekspektasi.
- Modul Tambah Pelanggan Baru: **FAIL** – payload Supabase salah sehingga insert tidak pernah berhasil. Diperlukan perbaikan sebelum fitur diluncurkan.

**Lampiran**: Whitebox harian (`Doc/Whitebox.md`) menyediakan detail naratif untuk referensi cepat.
