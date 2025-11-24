Halo Tim Mobile,

Berikut ringkasan eksekusi dan mapping penerapan endpoint Dashboard (dari Tim Web) + rencana status handling untuk Mobile.

==============================================================================
Endpoint yang Tersedia
==============================================================================
| Endpoint                                 | Method | Kegunaan                                   |
|------------------------------------------|--------|--------------------------------------------|
| /api/v1/customer/dashboard.php           | GET    | Load Beranda (customer, paket, invoice, latest_payment) |
| /api/v1/payments/status.php?invoice_id=X | GET    | Refresh status payment ringan              |

==============================================================================
Data Mapping → UI Dashboard
==============================================================================
- customer: name, phone, status → header profil pengguna.
- active_package: name, speed, price, is_active → kartu “Informasi Paket”.
- outstanding_invoice: invoice_id, invoice_number (jika ada), description, amount, due_date, status (issued|overdue), days_until_due.
  - Render ke kartu “Tagihan/Jatuh Tempo” (amount, due_date, status).
  - Jika null → sembunyikan kartu tagihan, tampilkan state “tidak ada tagihan”.
- outstanding_invoice.latest_payment (opsional):
  - payment_id, status (pending/settlement/expire/cancel/deny), preferred_channel, payment_type, redirect_url, snap_token, expires_at, created_at.
  - optional channel data: va_numbers[], qr_string, bill_key, biller_code.
  - UI: jika status pending → tombol “Lanjutkan Pembayaran” (open redirect_url). Jika settle/expire/cancel/deny/null → tombol “Bayar Sekarang” (buat sesi baru).
- summary: total_outstanding, outstanding_count, has_pending_payment → badge/label ringkas di header tagihan.

==============================================================================
Flow Eksekusi di Mobile (disarankan)
==============================================================================
1) Load Beranda
   - GET /customer/dashboard.php (dengan Authorization bearer Supabase).
   - Tampilkan customer + paket + tagihan (jika ada).
   - Jika latest_payment.status == pending dan belum expire → set tombol “Lanjutkan Pembayaran” (redirect_url).
   - Jika tidak ada payment pending → tombol “Bayar Sekarang” (POST checkout).

2) Setelah kembali dari Snap
   - GET /payments/status.php?invoice_id=X.
   - Jika settlement → tampilkan sukses, refresh Dashboard.
   - Jika pending → tampilkan “menunggu”, jangan buat sesi baru.
   - Jika expire/cancel/deny → beri opsi buat sesi baru.

3) Tombol “Refresh Status” (manual)
   - GET /payments/status.php?invoice_id=X untuk update ringan tanpa memuat ulang dashboard penuh.

==============================================================================
Status Handling (Front-end Plan)
==============================================================================
Status payment:
- pending → Tunjukkan instruksi “lanjutkan pembayaran”, tombol = redirect_url.
- settlement → Tunjukkan “Pembayaran berhasil”, disable checkout, refresh invoice status ke paid.
- expire / cancel / deny → Tampilkan pesan, aktifkan opsi “Bayar Sekarang” (sesi baru).

Status invoice (dari schema Supabase):
- issued → hanya status ini yang boleh bayar/checkout.
- overdue → tampilkan “Tagihan kedaluwarsa”, matikan tombol atau arahkan ke pembayaran baru jika server mengizinkan.
- paid → tampilkan “lunas”, nonaktifkan tombol.
- cancelled/draft → nonaktifkan tombol, tampilkan pesan status.

Expiry awareness:
- Gunakan expires_at dari latest_payment; jika waktu > now → perlakukan sebagai expire walau status masih pending.

==============================================================================
Logic Sample (ringkas)
==============================================================================
loadDashboard():
- panggil GET /customer/dashboard.php
- render kartu paket, tagihan, status
- if (latest_payment.pending && !expired) => btn “Lanjutkan Pembayaran” → open redirect_url
- else => btn “Bayar Sekarang” → checkout baru

onReturnFromSnap(invoiceId):
- GET /payments/status.php?invoice_id=invoiceId
- settlement => success UI + reload dashboard
- pending => info “menunggu”; tetap gunakan redirect_url jika user lanjut
- expire/cancel/deny => info gagal; izinkan sesi baru

==============================================================================
Test Endpoint (ngrok)
==============================================================================
Dashboard      : https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/customer/dashboard.php
Payment Status : https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/status.php?invoice_id=10
Login          : https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/auth/login.php
Checkout       : https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/checkout.php

==============================================================================
Catatan Penting
==============================================================================
1) Jangan buat payment baru jika latest_payment.status == pending dan belum expire → gunakan redirect_url.
2) Cek expires_at untuk override pending yang sudah kadaluarsa.
3) has_pending_payment bisa jadi shortcut untuk badge/indikator cepat.
4) days_until_due bisa negatif (overdue) → tampilkan label “Lewat jatuh tempo”.
5) Invoice hanya payable jika status = issued (atau aturan server mengizinkan overdue).

Jika ada penyesuaian struktur respons, kabari tim web. Terima kasih!
