# Payment Integration Hand-off

Dokumen ini ditujukan untuk tim **Web/API** agar implementasi pembayaran (Hybrid UI + Midtrans Snap Redirect) konsisten dengan kebutuhan Android app.

## 1. Tujuan

1. Menghasilkan endpoint checkout yang menggabungkan data `customers`, `users`, `service_packages`, `invoices`, dan tabel pembayaran baru.
2. Mengirim daftar metode pembayaran dinamis agar UI kustom di Android mengikuti konfigurasi server.
3. Mengorkestrasi Snap Redirect sekaligus menyimpan jejak transaksi untuk rekonsiliasi dan riwayat tagihan.

## 2. Data yang Harus Disiapkan

| Kategori | Field | Sumber | Keterangan |
| --- | --- | --- | --- |
| Identitas Pelanggan | `customer_id` | `customers.id` | Primary key relasi invoice |
|  | `customer_name` | `customers.name` | Nama yang ditampilkan & dikirim ke Midtrans |
|  | `customer_email` | `users.email` | Dibutuhkan untuk `customer_details` Snap |
|  | `customer_phone` | `customers.phone` | Opsional tapi disarankan |
| Paket/Produk | `package_id` | `service_packages.id` | Paket aktif pelanggan |
|  | `package_name` | `service_packages.name` | Tampil pada UI “Item” |
|  | `package_duration`/`speed` | `service_packages` | Untuk label paket |
| Invoice | `invoice_id` | `invoices.id` | PK internal |
|  | `invoice_number` | Format bebas (mis. `INV-YYYY-MM-###`) | Ditampilkan di teks “No. Invoice” |
|  | `gross_amount` | `invoices.amount` | Jumlah total yang akan dibayar |
|  | `amount_breakdown` | Hitungan server | Base price, pajak, biaya admin, diskon |
|  | `description` | `invoices.description` | Opsional |
|  | `due_date` | `invoices.due_date` | Untuk batas waktu pembayaran |
|  | `status` | `invoices.status` | Draft/Issued/Overdue/Paid |
| Metode Pembayaran | `available_payment_methods[]` | Konfigurasi server | Lihat detail §3 |
| Snap | `snap_token`, `redirect_url`, `expires_at`, `transaction_time`, `transaction_id`, `payment_type` | Response Midtrans | Harus disimpan & dikembalikan ke app |
| Tracking Pembayaran | `payments` table baru | Server | Menyimpan `invoice_id`, `snap_token`, `preferred_channel`, `status`, `fraud_status`, `settlement_time`, `va_numbers`, `qr_string`, dsb. |

> **Catatan**: `PaymentIntegration.md` ini tidak mengubah schema Supabase; tim Web bertanggung jawab menambah tabel/payment log sesuai kebutuhan backend.

## 3. Struktur `available_payment_methods[]`

Android akan menampilkan card untuk setiap objek berikut:

```json
{
  "channel_id": "bank_transfer_bri",
  "label": "BANK BRI",
  "sub_label": "Virtual Account",
  "icon_url": "https://.../logo_bri.png",
  "min_amount": 10000,
  "max_amount": 5000000,
  "is_preferred": false,
  "fee": 2500
}
```

Field wajib:

1. `channel_id` — string unik yang akan diteruskan sebagai `preferred_channel`.
2. `label` — teks utama card.
3. `icon_url` — atau fallback ke nama drawable jika offline (Android akan menyediakan mapping).
4. `is_available`/validations — optional flags (mis. jika jumlah melampaui limit).

## 4. Endpoint yang Dibutuhkan

### 4.1. `GET /invoices/{id}/payment`

- **Tujuan**: Mengisi layar `fragment_pembayaran`.
- **Respons minimal**:

```json
{
  "invoice": {
    "invoice_id": 123,
    "invoice_number": "INV-2025-11-001",
    "item_label": "Paket Premium - 1 Bulan",
    "gross_amount": 350000,
    "amount_breakdown": {
      "base": 315000,
      "tax": 35000,
      "admin_fee": 0,
      "discount": 0
    },
    "due_date": "2025-10-24T23:59:59+07:00",
    "status": "issued"
  },
  "customer": {
    "name": "Aang",
    "email": "aang@example.com",
    "phone": "+628xx",
    "address": "Jl. ..."
  },
  "available_payment_methods": [ ... ],
  "latest_payment": {
    "payment_id": 999,
    "status": "pending",
    "preferred_channel": "qris",
    "snap_token": "...",          // optional jika masih valid
    "redirect_url": "https://...",
    "expires_at": "2025-10-24T23:59:59+07:00"
  }
}
```

### 4.2. `POST /payments/checkout`

#### Request

```json
{
  "invoice_id": 123,
  "preferred_channel": "bank_transfer_bri",
  "return_url": "inet://payment-result"        // optional, untuk deep link balik
}
```

#### Langkah server

1. Validasi invoice (status != `paid`, amount > 0, belum kedaluwarsa).
2. Ambil data pelanggan + paket.
3. Tambahkan record baru di tabel `payments`.
4. Panggil Midtrans `POST /snap/v1/transactions` dengan payload:
   - `transaction_details`: `order_id`, `gross_amount`.
   - `item_details`: snapshot paket.
   - `customer_details`: name/email/phone.
   - `enabled_payments` atau `payment_type` berdasar `preferred_channel`.
   - `callbacks.finish`: `return_url` jika disediakan.
   - `expiry`: menggunakan `due_date`.
5. Simpan `snap_token`, `redirect_url`, `transaction_id`, `fraud_status`, `transaction_time`, `expires_at` ke tabel `payments`.

#### Respons

```json
{
  "order_id": "INV-2025-11-001",
  "snap_token": "...",
  "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/...",
  "gross_amount": 350000,
  "preferred_channel": "bank_transfer_bri",
  "expires_at": "2025-10-24T23:59:59+07:00",
  "invoice": { ... },
  "payment_id": 999
}
```

### 4.3. `GET /payments/{payment_id}`

- Mengembalikan status `pending|settlement|expire|cancel`, `settlement_time`, `va_numbers`, `qr_string`, `redirect_url` terbaru.
- Digunakan setelah user kembali dari Snap atau ketika membuka Riwayat.

### 4.4. Webhook Midtrans → `/payments/midtrans/callback`

- Terapkan validasi `signature_key`.
- Update tabel `payments` + `invoices.status` (`paid` jika settlement).
- Simpan `fraud_status`, `transaction_status`, `payment_type`, `va_numbers`, `expiry_time`.
- Trigger notifikasi/push jika perlu.

## 5. Kebutuhan Tabel Baru (Saran)

```sql
CREATE TABLE payments (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  order_id TEXT NOT NULL,
  preferred_channel TEXT,
  snap_token TEXT,
  redirect_url TEXT,
  transaction_id TEXT,
  payment_type TEXT,
  status TEXT NOT NULL DEFAULT 'pending',
  fraud_status TEXT,
  gross_amount NUMERIC(10,2) NOT NULL,
  expires_at TIMESTAMPTZ,
  settlement_time TIMESTAMPTZ,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

`metadata` dapat menyimpan `va_numbers`, `qr_string`, `biller_code`, dsb.

## 6. Flow Ringkas

1. App memanggil `GET /invoices/{id}/payment` → render UI.
2. User pilih metode → app kirim `POST /payments/checkout`.
3. Server create Snap transaction → response `snap_token` & `redirect_url`.
4. App buka Snap via redirect.
5. Midtrans callback ke server → server update status.
6. App polling `GET /payments/{id}` atau `GET /invoices/{id}/payment` untuk status akhir.

## 7. Checklist Tim Web

- [ ] Endpoint `GET /invoices/{id}/payment` tersedia.
- [ ] Endpoint `POST /payments/checkout` menghasilkan Snap token & menyimpan log.
- [ ] Endpoint `GET /payments/{id}` untuk status.
- [ ] Webhook Midtrans siap dengan verifikasi `signature_key`.
- [ ] Tabel `payments` atau struktur serupa dibuat.
- [ ] Konfigurasi `available_payment_methods` bisa dikelola tanpa update aplikasi.

Silakan hubungi tim Android jika ada perubahan payload supaya UI dapat disesuaikan sejak awal.
