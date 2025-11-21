## Alur User Journey (Web → Mobile Reference)

Ringkasan ini memetakan flow pelanggan dari awal memilih paket hingga layanan aktif, sebagai referensi untuk pengembangan aplikasi mobile.

### 1. Pilih Paket
- Pengguna membuka landing page (`public/LandingPages.php`).
- CTA “Pilih Paket Ini” menyertakan `service_package_id` dan mengarahkan ke halaman register.

### 2. Registrasi
- Form registrasi (`public/Register.php`) mengumpulkan:
  - Nama lengkap
  - Email + password
  - Nomor telepon WhatsApp
  - Alamat pemasangan
  - Paket yang dipilih (read-only dari CTA)
- Backend melakukan:
  1. Membuat user Supabase Auth (role `customer`).
  2. Menyisipkan data ke tabel `customers` (`status = 'new'`).
  3. Menautkan user → customer di tabel `users`.
  4. Auto-login dan menyimpan session.
- Setelah sukses, user langsung diarahkan ke halaman onboarding.

### 3. Onboarding + CTA WhatsApp
- Halaman `user/onboarding.php` menampilkan:
  - Status layanan (`new`, menunggu verifikasi).
  - Paket yang dipilih, data kontak, instruksi hubungi admin.
  - Tombol CTA WhatsApp. Saat ditekan:
    - Backend mencatat event `customers.onboarding_cta_clicked_at`.
    - User dibawa ke chat WhatsApp untuk validasi manual/penjadwalan instalasi.
- Dalam fase ini status masih `new`, dashboard utama belum bisa diakses.

### 4. Validasi Admin
- Admin menerima data pendaftar di panel:
  - Mengecek CTA (sudah klik atau belum) dan status pelanggan.
  - Setelah instalasi/verifikasi selesai, admin mengubah status pelanggan menjadi `active`.
  - Begitu status `active`, user bisa masuk ke dashboard pelanggan.

### 5. Dashboard Pelanggan
- Halaman `user/dashboard.php` memuat:
  - Status layanan (badge `active`/`suspended`).
  - Paket, kecepatan, harga.
  - Tagihan aktif (invoice `issued`/`overdue`), riwayat 8 invoice terakhir.
  - Tombol “Bayar Sekarang” (Snap Midtrans) jika ada invoice `issued/overdue`.
  - Menu:
    - Riwayat tagihan
    - Profil & ubah data kontak
    - Tiket keluhan
    - Ubah paket (tersedia jika tidak ada tiket perubahan aktif).

### 6. Perubahan Paket (Opsional)
- Dari profil atau menu “Ubah paket”, pelanggan:
  - Memilih paket target.
  - Memasukkan catatan/tanggal preferensi.
  - Sistem membuat tiket `kategori = perubahan_paket`.
  - Jika sudah ada permintaan pending atau invoice belum dibayar, form akan menolak pengajuan.
- Admin memproses di `admin/tickets.php`:
  - Menjadwalkan aktivasi (status `dijadwalkan`) → paket baru aktif otomatis saat cron invoice tanggal 1.
  - Menolak/menutup tiket bila diperlukan.

### 7. Penagihan Bulanan
- Skrip `scripts/generate_invoices.php` (cron tanggal 1):
  - Menerbitkan invoice `issued` untuk semua pelanggan `active`.
  - Mengeksekusi perubahan paket yang sudah dijadwalkan.
- Pembayaran:
  - Pelanggan menggunakan Snap (Midtrans) via dashboard → invoice menjadi `paid`.
  - Jika invoice melewati due date, cron `scripts/mark_overdue.php` menandai `overdue` dan pelanggan berubah `suspended`.
  - Begitu semua tagihan lunas, status pelanggan kembali `active`.

### 8. Keluhan / Support
- Pelanggan membuka `user/tickets.php` untuk membuat tiket (sumber “web”) atau menggunakan tombol WhatsApp:
  - Tombol WA memanggil `api/tickets-whatsapp.php` yang membuat tiket otomatis dan membuka chat.
  - Riwayat tiket menampilkan status & percakapan.
- Admin menggunakan `admin/tickets.php` untuk:
  - Menangani keluhan reguler (open/in_progress/closed).
  - Memproses tiket perubahan paket (jadwalkan, setujui, tolak).

### Flow Teknis: Tiket Keluhan
1. **Input Pelanggan (user/tickets.php)**
   - Form laporan keluhan berisi: subjek keluhan, deskripsi detail, dan kategori (misal koneksi/tagihan). Data dasar (customer_id, paket) diambil dari Supabase.
   - Saat submit, frontend memanggil `api/tickets.php` (sumber web) yang memanggil `TicketRepository->create()`.
2. **Penyimpanan Backend**
   - Endpoint membuat entry `tickets` dengan `status = 'open'`, `prioritas = 'normal'`, `sumber = 'web'`, `pengguna_pembuat_id = user_id`.
   - Bila laporan via WhatsApp (tombol CTA), endpoint `api/tickets-whatsapp.php` otomatis membuat tiket: deskripsi diisi “Keluhan via WhatsApp …” dengan `sumber = 'whatsapp'`.
3. **Admin Handling**
   - `admin/tickets.php` memuat daftar tiket + percakapan (`TicketMessageRepository`).
   - Admin bisa:
     - Update status (`open/in_progress/closed`), menandai `menunggu_detail`.
     - Menambah pesan (public/internal) via `TicketMessageRepository->create`.
4. **Feedback ke Pelanggan**
   - Pelanggan melihat riwayat tiket di `user/tickets.php` (pakai RLS: hanya tiket miliknya).
   - Status tiket ditampilkan real time (Open → Sedang diproses → Selesai). Pesan admin yang tidak `internal` terlihat pelanggan.

### Flow Teknis: Ubah Paket
1. **Input Pelanggan (user/change-package.php)**
   - Form menampilkan paket aktif dan daftar paket baru. Pelanggan memilih paket tujuan, opsi tanggal preferensi, dan catatan.
   - Validasi server:
     - Menolak jika ada tiket perubahan aktif (`TicketPackageChangeRepository->findPendingForCustomer()`).
     - Menolak jika ada invoice `issued/overdue` (dibaca lewat `InvoiceRepository`).
2. **Penyimpanan Backend**
   - Endpoint membuat tiket `kategori = 'perubahan_paket'` (`TicketRepository->create`).
   - Detail disimpan di tabel `ticket_perubahan_paket` (kolom `paket_sekarang_id`, `paket_diminta_id`, `catatan_pelanggan`, `status_keputusan = 'menunggu'`, `tanggal_aktif_diinginkan`).
3. **Admin Handling (`admin/tickets.php`)**
   - Panel menampilkan kartu detail paket sekarang vs paket diminta.
   - Aksi admin:
     - **Setujui & Jadwalkan**: mengisi `jadwal_aktivasi` (default tanggal 1 bulan berikutnya) → `status_keputusan = 'dijadwalkan'`.
     - **Setujui sekarang** (opsi sebelumnya) telah diganti, kini perubahan dieksekusi otomatis oleh cron.
     - **Tolak**: mengisi alasan, menutup tiket.
     - Catatan admin tersimpan di kolom `catatan_admin`.
4. **Eksekusi Otomatis**
   - Cron `scripts/generate_invoices.php` memeriksa tiket `dijadwalkan` dengan `jadwal_aktivasi <= tanggal 1`, lalu:
     - Mengubah `customers.service_package_id` ke paket baru.
     - Menandai detail tiket `status_keputusan = 'disetujui'`, `diterapkan_pada = NOW()` dan menutup tiket (`tickets.status = 'closed'`).
     - Membuat invoice baru dengan harga paket baru untuk bulan berjalan.
5. **Feedback ke Pelanggan**
   - Di profil/tiket, pelanggan melihat status (menunggu jadwal / disetujui / ditolak) beserta jadwal aktivasi.
   - Setelah cron jalan, dashboard menampilkan paket baru dan tagihan bulan berikutnya otomatis memakai harga paket tersebut.

### Catatan untuk Pengembangan Mobile
- **Autentikasi:** Gunakan Supabase Auth (email/password). Setelah login, panggil API/RPC sesuai permission.
- **Data register tambahan:** Tidak aman menulis ke `customers/users` langsung dari app tanpa service key. Buat endpoint server-side atau RPC terproteksi untuk menyimpan nama, telepon, alamat, paket.
- **Monitoring status:** Mobile cukup memeriksa kolom `customers.status` (`new`, `active`, `suspended`, `cancelled`) untuk menampilkan halaman sesuai fase (onboarding vs dashboard).
- **Tagihan & Midtrans:** Tampilkan invoice `issued/overdue` dan gunakan Snap (client key) serupa dashboard. Pastikan webhook tetap memproses notifikasi.
- **Perubahan paket / keluhan:** Gunakan RPC atau endpoint existing (lihat `Doc/MobileDokumentasi.md`) agar logika sama dengan web.
