# Mobile Payment API - Testing Guide

> **Status:** Ready for Testing
> **Last Updated:** 2025-11-15
> **Endpoints Implemented:** 2/2 (checkout, status)

---

## 📋 **WHAT'S BEEN IMPLEMENTED**

### **1. PaymentRepository** ✅
**File:** `src/Supabase/PaymentRepository.php`

**Methods:**
- `findByOrderId()` - Webhook lookup
- `findById()` - Get payment by ID
- `findLatestByInvoice()` - Get latest payment for invoice
- `listByCustomer()` - Payment history (for future)
- `create()` - Create payment record
- `update()` - Update payment record
- `updateByOrderId()` - Webhook update
- `findPendingExpiring()` - Cleanup job (for future)
- `countByCustomer()` - Statistics (for future)

---

### **2. POST /api/v1/payments/checkout** ✅
**File:** `api/v1/payments/checkout.php`

**Features:**
- ✅ JWT authentication (customer only)
- ✅ Invoice validation (exists, belongs to customer, not paid)
- ✅ Duplicate payment prevention (return existing if still valid)
- ✅ Midtrans Snap token creation (redirect mode)
- ✅ Payment method filtering (`preferred_channel`)
- ✅ Deep link support (`return_url`)
- ✅ Payment record tracking in database
- ✅ Expiry time calculation (24h or due_date)
- ✅ Comprehensive error handling

**Request:**
```json
POST /api/v1/payments/checkout
Authorization: Bearer {access_token}
Content-Type: application/json

{
  "invoice_id": 123,
  "preferred_channel": "qris",
  "return_url": "inet://payment-result"
}
```

**Response Success (201):**
```json
{
  "success": true,
  "message": "Payment session created successfully",
  "data": {
    "payment_id": 456,
    "order_id": "INV-20251115-000123",
    "snap_token": "abc123xyz...",
    "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/abc123xyz",
    "gross_amount": 350000.00,
    "preferred_channel": "qris",
    "expires_at": "2025-11-16 14:30:00",
    "invoice": {
      "id": 123,
      "description": "Paket Premium - November 2025",
      "due_date": "2025-11-30"
    }
  }
}
```

---

### **3. GET /api/v1/payments/status** ✅
**File:** `api/v1/payments/status.php`

**Features:**
- ✅ JWT authentication (customer only)
- ✅ Dual query support (invoice_id OR payment_id)
- ✅ Payment ownership verification
- ✅ Invoice status tracking
- ✅ Payment details (VA numbers, QR string, deeplink)
- ✅ Metadata parsing (JSONB)
- ✅ Comprehensive status messages

**Request (by invoice_id):**
```http
GET /api/v1/payments/status?invoice_id=123
Authorization: Bearer {access_token}
```

**Request (by payment_id):**
```http
GET /api/v1/payments/status?payment_id=456
Authorization: Bearer {access_token}
```

**Response Success (200):**
```json
{
  "success": true,
  "message": "Payment successful",
  "data": {
    "invoice_id": 123,
    "invoice_status": "paid",
    "invoice_amount": 350000.00,
    "paid_at": "2025-11-15 14:35:00",
    "payment": {
      "payment_id": 456,
      "order_id": "INV-20251115-000123",
      "status": "settlement",
      "payment_type": "qris",
      "preferred_channel": "qris",
      "transaction_id": "TXN-20251115-ABC123",
      "settlement_time": "2025-11-15 14:35:00",
      "qr_string": "00020101021226670016COM.NOBUBANK.WWW..."
    }
  }
}
```

---

### **4. Webhook Update** ✅
**File:** `api/midtrans/notification.php`

**Features:**
- ✅ Support 2 order_id formats (web & mobile)
- ✅ Payment tracking update (if record exists)
- ✅ Metadata storage (VA, QR, deeplink)
- ✅ Status mapping to payment_status enum
- ✅ Settlement time tracking
- ✅ Backward compatible (web payments tidak terpengaruh)
- ✅ Error logging (tidak gagalkan webhook)

---

## 🧪 **TESTING CHECKLIST**

### **Pre-Testing Setup**

```bash
# 1. Verify table payments exists
# Login ke Supabase SQL Editor dan jalankan:
SELECT * FROM payments LIMIT 1;

# 2. Verify .env configuration
MIDTRANS_SERVER_KEY=SB-Mid-server-xxxxx
MIDTRANS_CLIENT_KEY=SB-Mid-client-xxxxx
MIDTRANS_IS_PRODUCTION=false
MIDTRANS_ENABLE_3DS=true
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_SERVICE_ROLE_KEY=xxx
```

---

### **Test 1: Create Payment Session (Checkout)**

**Objective:** Mobile app create Snap token untuk payment

**Steps:**

1. **Get Access Token** (Login via Supabase Auth)
   ```bash
   # Login sebagai customer test
   # Simpan access_token dari response
   ```

2. **Get Invoice ID**
   ```sql
   -- Cari invoice customer yang belum paid
   SELECT i.id, i.customer_id, i.amount, i.status, i.description
   FROM invoices i
   WHERE i.status IN ('issued', 'overdue')
   LIMIT 5;
   ```

3. **Call Checkout API**
   ```bash
   curl -X POST http://localhost/Form-Handling/api/v1/payments/checkout \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "invoice_id": 123,
       "preferred_channel": "qris",
       "return_url": "inet://payment-result"
     }'
   ```

**Expected Results:**
- ✅ Status: 201 Created
- ✅ Response contains: snap_token, redirect_url, payment_id, order_id
- ✅ Record inserted ke table payments:
  ```sql
  SELECT * FROM payments WHERE invoice_id = 123 ORDER BY created_at DESC LIMIT 1;
  ```
- ✅ Status payment = 'pending'
- ✅ order_id format = 'INV-20251115-000123'
- ✅ expires_at terisi (24 jam dari now)

**Error Cases to Test:**
```bash
# Test 1: Invalid invoice_id
curl ... -d '{"invoice_id": 99999, "preferred_channel": "qris"}'
# Expected: 404 Not Found

# Test 2: Invoice belongs to different customer
curl ... -d '{"invoice_id": [invoice_milik_customer_lain]}'
# Expected: 403 Forbidden

# Test 3: Invoice already paid
curl ... -d '{"invoice_id": [invoice_yang_sudah_paid]}'
# Expected: 422 Validation Error

# Test 4: Missing access token
curl -X POST ... (without -H "Authorization: Bearer ...")
# Expected: 401 Unauthorized

# Test 5: Duplicate checkout (call same invoice 2x)
# Call checkout 2x dengan invoice_id yang sama
# Expected: Return existing payment (is_existing: true)
```

---

### **Test 2: Payment via Midtrans Snap**

**Objective:** User bayar via Midtrans dan webhook update database

**Steps:**

1. **Open Snap URL** (dari response Test 1)
   ```
   redirect_url: https://app.sandbox.midtrans.com/snap/v2/vtweb/abc123xyz
   ```

2. **Complete Payment di Midtrans**
   - Pilih payment method (QRIS, Bank Transfer, etc)
   - Ikuti flow pembayaran Midtrans
   - Di sandbox, bisa langsung "Pay Now" tanpa bayar real

3. **Wait for Webhook**
   - Midtrans akan kirim notification ke:
     ```
     POST http://your-domain.com/Form-Handling/api/midtrans/notification.php
     ```
   - Webhook akan auto-update:
     - `invoices.status` = 'paid'
     - `invoices.paid_at` = NOW()
     - `payments.status` = 'settlement'
     - `payments.transaction_id` = Midtrans transaction ID
     - `payments.metadata` = VA numbers, QR string, dll

**Expected Results:**
- ✅ Invoice status berubah jadi 'paid'
  ```sql
  SELECT status, paid_at FROM invoices WHERE id = 123;
  ```
- ✅ Payment status berubah jadi 'settlement'
  ```sql
  SELECT status, transaction_id, settlement_time, metadata
  FROM payments
  WHERE order_id = 'INV-20251115-000123';
  ```
- ✅ Metadata terisi dengan payment details
- ✅ settlement_time terisi

**Check Logs:**
```bash
# Cek webhook logs
tail -f storage/logs/midtrans.log

# Expected log entries:
# [2025-11-15 14:35:00] Webhook Success: Payment updated: INV-20251115-000123 -> settlement
```

---

### **Test 3: Check Payment Status (Mobile App)**

**Objective:** Mobile app cek status payment setelah redirect dari Midtrans

**Steps:**

1. **Call Status API by Invoice ID**
   ```bash
   curl -X GET "http://localhost/Form-Handling/api/v1/payments/status?invoice_id=123" \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
   ```

2. **Call Status API by Payment ID**
   ```bash
   curl -X GET "http://localhost/Form-Handling/api/v1/payments/status?payment_id=456" \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
   ```

**Expected Results (Paid Invoice):**
- ✅ Status: 200 OK
- ✅ invoice_status = 'paid'
- ✅ payment.status = 'settlement'
- ✅ settlement_time terisi
- ✅ transaction_id terisi
- ✅ message = 'Invoice has been paid'

**Expected Results (Pending Payment):**
- ✅ Status: 200 OK
- ✅ invoice_status = 'issued'
- ✅ payment.status = 'pending'
- ✅ redirect_url masih bisa digunakan (belum expire)
- ✅ message = 'Payment is pending'

**Error Cases:**
```bash
# Test 1: Invalid invoice_id
curl ... "?invoice_id=99999"
# Expected: 404 Not Found

# Test 2: Invoice belongs to different customer
curl ... "?invoice_id=[invoice_milik_customer_lain]"
# Expected: 403 Forbidden

# Test 3: Missing both parameters
curl ... (no query params)
# Expected: 422 Validation Error
```

---

### **Test 4: Web Payment Compatibility**

**Objective:** Pastikan web payment tetap jalan normal (backward compatibility)

**Steps:**

1. **Create Web Payment** (via existing web interface)
   - Login sebagai customer di web
   - Klik "Bayar" di invoice
   - Complete payment via Snap popup

2. **Check Webhook Processing**
   - Webhook harus update invoice.status = 'paid'
   - Webhook TIDAK create payment record (karena order_id format berbeda)

3. **Verify Logs**
   ```bash
   tail -f storage/logs/midtrans.log

   # Expected:
   # [2025-11-15] Webhook Info: No payment record for: invoice-123-1234567890 (web payment, skip tracking)
   ```

**Expected Results:**
- ✅ Invoice.status updated jadi 'paid'
- ✅ Webhook tidak error (meskipun payment record tidak ada)
- ✅ Log mencatat "web payment, skip tracking"
- ✅ Web dashboard show invoice paid

---

## 🐛 **TROUBLESHOOTING**

### **Issue 1: Checkout Returns 500 Error**

**Possible Causes:**
```bash
# Check 1: Midtrans configuration
# Verify .env:
MIDTRANS_SERVER_KEY=SB-Mid-server-xxxxx (not empty)

# Check 2: Supabase connection
# Test repository:
php -r "
require 'bootstrap.php';
require 'SupaConfig.php';
\$client = new GuzzleHttp\Client([...]);
\$repo = new App\Supabase\InvoiceRepository(\$client);
var_dump(\$repo->findById(1));
"

# Check 3: PaymentRepository autoload
composer dump-autoload

# Check 4: Error logs
tail -f storage/logs/midtrans.log
```

---

### **Issue 2: Webhook Not Updating Payment**

**Debugging Steps:**
```bash
# Check 1: Webhook URL accessible
curl -X POST http://your-domain.com/Form-Handling/api/midtrans/notification.php

# Check 2: Order ID format
# Mobile order_id should match: INV-YYYYMMDD-000123
SELECT order_id FROM payments WHERE id = 456;

# Check 3: Webhook logs
tail -f storage/logs/midtrans.log

# Check 4: Manual webhook test
# Use Midtrans Dashboard → Transaction → Send Webhook
```

---

### **Issue 3: Payment Status Always Pending**

**Debugging:**
```sql
-- Check payment record
SELECT
  p.*,
  i.status as invoice_status,
  i.paid_at
FROM payments p
JOIN invoices i ON i.id = p.invoice_id
WHERE p.id = 456;

-- Check webhook history
SELECT * FROM payments
WHERE order_id = 'INV-20251115-000123'
ORDER BY updated_at DESC;

-- Expected: updated_at should change after webhook
```

---

### **Issue 4: Duplicate Payment Records**

**Cause:** Calling checkout multiple times for same invoice

**Fix:** Implemented in code (return existing if still valid)

**Verify:**
```bash
# Call checkout 2x dengan invoice_id yang sama
# First call: Create new payment
# Second call: Return existing payment with is_existing: true

# Check database:
SELECT COUNT(*) FROM payments WHERE invoice_id = 123 AND status = 'pending';
# Expected: 1 (not 2)
```

---

## 📊 **DATABASE QUERIES FOR MONITORING**

### **Active Payments**
```sql
SELECT
  p.id,
  p.order_id,
  p.status,
  p.gross_amount,
  p.expires_at,
  i.customer_id,
  c.name as customer_name
FROM payments p
JOIN invoices i ON i.id = p.invoice_id
JOIN customers c ON c.id = i.customer_id
WHERE p.status = 'pending'
  AND p.expires_at > NOW()
ORDER BY p.created_at DESC;
```

### **Successful Payments Today**
```sql
SELECT
  COUNT(*) as total_payments,
  SUM(gross_amount) as total_amount
FROM payments
WHERE status = 'settlement'
  AND DATE(settlement_time) = CURRENT_DATE;
```

### **Payment Success Rate**
```sql
SELECT
  status,
  COUNT(*) as count,
  ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM payments
GROUP BY status
ORDER BY count DESC;
```

### **Expired Payments (Need Cleanup)**
```sql
SELECT
  p.id,
  p.order_id,
  p.expires_at,
  i.customer_id
FROM payments p
JOIN invoices i ON i.id = p.invoice_id
WHERE p.status = 'pending'
  AND p.expires_at < NOW()
ORDER BY p.expires_at ASC;
```

---

## ✅ **SUCCESS CRITERIA**

Payment API dianggap berhasil jika:

**Functional:**
- ✅ Checkout creates Snap token & payment record
- ✅ Midtrans payment updates invoice & payment status
- ✅ Status API returns correct payment details
- ✅ Web payments tidak terpengaruh (backward compatible)

**Performance:**
- ✅ Checkout response < 3 seconds
- ✅ Status API response < 500ms
- ✅ Webhook processing < 1 second

**Data Integrity:**
- ✅ No duplicate payment records for same invoice
- ✅ Invoice status sync dengan payment status
- ✅ Metadata correctly stored (VA, QR, etc)
- ✅ settlement_time terisi saat payment success

**Error Handling:**
- ✅ Invalid invoice → 404
- ✅ Unauthorized access → 401/403
- ✅ Midtrans error → 500 with log
- ✅ Webhook error tidak gagalkan invoice update

---

## 🚀 **NEXT STEPS AFTER TESTING**

1. **If All Tests Pass:**
   - [ ] Update SchemaSupabase.md dengan payments table
   - [ ] Integrate dengan mobile app (Android/iOS)
   - [ ] Configure production Midtrans credentials
   - [ ] Set up monitoring/alerting
   - [ ] Load testing (100+ concurrent checkouts)

2. **If Issues Found:**
   - [ ] Document issue di GitHub Issues
   - [ ] Fix bug & re-test
   - [ ] Update code & documentation

3. **Future Enhancements:**
   - [ ] Payment method configuration API (GET /payment-methods)
   - [ ] Invoice detail API (GET /invoices/{id}/payment)
   - [ ] Payment history API (GET /customers/me/payments)
   - [ ] Expired payment cleanup cron job
   - [ ] Push notification saat payment success
   - [ ] Payment receipt PDF generation

---

## 📞 **SUPPORT**

**Issues/Questions:**
- Check logs: `storage/logs/midtrans.log`
- Database: Supabase Dashboard → Table Editor → payments
- Midtrans: Dashboard → Transactions → Transaction History

**References:**
- Midtrans Snap Docs: https://docs.midtrans.com/docs/snap-overview
- Supabase REST API: https://supabase.com/docs/guides/api
- Payment Integration Doc: `Doc/PaymentIntegration.md`
- API Requirements: `Doc/APIRequirements.md`

---

**Testing Status:** Ready ✅
**Estimated Testing Time:** 30-45 minutes
**Required:** Postman/cURL + Midtrans Sandbox Account + Test Customer Account
