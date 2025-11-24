Halo Tim Mobile,

Endpoint Dashboard sudah selesai dikembangkan dengan fitur latest_payment untuk mendukung flow "Lanjutkan Pembayaran". Berikut detailnya:

  ---
Endpoint yang Tersedia

| Endpoint                                 | Method | Kegunaan                                   |
  |------------------------------------------|--------|--------------------------------------------|
| /api/v1/customer/dashboard.php           | GET    | Load Beranda (semua data + latest_payment) |
| /api/v1/payments/status.php?invoice_id=X | GET    | Refresh status payment saja                |

  ---
Flow yang Direkomendasikan

┌─────────────────────────────────────────────────────────────┐
│                      MOBILE APP FLOW                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│   1. USER BUKA APP / MASUK BERANDA                           │
│      └── GET /dashboard.php                                  │
│          └── Return: semua data (customer, package,          │
│              invoice, latest_payment)                        │
│                                                              │
│   2. USER TEKAN "REFRESH STATUS"                             │
│      └── GET /payments/status.php?invoice_id=X               │
│          └── Return: payment status only (lebih ringan)      │
│                                                              │
│   3. USER KEMBALI DARI SNAP WEBVIEW                          │
│      └── GET /payments/status.php?invoice_id=X               │
│          └── Cek apakah sudah settlement/paid                │
│                                                              │
└─────────────────────────────────────────────────────────────┘

  ---
1. Dashboard Endpoint

GET /api/v1/customer/dashboard.php
Authorization: Bearer {access_token}

Response:

{
"success": true,
"message": "Dashboard data retrieved successfully",
"data": {
"customer": {
"id": 16,
"name": "Leon Bari J",
"phone": "085756476554",
"status": "active|suspended|inactive"
},
"active_package": {
"id": 1,
"name": "Basic 20 Mbps",
"speed": "20 Mbps",
"price": 150000
},
"outstanding_invoice": {
"invoice_id": 10,
"amount": 90000,
"description": "Tagihan Langganan November 2025",
"due_date": "2025-12-10",
"status": "issued|overdue",
"days_until_due": 17,
"latest_payment": {
"payment_id": 11,
"invoice_id": 10,
"order_id": "INV-10-1763783062",
"status": "pending|settlement|expire|cancel|deny",
"preferred_channel": "qris|gopay|bca_va|...",
"payment_type": "qris|bank_transfer|...",
"redirect_url": "https://app.sandbox.midtrans.com/snap/v4/redirection/xxx",
"snap_token": "xxx-xxx-xxx",
"expires_at": "2025-11-22T03:59:22+00:00",
"created_at": "2025-11-22T03:44:24+00:00",
"va_numbers": [{"bank": "bca", "va_number": "123456"}],
"qr_string": "00020101021226...",
"bill_key": "123456",
"biller_code": "70012"
}
},
"summary": {
"total_outstanding": 90000,
"outstanding_count": 1,
"has_pending_payment": true
}
}
}

  ---
2. Payment Status Endpoint (Lightweight)

GET /api/v1/payments/status.php?invoice_id=10
Authorization: Bearer {access_token}

Response:

{
"success": true,
"message": "Payment is pending",
"data": {
"invoice_id": 10,
"invoice_status": "issued",
"invoice_amount": 90000,
"invoice_description": "Tagihan Langganan November 2025",
"due_date": "2025-12-10",
"paid_at": null,
"payment": {
"payment_id": 11,
"order_id": "INV-10-1763783062",
"status": "pending",
"preferred_channel": "qris",
"payment_type": "qris",
"transaction_id": null,
"settlement_time": null,
"expires_at": "2025-11-22T03:59:22+00:00",
"redirect_url": "https://app.sandbox.midtrans.com/snap/v4/redirection/xxx",
"va_numbers": [{"bank": "bca", "va_number": "123456"}],
"qr_string": "00020101021226..."
}
}
}

  ---
Logic untuk UI (Java)

// ══════════════════════════════════════════════════════════════
// 1. LOAD BERANDA - Panggil Dashboard
// ══════════════════════════════════════════════════════════════
private void loadDashboard() {
api.getDashboard(token, new Callback<DashboardResponse>() {
@Override
public void onSuccess(DashboardResponse dashboard) {
// Update UI customer info
txtCustomerName.setText(dashboard.customer.name);
txtPackageName.setText(dashboard.active_package.name);

              // Cek tagihan
              if (dashboard.outstanding_invoice != null) {
                  showInvoiceCard(dashboard.outstanding_invoice);
                  setupPaymentButton(dashboard.outstanding_invoice);
              } else {
                  hideInvoiceCard();
              }
          }
      });
}

// ══════════════════════════════════════════════════════════════
// 2. SETUP TOMBOL BAYAR
// ══════════════════════════════════════════════════════════════
private void setupPaymentButton(OutstandingInvoice invoice) {
LatestPayment payment = invoice.latest_payment;

      if (payment != null && payment.status.equals("pending")) {
          // Ada payment pending - tampilkan "Lanjutkan Pembayaran"
          btnPay.setText("Lanjutkan Pembayaran");
          btnPay.setOnClickListener(v -> {
              openSnapWebView(payment.redirect_url);
          });
      } else {
          // Tidak ada payment atau sudah expire/cancel
          btnPay.setText("Bayar Sekarang");
          btnPay.setOnClickListener(v -> {
              createNewPayment(invoice.invoice_id);
          });
      }
}

// ══════════════════════════════════════════════════════════════
// 3. SETELAH KEMBALI DARI SNAP - Refresh Status
// ══════════════════════════════════════════════════════════════
private void onReturnFromSnap(int invoiceId) {
// Gunakan endpoint ringan untuk cek status
api.getPaymentStatus(token, invoiceId, new Callback<PaymentStatusResponse>() {
@Override
public void onSuccess(PaymentStatusResponse response) {
if (response.payment != null) {
switch (response.payment.status) {
case "settlement":
showSuccessDialog("Pembayaran berhasil!");
loadDashboard(); // Refresh dashboard
break;
case "pending":
showInfoDialog("Menunggu pembayaran...");
break;
case "expire":
showErrorDialog("Pembayaran kedaluwarsa");
break;
case "cancel":
case "deny":
showErrorDialog("Pembayaran dibatalkan");
break;
}
}
}
});
}

// ══════════════════════════════════════════════════════════════
// 4. TOMBOL REFRESH STATUS (Manual)
// ══════════════════════════════════════════════════════════════
private void onRefreshStatusClicked(int invoiceId) {
btnRefresh.setEnabled(false);
showLoading();

      api.getPaymentStatus(token, invoiceId, new Callback<PaymentStatusResponse>() {
          @Override
          public void onSuccess(PaymentStatusResponse response) {
              hideLoading();
              btnRefresh.setEnabled(true);
              updatePaymentStatusUI(response);
          }

          @Override
          public void onError(String message) {
              hideLoading();
              btnRefresh.setEnabled(true);
              showError(message);
          }
      });
}

  ---
Status Payment Reference

| Status     | Deskripsi              | Aksi UI                             |
  |------------|------------------------|-------------------------------------|
| pending    | Menunggu pembayaran    | Resume via redirect_url             |
| settlement | Pembayaran berhasil    | Tampilkan sukses, refresh dashboard |
| expire     | Pembayaran kedaluwarsa | Buat payment baru                   |
| cancel     | Dibatalkan user        | Buat payment baru                   |
| deny       | Ditolak (fraud/limit)  | Buat payment baru                   |

  ---
Field Tambahan di latest_payment (Opsional)

| Field       | Kapan Ada     | Kegunaan                 |
  |-------------|---------------|--------------------------|
| va_numbers  | Bank Transfer | Menampilkan nomor VA     |
| qr_string   | QRIS          | Generate QR code lokal   |
| bill_key    | Mandiri Bill  | Nomor pembayaran Mandiri |
| biller_code | Mandiri Bill  | Kode biller Mandiri      |

  ---
Catatan Penting

1. Jangan buat payment baru jika status = pending → Gunakan redirect_url untuk resume
2. Cek expires_at → Jika sudah lewat, payment otomatis expire meskipun status masih pending
3. has_pending_payment: true → Shortcut untuk cek ada pending payment tanpa parsing detail
4. days_until_due → Negatif jika sudah lewat jatuh tempo (overdue)
5. Gunakan /payments/status.php untuk refresh ringan, /dashboard.php untuk load penuh

  ---
Test Endpoint

| Endpoint       | URL                                                                                                        |
  |----------------|------------------------------------------------------------------------------------------------------------|
| Dashboard      | https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/customer/dashboard.php            |
| Payment Status | https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/status.php?invoice_id=10 |
| Login          | https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/auth/login.php                    |
| Checkout       | https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/checkout.php             |

  ---
Jika ada pertanyaan atau butuh penyesuaian response structure, silakan hubungi tim web.

Terima kasih!