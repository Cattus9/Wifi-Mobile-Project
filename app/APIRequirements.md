# API Requirements – Wifi Mobile App

Dokumen ini merinci kebutuhan API untuk seluruh fitur aplikasi. Login & registrasi memakai **Supabase Auth**, sementara endpoint lain dapat dibangun di service backend internal (mis. Supabase REST + Edge Functions).

## 1. Autentikasi

| Fitur | Endpoint | Keterangan |
| --- | --- | --- |
| Login | `POST https://<supabase-project>.supabase.co/auth/v1/token?grant_type=password` | Sudah dipakai `SupabaseAuthService`. |
| Logout | `POST https://<supabase-project>.supabase.co/auth/v1/logout` (optional) | Saat ini app hanya menghapus session lokal. |
| Refresh token | `POST /auth/v1/token?grant_type=refresh_token` | Belum diimplementasikan di app, tapi disarankan untuk jangka panjang. |
| Register | `POST /auth/v1/signup` | Bisa langsung Supabase atau via backend custom bila perlu validasi tambahan. |

## 2. Dashboard / Informasi Pelanggan

| Fitur | Method & Endpoint | Response Utama | Catatan |
| --- | --- | --- | --- |
| Profil pelanggan | `GET /customers/me` | `customer_id`, `name`, `email`, `phone`, `address`, `current_package`, `status` | Dipakai Beranda/Akun. Data bisa diambil dari tabel `users` + `customers`. |
| Paket aktif & statistik | `GET /customers/me/active-package` | `package_name`, `speed`, `quota_used`, `quota_limit`, `expiry_date`, `remaining_days` | Menggantikan data dummy di `BerandaFragment`. |
| Banner/announcement (opsional) | `GET /announcements?origin=mobile` | List message | Jika ingin menampilkan info tambahan di beranda. |

## 3. Paket Layanan

| Fitur | Method & Endpoint | Response Utama | Catatan |
| --- | --- | --- | --- |
| Daftar paket | `GET /service-packages?is_active=true` | Array `id`, `name`, `description`, `speed`, `duration`, `price`, `is_popular`, `quota`, `phone`, `original_price` | Saat ini diambil via helper `conn` langsung ke Supabase. Endpoint server bisa menambahkan caching/filter. |
| Detail paket | `GET /service-packages/{id}` | Info lengkap + FAQ/terms (opsional) | Saat ini detail diambil dari objek yang sama; endpoint custom bisa menyimpan FAQ/benefit. |
| CTA beli paket | `POST /customers/me/packages/change` | `order_id`, `status`, `message` | Tombol “Beli” di `DetailPaketFragment` nantinya mengarahkan user ke pembelian/pemasangan baru. |

## 4. Pembayaran

Gunakan spesifikasi dari `PaymentIntegration.md`. Ringkasannya:

| Fitur | Endpoint | Deskripsi |
| --- | --- | --- |
| Mendapatkan data pembayaran | `GET /invoices/{invoice_id}/payment` | Mengisi `fragment_pembayaran` (detail invoice + daftar metode). |
| Checkout Snap | `POST /payments/checkout` | Mengirim `invoice_id`, `preferred_channel`, menerima `snap_token`, `redirect_url`. |
| Status payment | `GET /payments/{payment_id}` atau `GET /invoices/{invoice_id}/payment` | Menampilkan status setelah user kembali dari Snap. |
| Webhook Midtrans | `POST /payments/midtrans/callback` | Server-side only; update status & invoice. |
| Daftar metode dinamis | `GET /payment-methods?context=mobile` (opsional) | Jika ingin UI selalu sinkron tanpa revisi app. Boleh digabung di endpoint payment detail. |

## 5. Riwayat Pembayaran

| Fitur | Endpoint | Response | Catatan |
| --- | --- | --- | --- |
| List invoice/payment | `GET /customers/me/payments?limit=12&offset=0` | Array `invoice_id`, `month_label`, `payment_date`, `method`, `status`, `amount`, `transaction_id`, `payment_type` | Mengganti dummy data di `RiwayatFragment`. |
| Detail invoice | `GET /invoices/{invoice_id}` | `items[]`, `amount_breakdown`, `status`, `paid_at`, `due_date` | Untuk future detail screen. |
| Bukti bayar (opsional) | `GET /payments/{payment_id}/receipt` | URL PDF / HTML | Dipakai jika user ingin download bukti. |

## 6. Akun & Pengaturan

| Fitur | Endpoint | Response | Catatan |
| --- | --- | --- | --- |
| Profil ringkas | `GET /customers/me` | Sudah termasuk di Dashboard. |
| Update profil | `PATCH /customers/me` | `name`, `phone`, `address` | Jika suatu saat app butuh edit data profil. |
| Logout global | `POST /auth/revoke` (opsional) | - | Jika ingin revoke session di server. |

## 7. Customer Service / Tiket

Sesuai schema `tickets` di `SchemaSupabase.md`.

| Fitur | Endpoint | Response | Catatan |
| --- | --- | --- | --- |
| List tiket | `GET /tickets?customer_id=me` | `id`, `subject`, `status`, `prioritas`, `updated_at` | Untuk daftar keluhan. |
| Detail tiket | `GET /tickets/{id}` | `subject`, `description`, `status`, `messages[]` | Dapat menampilkan riwayat chat. |
| Buat tiket | `POST /tickets` | `ticket_id`, `status` | Diisi dari form laporan (CS). |
| Kirim pesan | `POST /tickets/{id}/messages` | `message_id`, `timestamp` | Untuk follow-up. |
| Upload lampiran | `POST /tickets/{id}/attachments` | `attachment_id`, `url` | Jika form mendukung upload. |

## 8. Notifikasi & Misc (Opsional)

| Fitur | Endpoint | Catatan |
| --- | --- | --- |
| Push token register | `POST /devices/register` | Menyimpan FCM token untuk notifikasi invoice/pembayaran. |
| Pengumuman | `GET /announcements` | Untuk banner di home. |
| App config | `GET /app-config?platform=android` | Menyediakan flag remote (mis. toggle metode bayar). |

## 9. Ringkasan Mapping Fitur → API

| Fitur App | Endpoint wajib |
| --- | --- |
| Splash/Login | Supabase Auth (login + refresh). |
| Register | Supabase Auth signup atau endpoint custom. |
| Beranda (paket aktif) | `GET /customers/me`, `GET /customers/me/active-package`. |
| Daftar Paket | `GET /service-packages`. |
| Detail Paket | `GET /service-packages/{id}`. |
| Beli Paket | `POST /customers/me/packages/change` + flow pembayaran. |
| Riwayat Pembayaran | `GET /customers/me/payments`, `GET /invoices/{id}`. |
| Pembayaran | `GET /invoices/{id}/payment`, `POST /payments/checkout`, `GET /payments/{id}`. |
| Akun | `GET /customers/me`, `PATCH /customers/me`. |
| Logout | Supabase logout/refresh revoke (optional). |
| Customer Service | `GET/POST /tickets`, `GET/POST /tickets/{id}/messages`, attachments. |

Endpoint bisa dibangun di Supabase Edge Functions, Node/Go service, atau digabung dalam satu API gateway. Yang penting: setiap respons menyediakan data sesuai kebutuhan UI (lihat `AppOverview.md` untuk detail layout).

Silakan menyesuaikan nama domain/path sesuai arsitektur backend Anda. Jika ada perubahan payload, informasikan ke tim Android sebelum release.
