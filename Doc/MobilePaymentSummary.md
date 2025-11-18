# Mobile Payment Integration – Android Client Recap

Date: 2025-11-16  
Owner: Mobile Team

---

## Retrofit Connectivity
- Added Retrofit/OkHttp stack with logging + Supabase-auth interceptor (`PaymentApiClient`, `AuthInterceptor`).  
- Config points to `https://<ngrok>/Form-Handling/api/v1/` so every request already carries the latest ngrok host.  
- DTOs implemented sesuai API docs: `CheckoutResponseData`, `PaymentStatusResponseData`, `InvoiceDetailResponseData`, `InvoiceListResponseData`.

## Invoice Handling
- Fragment now calls `GET /api/v1/invoices/detail?id=…` sebelum mengaktifkan tombol bayar. Fields yang terikat:
  - `invoice_number`, `amount`, `description`, `due_date`.
  - `can_pay=false` → tombol disable + status info.
  - `latest_payment` pending → auto-poll status ketika fragment muncul.
- Jika fragment tidak menerima `invoiceId`, fallback memanggil `GET /api/v1/invoices?status=issued` (limit 1). Ketika tidak ada invoice `issued`, lanjut ke `overdue`. Jika tetap kosong → menampilkan “Tidak ada tagihan yang bisa dibayar”.

## Payment Flow
1. Pengguna memilih metode (QRIS/BRI/BCA/Mandiri).  
2. Tombol “Lanjut ke Pembayaran” memanggil `POST /payments/checkout` dengan payload:
   ```json
   { "invoice_id": <current>, "preferred_channel": "<channel>", "return_url": "inet://payment-result" }
   ```
   - Jika server mengembalikan session existing (`is_existing=true`), fragment tetap pakai `redirect_url` itu.
3. Fragment membuka `redirect_url` lewat Chrome Custom Tab.  
4. Saat user kembali (onResume), fragment memanggil `GET /payments/status` dengan kombinasi `invoice_id` + `payment_id`. Status ditampilkan: `pending` (menunggu), `settlement` (sukses, disable tombol), `expire/cancel` (reset).

## Ownership & Security
- Semua request melewati `AuthInterceptor` → header `Authorization: Bearer <Supabase token>`.  
- Backend otomatis mem-filter invoice/pembayaran berdasarkan user token. Tidak ada input user ID manual di app, jadi hanya invoice milik user login yang pernah dipanggil.

## Outstanding Issue (per 2025-11-16)
- Pengujian terbaru dengan invoice 10 (akun pelanggan baru) masih gagal pada tahap checkout:
  - Request `POST /api/v1/payments/checkout.php` (body `{"invoice_id":10,"preferred_channel":"bank_transfer_mandiri","return_url":"inet://payment-result"}`) → Response `500 {"success":false,"message":"An unexpected error occurred","error_code":"SERVER_ERROR"}`.
  - Sebelumnya status `GET /payments/status.php?invoice_id=10&payment_id=2` menunjukkan payment lama `status=pending`, `preferred_channel="qris"`.
  - Karena backend kini auto-enable 8 channel jika tidak dikirim `preferred_channel`, mohon team web cek log `storage/logs/midtrans.log` sekitar `2025-11-16 21:25:05` (ngrok GMT 14:25) untuk melihat detail error yang dilempar Midtrans saat request masuk.
  - Jika invoice 10 masih mengarah ke payment lama (id 2, pending), mohon dibersihkan agar checkout baru menghasilkan Snap token baru (bukan `is_existing=true`). Saat ini, percobaan checkout hanya mengembalikan session lama atau 500.

## To‑Do / Notes
- Activity pen-launch Pembayaran perlu meneruskan `invoiceId` jika invoice spesifik dipilih, supaya fallback tidak perlu men-scan list.  
- Pastikan `ApiConfig.BASE_URL` dan network security config diperbarui setiap kali ngrok berubah.  
- Testing dilakukan via Postman: 4 endpoint (list detail invoice, checkout, status) sudah verified – JSON cocok dengan dokumen backend.

---

Handover siap ke tim web/backend untuk confirm bahwa mobile client sudah mengikuti kontrak API terbaru.
