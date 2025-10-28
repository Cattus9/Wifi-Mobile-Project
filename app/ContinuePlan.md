# Catatan Lanjutan Sesi Pengembangan

1. **Rencana Fondasi (mengacu pada `Plan.md`)**
   - Migrasi autentikasi ke Supabase Auth lengkap (signup, signin, refresh, logout) dan hubungkan ke tabel `customers`.
   - Bangun data layer (repository + model) memakai OkHttp/Retrofit + Gson untuk `service_packages`, `customers`, `invoices`, `tickets`.
   - Prioritas fitur: Riwayat (billing & tiket) → Pembayaran (CTA bayar, status invoice) → Akun (profil pelanggan). Lengkapi dengan ViewModel serta state Loading/Success/Error.
   - Tingkatkan keamanan distribusi (hindari hardcode anon key, siapkan rotasi token, logging/monitoring, rencana notifikasi via FCM + Supabase trigger).

2. **Instruksi UI Riwayat**
   - Background utama tetap biru (`register_primary`). Di atasnya ada kontainer putih (`fragment_card`) selebar layar dengan sudut atas melengkung sebagai wadah seluruh komponen Riwayat (highlight + daftar).
   - Card highlight menampilkan total pembayaran, status layanan, tunggakan. Semua teks di dalam kontainer putih menggunakan warna teks gelap (hitam/abu tua) kecuali elemen yang memang butuh kontras khusus.
   - Chip “Lunas” harus berbentuk rounded dengan latar hijau muda dan teks hijau tua sesuai guideline tim desain.
   - RecyclerView untuk daftar riwayat berada di dalam kontainer putih yang sama, menampilkan card berteks hitam dengan meta “Pembayaran [Bulan]”, “tanggal • metode • invoice”, serta badge status berwarna (PAID/OVERDUE/DRAFT).

3. **Integrasi Data Riwayat**
   - Ketika sesi user sudah tersedia, Riwayat harus mengambil invoice/tiket berdasarkan `customer_id` aktif melalui Supabase REST (`/rest/v1/invoices?customer_id=eq.{id}` dll).
   - Ringkasan highlight: jumlah total bayar periode berjalan, bulan terakhir lunas, status layanan dari `customers.status`, serta nominal tunggakan (akumulasi invoice `overdue`).
   - Sementara session belum tersedia, gunakan data dummy sebagai placeholder tetapi jaga struktur repository/ViewModel agar mudah diganti data nyata.

4. **Checklist Kelanjutan**
   - Sesuaikan layout sesuai instruksi di atas (parent biru + kontainer putih, warna teks, chip hijau).
   - Tambahkan warna baru untuk chip lunas (latar hijau muda + teks hijau tua) pada `colors.xml`.
   - Pastikan card riwayat menggunakan `MaterialCardView` dengan teks hitam dan badge status yang bisa ditint sesuai status invoice.
   - Setelah UI konsisten, lanjutkan implementasi ViewModel + repository Supabase untuk Riwayat sebelum berpindah ke fitur Pembayaran/Akun.

Catatan ini dimaksudkan sebagai referensi singkat ketika sesi dilanjutkan pada terminal/lingkungan berbeda.
- Catatan: Tidak perlu menampilkan Perangkat & Koneksi pada profil karena aplikasi belum mendukung fitur tersebut.
