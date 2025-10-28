# Rencana Pengembangan ISP App

## Sasaran Utama
- Integrasikan autentikasi Supabase (sign-up, sign-in, refresh, logout) dan tautkan user session ke entri `customers`.
- Bangun lapisan data (repository + model) berbasis OkHttp/Retrofit + Gson untuk tabel utama: `service_packages`, `customers`, `invoices`, `tickets`.
- Implementasikan fitur front-office secara bertahap dimulai dari Riwayat (billing & tiket), Pembayaran (CTA bayar, status invoice), dan Akun (profil pelanggan).
- Tingkatkan keamanan distribusi (hindari embedding anon key, tambah mekanisme rotasi token dan handling revoke).

## Tahapan Kerja
1. **Otentikasi**
   - Migrasikan `conn` ke Retrofit/OkHttp.
   - Gunakan Supabase Auth password grant dengan penyimpanan token aman (`EncryptedSharedPreferences`/`Jetpack DataStore`).
   - Sinkronkan token ke profil `customers` (fetch `users?select=*,customer:customers(*)`).

2. **Data Layer**
   - Definisikan model Java/Kotlin untuk paket, pelanggan, invoice, tiket, dan enumerasi status.
   - Buat repository dengan endpoint Supabase REST, dukung query parameter (filter per customer_id, status, ordering).
   - Siapkan error handling generik + mapper untuk UI state (loading/success/error).

3. **Fitur Riwayat (Fokus Saat Ini)**
   - Gunakan session user untuk menarik daftar invoice terbaru dan ringkasan status (total dibayar, tunggakan, status layanan).
   - Render list invoice/tiket di RecyclerView dengan adapter khusus, dukung empty-state dan indikator status.
   - Tambah ViewModel untuk pengelolaan state dan pemisahan logika jaringan dari UI.

4. **Fitur Pembayaran & Akun**
   - Pembayaran: tampilkan invoice due/overdue, tombol bayar (navigasi ke gateway atau bukti transfer).
   - Akun: detail pelanggan, paket aktif, progres onboarding, opsi logout.

5. **Keamanan & Infrastruktur**
   - Pindahkan Supabase anon key ke konfigurasi build atau backend proxy.
   - Tambah logging terstruktur (Timber) dan monitoring error.
   - Rencanakan notifikasi (tagihan jatuh tempo, status tiket) via FCM + Supabase triggers.

Dokumen ini menjadi acuan iterasi berikutnya; setiap fase dapat dipecah lagi dalam task list Jira/Trello sesuai prioritas.

