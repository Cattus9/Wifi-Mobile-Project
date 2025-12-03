# Invoice Receipt API Documentation

Documentation untuk endpoint Invoice Receipt yang menyediakan data lengkap untuk menampilkan bukti pembayaran di aplikasi mobile.

---

## Endpoints

### 1. Invoice Detail (untuk Receipt)

**Endpoint:**
```
GET /api/v1/invoices/detail.php?id={invoice_id}
```

**Authorization:**
```
Authorization: Bearer {access_token}
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | integer | Yes | Invoice ID |

**Response Success (200):**

```json
{
  "success": true,
  "message": "Invoice retrieved successfully",
  "data": {
    "invoice_id": 10,
    "invoice_number": "INV-2025-11-0010",
    "month_label": "Desember 2025",
    "amount": 90000,
    "due_date": "2025-12-10",
    "status": "paid",
    "description": "Tagihan Langganan November 2025 (Prorata)",
    "created_at": "2025-11-13T01:01:25+00:00",
    "paid_at": "2025-11-23T16:01:00+00:00",
    "paid_at_formatted": "23-11-2025 16:01",
    "is_overdue": false,
    "can_pay": false,
    "customer": {
      "id": 16,
      "name": "Leon Bari J",
      "email": "leon@gmail.com",
      "phone": "085756476554",
      "address": "Rambi",
      "status": "active"
    },
    "package": {
      "id": 1,
      "name": "Basic 20 Mbps",
      "description": "Paket entry level untuk pelanggan baru",
      "speed": "20 Mbps",
      "price": 150000
    },
    "payment_methods": [
      {
        "type": "qris",
        "label": "QRIS",
        "description": "Scan QR dengan aplikasi e-wallet",
        "icon": "qris",
        "fee": 0
      }
    ],
    "latest_payment": {
      "payment_id": 13,
      "order_id": "INV-10-1763872460",
      "status": "cancel",
      "preferred_channel": "qris",
      "expires_at": "2025-11-23T04:49:20+00:00",
      "redirect_url": "https://app.sandbox.midtrans.com/snap/v4/redirection/xxx",
      "created_at": "2025-11-23T04:34:22.421873+00:00"
    }
  }
}
```

**Response Error (404):**
```json
{
  "success": false,
  "message": "Invoice not found",
  "error_code": "NOT_FOUND"
}
```

---

### 2. Dashboard (dengan Invoice Info)

**Endpoint:**
```
GET /api/v1/customer/dashboard.php
```

**Authorization:**
```
Authorization: Bearer {access_token}
```

**Response Success (200):**

```json
{
  "success": true,
  "message": "Dashboard data retrieved successfully",
  "data": {
    "customer": {
      "id": 16,
      "name": "Leon Bari J",
      "phone": "085756476554",
      "address": "Rambi",
      "status": "active"
    },
    "active_package": {
      "id": 1,
      "name": "Basic 20 Mbps",
      "description": "Paket entry level untuk pelanggan baru",
      "speed": "20 Mbps",
      "price": 150000
    },
    "outstanding_invoice": {
      "invoice_id": 10,
      "invoice_number": "INV-2025-11-0010",
      "amount": 90000,
      "description": "Tagihan Langganan November 2025",
      "due_date": "2025-12-10",
      "status": "issued",
      "paid_at": null,
      "paid_at_formatted": null,
      "days_until_due": 17,
      "latest_payment": {
        "payment_id": 16,
        "invoice_id": 10,
        "order_id": "INV-10-1763910764",
        "status": "pending",
        "preferred_channel": "qris",
        "payment_type": null,
        "redirect_url": "https://app.sandbox.midtrans.com/snap/v4/redirection/xxx",
        "snap_token": "xxx-xxx-xxx",
        "expires_at": "2025-11-23T15:27:44+00:00",
        "created_at": "2025-11-23T15:12:46+00:00"
      }
    },
    "summary": {
      "total_outstanding": 90000,
      "outstanding_count": 1,
      "has_pending_payment": true
    }
  }
}
```

---

## Field Descriptions

### Invoice Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `invoice_id` | integer | ID invoice di database | `10` |
| `invoice_number` | string | Nomor invoice (format: INV-YYYY-MM-{id}) | `"INV-2025-11-0010"` |
| `month_label` | string | Label bulan untuk UI | `"Desember 2025"` |
| `amount` | float | Jumlah tagihan | `90000` |
| `due_date` | string (date) | Tanggal jatuh tempo | `"2025-12-10"` |
| `status` | string | Status invoice | `"paid"`, `"issued"`, `"overdue"`, `"cancelled"` |
| `description` | string | Deskripsi tagihan | `"Tagihan Langganan November 2025"` |
| `created_at` | string (ISO 8601) | Tanggal dibuat | `"2025-11-13T01:01:25+00:00"` |
| `paid_at` | string (ISO 8601) | Tanggal dibayar (null jika belum) | `"2025-11-23T16:01:00+00:00"` |
| `paid_at_formatted` | string | Format Indonesia (dd-mm-yyyy HH:mm) | `"23-11-2025 16:01"` |
| `is_overdue` | boolean | Apakah sudah lewat jatuh tempo | `false` |
| `can_pay` | boolean | Apakah bisa dibayar | `false` |

### Customer Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | integer | Customer ID | `16` |
| `name` | string | Nama customer | `"Leon Bari J"` |
| `email` | string | Email customer | `"leon@gmail.com"` |
| `phone` | string | Nomor telepon | `"085756476554"` |
| `address` | string | Alamat lengkap | `"Rambi"` |
| `status` | string | Status customer | `"active"`, `"suspended"`, `"inactive"` |

### Package Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `id` | integer | Package ID | `1` |
| `name` | string | Nama paket | `"Basic 20 Mbps"` |
| `description` | string | Deskripsi paket | `"Paket entry level untuk pelanggan baru"` |
| `speed` | string | Kecepatan internet | `"20 Mbps"` |
| `price` | float | Harga bulanan | `150000` |

### Latest Payment Fields

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `payment_id` | integer | Payment ID | `13` |
| `order_id` | string | Order ID untuk tracking | `"INV-10-1763872460"` |
| `status` | string | Status payment | `"pending"`, `"settlement"`, `"expire"`, `"cancel"` |
| `preferred_channel` | string | Metode pembayaran | `"qris"`, `"gopay"`, `"bca_va"` |
| `redirect_url` | string | URL Snap Midtrans | `"https://app.sandbox.midtrans.com/..."` |
| `expires_at` | string (ISO 8601) | Waktu kadaluarsa payment | `"2025-11-23T15:27:44+00:00"` |

---

## Invoice Number Format

Format nomor invoice: `INV-YYYY-MM-{id}`

**Contoh:**
- Invoice ID 10 created November 2025 → `INV-2025-11-0010`
- Invoice ID 123 created December 2025 → `INV-2025-12-0123`

**Format:**
```
INV-2025-11-0010
│   │    │  └──── Invoice ID (4 digit, zero-padded)
│   │    └─────── Bulan pembuatan invoice
│   └──────────── Tahun pembuatan invoice
└──────────────── Prefix tetap
```

---

## Date Format

API mengirimkan 2 format tanggal:

| Field | Format | Kegunaan |
|-------|--------|----------|
| `paid_at` | ISO 8601 (`yyyy-MM-dd'T'HH:mm:ssXXX`) | Untuk parsing di mobile/backend |
| `paid_at_formatted` | Indonesia (`dd-mm-yyyy HH:mm`) | Untuk display ke user |

**Contoh:**
- `paid_at`: `"2025-11-23T16:01:00+00:00"`
- `paid_at_formatted`: `"23-11-2025 16:01"`

---

## Use Case: Invoice Receipt UI

Berdasarkan mockup UI mobile (lihat `images/Mobile/Invoice1.png` dan `Invoice2.png`):

```
┌──────────────────────────────────────────────────────┐
│  📄 INVOICE                    [✓ SUDAH BAYAR]        │
├──────────────────────────────────────────────────────┤
│  No: JBR-01A-14621-RR-BRONZE                         │
│  (Gunakan: invoice_number dari API)                  │
├──────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────────────┐   │
│  │ Beri Berwanto   │  │ INDONESIA               │   │
│  │ ID 2630325066   │  │ 11 Dec 05:01:11c        │   │
│  │                 │  │                         │   │
│  │ 081419720       │  │ Islamic Spiritual...    │   │
│  │ 3                │  │ Gg Wurarah Blok         │   │
│  │ Sleman          │  │ Kc VI / Jl Setra        │   │
│  │                 │  │                         │   │
│  │ 000004@mail     │  │ 12.VI / 2021            │   │
│  └─────────────────┘  └─────────────────────────┘   │
│  (customer fields)     (package & address)           │
├──────────────────────────────────────────────────────┤
│  JBR-01A-14621-RR-BRONZE                             │
│  Pembayaran Internet Jatuh Tempo                      │
│  15 Oct 2025     1 Bulan     Rp 86.000               │
│  (due_date)      (period)    (amount)                │
├──────────────────────────────────────────────────────┤
│  Biayalain         Rp 25.000                         │
│  PPN               Rp    000                          │
│  TOTAL PEMBAYARAN  Rp 111.000                        │
│  (amount breakdown + total)                          │
├──────────────────────────────────────────────────────┤
│  Informasi Transaksi                                  │
│  TANGGAL  METODE    STATUS   AMOUNT                  │
│  15 Oct   VIRTUAL   PAID     Rp 25.000 & 0.00       │
│  2025              123678                            │
│  (paid_at_formatted) (preferred_channel)             │
└──────────────────────────────────────────────────────┘
```

**Mapping API Response ke UI:**

| UI Element | API Field | Example |
|------------|-----------|---------|
| Invoice Number (Header) | `invoice_number` | `"INV-2025-11-0010"` |
| Customer Name | `customer.name` | `"Leon Bari J"` |
| Customer ID | `customer.id` | `16` |
| Phone | `customer.phone` | `"085756476554"` |
| Address | `customer.address` | `"Rambi"` |
| Email | `customer.email` | `"leon@gmail.com"` |
| Package Name | `package.name` | `"Basic 20 Mbps"` |
| Package Description | `package.description` | `"Paket entry level..."` |
| Due Date | `due_date` | `"2025-12-10"` |
| Amount | `amount` | `90000` |
| Status Badge | `status` | `"paid"`, `"issued"` |
| Payment Date | `paid_at_formatted` | `"23-11-2025 16:01"` |
| Payment Method | `latest_payment.preferred_channel` | `"qris"` |

---

## Implementation Example (Java/Kotlin)

### Fetch Invoice Detail

**Java:**
```java
// Retrofit Interface
@GET("/api/v1/invoices/detail.php")
Call<InvoiceDetailResponse> getInvoiceDetail(
    @Header("Authorization") String token,
    @Query("id") int invoiceId
);

// Usage
apiService.getInvoiceDetail("Bearer " + token, invoiceId)
    .enqueue(new Callback<InvoiceDetailResponse>() {
        @Override
        public void onResponse(Call<InvoiceDetailResponse> call, Response<InvoiceDetailResponse> response) {
            if (response.isSuccessful() && response.body() != null) {
                InvoiceData data = response.body().getData();

                // Display invoice number
                txtInvoiceNumber.setText(data.getInvoiceNumber());

                // Display paid date (formatted)
                txtPaidDate.setText(data.getPaidAtFormatted());

                // Display customer info
                txtCustomerName.setText(data.getCustomer().getName());
                txtCustomerAddress.setText(data.getCustomer().getAddress());

                // Display package info
                txtPackageName.setText(data.getPackage().getName());
                txtPackageDesc.setText(data.getPackage().getDescription());
            }
        }

        @Override
        public void onFailure(Call<InvoiceDetailResponse> call, Throwable t) {
            // Handle error
        }
    });
```

**Kotlin:**
```kotlin
// Suspend function with Coroutines
suspend fun getInvoiceDetail(invoiceId: Int): InvoiceDetailResponse {
    return apiService.getInvoiceDetail("Bearer $token", invoiceId)
}

// Usage in ViewModel
viewModelScope.launch {
    try {
        val response = repository.getInvoiceDetail(invoiceId)
        val data = response.data

        _invoiceNumber.value = data.invoiceNumber
        _paidDate.value = data.paidAtFormatted
        _customerName.value = data.customer.name
    } catch (e: Exception) {
        // Handle error
    }
}
```

---

## Testing

### Base URL
- **Production:** `https://your-domain.com/Form-Handling`
- **Sandbox (Ngrok):** `https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling`

### Test Credentials
| Email | Password | Status | Invoice |
|-------|----------|--------|---------|
| `ramadhan@wifi.local` | `Admin123` | Active | Paid |
| `leon@gmail.com` | `Admin123` | Active | Paid |

### Postman Collection

**1. Login:**
```
POST {{base_url}}/api/v1/auth/login.php
Content-Type: application/json

{
  "email": "ramadhan@wifi.local",
  "password": "Admin123"
}
```

**2. Invoice Detail:**
```
GET {{base_url}}/api/v1/invoices/detail.php?id=10
Authorization: Bearer {{access_token}}
```

**3. Dashboard:**
```
GET {{base_url}}/api/v1/customer/dashboard.php
Authorization: Bearer {{access_token}}
```

---

## Error Handling

| HTTP Code | Error Code | Message | Action |
|-----------|------------|---------|--------|
| 401 | `UNAUTHORIZED` | Missing or invalid token | Re-login |
| 403 | `FORBIDDEN` | Access denied | User tidak punya akses ke invoice ini |
| 404 | `NOT_FOUND` | Invoice not found | Invoice ID tidak valid |
| 422 | `VALIDATION_ERROR` | Validation failed | Cek parameter request |
| 500 | `DATABASE_ERROR` | Server error | Retry atau hubungi admin |

---

## Notes

1. **Field `paid_at_formatted` akan `null`** jika invoice belum dibayar (`status` != `"paid"`)
2. **Field `latest_payment`** akan ada jika pernah ada transaksi pembayaran (meskipun gagal/cancel)
3. **Invoice number format** konsisten di semua endpoint (Dashboard & Invoice Detail)
4. **Customer address** mungkin `null` jika belum diisi di database
5. **Package description** mungkin `null` jika tidak ada di service_packages table

---

## Changelog

### Version 1.1 (2025-11-23)
- ✅ Added `invoice_number` dengan format baru `INV-YYYY-MM-{id}`
- ✅ Added `customer.address` untuk billing address
- ✅ Added `package.description` untuk detail layanan
- ✅ Added `paid_at_formatted` dengan format Indonesia `dd-mm-yyyy HH:mm`
- ✅ Updated dashboard endpoint dengan field yang sama

### Version 1.0 (Initial)
- Invoice detail endpoint
- Dashboard endpoint
- Basic invoice & payment info

---

## Support

Jika ada pertanyaan atau butuh penambahan field, silakan hubungi Tim Web.

**Tim Web Contact:**
- Repository: `Form-Handling`
- Endpoint Base: `/api/v1/`
