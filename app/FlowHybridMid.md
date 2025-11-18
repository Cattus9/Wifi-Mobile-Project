## 1\. User pilih metode di aplikasi kamu

- Halaman: **“Pilih Metode Pembayaran”** (UI kamu).

- User tap: **BRI / BCA / Mandiri / QRIS / dll**.

- App kirim ke server (API kamu), contoh payload:

  json

  Salin kode

  `{   "order_id": "INV-001",   "amount": 350000,   "selected_bank": "bri_va" }`


---

## 2\. Server buat transaksi Snap (dengan filter metode)

Di backend (pakai Server Key):

- Susun request ke Midtrans Snap, contoh:

  json

  Salin kode

  `{   "transaction_details": { "order_id": "INV-001", "gross_amount": 350000 },   "enabled_payments": ["bri_va"],   // pilih sesuai bank yang diklik user   "customer_details": { ... },   "expiry": { "unit": "hour", "duration": 24 } }`

- Midtrans balikin:

   - `token` (snap\_token)

   - `redirect_url`

- Server simpan transaksi ke database (status: **PENDING**).

- Server kirim `redirect_url` balik ke Android.


---

## 3\. Android buka Snap (Redirect)

Di app:

- Terima `redirect_url`.

- Buka dengan:

   - **Chrome Custom Tab** (disarankan), atau

   - **WebView**.


User sekarang lihat **halaman pembayaran Midtrans** (Snap), dengan metode yang sudah difilter (misal hanya BRI VA).

---

## 4\. User menyelesaikan pembayaran di Snap

- User ikuti instruksi (dapat VA / QR / dll).

- Setelah selesai / batal, Snap redirect ke:

   - `finish_url` / `pending_url` / `error_url` milik web/server kamu.

- Dari sisi app:

   - Bisa langsung kembali ke layar “Status Pembayaran”

   - Atau kamu deteksi callback URL (kalau pakai WebView).


---

## 5\. Server terima Payment Notification (webhook)

- Midtrans kirim **notifikasi** ke endpoint kamu:

   - `/midtrans/notification`

- Server update status order di DB:

   - `settlement` → **PAID**

   - `expire` → **EXPIRED**

   - `cancel` / `deny` → **FAILED**


---

## 6\. App menampilkan status akhir ke user

Di Android, setelah user kembali dari Snap:

- Tampilkan halaman **“Menunggu Pembayaran”**.

- App cek status ke server (polling berkala, misal tiap 5–10 detik) pakai `order_id`.

- Server jawab dengan status dari DB (PENDING/PAID/EXPIRED).

- Kalau status **PAID** → tampilkan **“Pembayaran Berhasil”**.

- Kalau **EXPIRED** / **FAILED** → tampilkan info + opsi **“Coba Lagi / Ganti Metode”**.


---

## Ringkasan super singkat (1 baris per langkah)

1. **User pilih bank di app** → kirim `selected_bank` + `order_id` ke server.

2. **Server panggil Snap** (`enabled_payments` sesuai pilihan) → dapat `redirect_url`.

3. **Android buka `redirect_url`** (Custom Tab/WebView).

4. **User bayar di Snap** → Midtrans redirect ke `finish_url`.

5. **Midtrans kirim webhook** → server update status order.

6. **App cek status ke server** → tampilkan “Berhasil / Expired / Gagal”.


Kalau kamu mau next step, kita bisa detailin **struktur tabel DB** dan **endpoint minimal** yang dibutuhkan:

- `/create-transaction`

- `/midtrans/notification`

- `/order-status?order_id=...`