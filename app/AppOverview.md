# Wifi Mobile App – Overview

Dokumen ini merangkum alur utama dan struktur fitur aplikasi Android **Wifi Mobile Project** agar setiap anggota tim memiliki gambaran menyeluruh.

## 1. Platform & Teknologi

- **Bahasa**: Java (Android SDK 36, minSdk 26).
- **UI**: XML layout + View Binding, Material Components, RecyclerView, Lottie.
- **Networking/Data**: OkHttp, org.json, Supabase REST & Auth.
- **State**: `SharedPreferences` (`LoginPrefs`, `SupabaseSession`) dan helper `TokenStorage`.

## 2. Flow Pengguna

1. **Splash → Login**
   - `SplashActivity` menampilkan animasi lalu membuka `LoginActivity`.
   - Jika `LoginPrefs.isLoggedIn == true`, login dilewati dan langsung ke dashboard (belum validasi token expiry).
2. **Autentikasi**
   - `LoginActivity` menggunakan `AuthRepository` → `SupabaseAuthService` untuk `signIn`.
   - Menyimpan session (access token, refresh token, expiry) di `TokenStorage` dan profil dasar (nama/email) di `SharedPreferences`.
   - `RegisterActivity` menyediakan form validasi lokal; belum terhubung backend.
3. **Dashboard (Bottom Navigation)**
   - `DashboardActivity` memuat lima tab utama dan menyimpan riwayat navigasi agar tombol back kembali ke tab sebelumnya.
4. **Fragmen isi**
   - **Beranda** (`BerandaFragment`): placeholder paket aktif dengan data dummy.
   - **Paket** (`PaketFragment`): memuat daftar paket dari Supabase (`service_packages`) via helper `conn.fetchServicePackages`, menampilkan card & membuka `DetailPaketFragment`.
   - **Riwayat** (`RiwayatFragment`): list dummy `PaymentHistoryItem` untuk invoice bulanan + ringkasan status pembayaran.
   - **Pembayaran** (`PembayaranFragment`): UI custom metode bayar (QRIS, VA bank). Integrasi Midtrans belum dihubungkan (lihat `PaymentIntegration.md`).
   - **Akun** (`AkunFragment`): menampilkan nama/email pengguna dari `LoginPrefs`, menyediakan logout (menghapus session + kembali ke Login).
5. **Detail & CS**
   - **Detail Paket** (`DetailPaketFragment`): menampilkan info lengkap, FAQ, terms, dan sembunyikan bottom nav sementara.
   - **CS** (`CsFragment`, `FormLaporanActivity`): layout dasar untuk formulir laporan, masih perlu wiring dan layout khusus.

## 3. Struktur Paket & Kelas

```
com.project.inet_mobile
├── data
│   ├── auth (AuthRepository, SupabaseAuthService, AuthSession, UserProfile, SignInResult, AuthException)
│   └── session (TokenStorage)
├── ui
│   ├── home (BerandaFragment)
│   ├── packages (PaketFragment + RecyclerView adapter via PaketAdapter & DetailPaketFragment)
│   ├── history (RiwayatFragment, RiwayatAdapter, PaymentHistoryItem)
│   ├── payment (PembayaranFragment + layout khusus)
│   ├── account (AkunFragment)
│   └── cs (CsFragment, FormLaporanActivity)
├── util (conn helper, MyApplication)
├── core Activities (Splash, Login, Register, Dashboard)
└── models (Paket.java)
```

## 4. Fitur Unggulan vs Status

| Fitur | Status Saat Ini | Catatan |
| --- | --- | --- |
| Splash screen | Selesai | Animasi fade-in logo + Lottie loading. |
| Login Supabase | Selesai (basic) | Perlu refresh token, error handling lanjutan. |
| Register | Form lokal | Belum kirim ke Supabase/API. |
| Dashboard + Bottom Nav | Selesai | History back stack kustom. |
| Beranda Paket Aktif | Dummy | Menunggu API paket aktif per pelanggan. |
| Daftar Paket Internet | Terhubung Supabase | Menggunakan helper `conn` + detail fragment. |
| Detail Paket | Selesai | Tombol beli belum mengarah ke checkout. |
| Riwayat Pembayaran | Dummy | Menunggu data invoice/payments nyata. |
| Pembayaran (UI) | Layout custom | Integrasi Midtrans masih via dokumen `PaymentIntegration.md`. |
| Akun & Logout | Selesai | Hanya nama/email; belum ada pengaturan lain. |
| Customer Service Form | Partial | Activity menggunakan layout fragment, masih perlu input & koneksi backend. |

## 5. Ketergantungan Layanan

- **Supabase Auth**: endpoint `/auth/v1/token`.
- **Supabase REST**: `/rest/v1/users` untuk profil, `/rest/v1/service_packages` untuk daftar paket.
- **Midtrans**: belum terintegrasi; spesifikasi ada di `PaymentIntegration.md`.

## 6. Area Pengembangan Berikutnya

1. **Manajemen Session**: refresh token, validasi expiry saat auto-login.
2. **Register & Onboarding**: koneksi ke Supabase/endpoint internal, mapping ke tabel `customers`.
3. **Pembayaran**: implementasi endpoint checkout + Snap redirect (lihat file integrasi).
4. **Riwayat**: tarik data nyata dari tabel `invoices`/`payments`.
5. **CS & Laporan**: buat schema ticket di backend (sudah ada di `SchemaSupabase.md`) dan hubungkan UI.
6. **Testing & Observability**: tambah unit/instrumentation test serta logging standar.

Dokumen ini akan diperbarui setiap kali ada perubahan flow atau fitur baru. Jika membutuhkan detail backend, rujuk `SchemaSupabase.md` dan `PaymentIntegration.md`. Silakan tambahkan catatan tambahan bila ada modul baru.
