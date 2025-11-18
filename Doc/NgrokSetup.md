# Ngrok Setup Guide - Mobile Payment Testing

> **Ngrok URL:** https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/
>
> **Purpose:** Expose localhost untuk testing mobile app dengan Midtrans webhook
>
> **Status:** Ready for testing

---

## 🌐 **KENAPA PERLU NGROK?**

Midtrans webhook **tidak bisa** kirim notification ke `localhost` atau `127.0.0.1`. Mereka perlu URL yang accessible dari internet.

**Solusi:** Ngrok membuat tunnel dari internet → localhost Anda

```
Internet (Midtrans)
    ↓
https://unthoroughly-arachidic-aaden.ngrok-free.dev
    ↓ (ngrok tunnel)
http://localhost/Form-Handling
    ↓
XAMPP (Your Computer)
```

---

## 📋 **SETUP CHECKLIST**

### **1. Install Ngrok** ✅

```bash
# Download dari: https://ngrok.com/download
# Atau via Chocolatey (Windows):
choco install ngrok

# Verify installation
ngrok version
```

---

### **2. Start Ngrok Tunnel**

```bash
# Basic tunnel (free tier)
ngrok http 80

# Custom subdomain (paid tier only)
ngrok http 80 --subdomain=unthoroughly-arachidic-aaden

# Expected output:
# Forwarding: https://unthoroughly-arachidic-aaden.ngrok-free.dev -> http://localhost:80
```

**IMPORTANT:** Free tier ngrok URL **berubah setiap restart**. URL `unthoroughly-arachidic-aaden` hanya valid selama ngrok jalan.

---

### **3. Verify Ngrok URL**

**Test di browser:**
```
https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/
```

**Expected:** Homepage project Anda muncul

**If Error "Visit Site" button:**
- Klik "Visit Site" button
- Ngrok free tier ada interstitial page warning
- Ini normal untuk free tier

---

### **4. Configure Midtrans Webhook**

**Login ke Midtrans Dashboard:**
1. Go to: https://dashboard.sandbox.midtrans.com/
2. Settings → Configuration
3. Payment Notification URL:
   ```
   https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/midtrans/notification.php
   ```
4. Finish Redirect URL (optional):
   ```
   https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/public/LandingPages.php
   ```
5. Save

**PENTING:** Update URL ini **setiap kali restart ngrok** (jika free tier)

---

### **5. Test Webhook Connectivity**

**Manual Test:**
```bash
# Dari terminal/PowerShell
curl -X POST https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/midtrans/notification.php

# Expected: Error (karena tidak ada valid Midtrans signature)
# Tapi response artinya endpoint reachable
```

**Via Midtrans Dashboard:**
1. Create test transaction
2. Complete payment
3. Midtrans auto-send webhook
4. Check logs: `storage/logs/midtrans.log`

---

## 🔧 **CORS CONFIGURATION**

Current CORS setting: `['*']` (allow all origins)

**For Production/Stricter Security:**

Edit `api/v1/_core/middleware.php`:

```php
// Change from:
ApiMiddleware::cors();

// To:
ApiMiddleware::cors([
    'https://unthoroughly-arachidic-aaden.ngrok-free.dev',
    'http://localhost',
    'http://127.0.0.1',
]);
```

**For Now:** Default `['*']` sudah cukup untuk testing.

---

## 📱 **MOBILE APP CONFIGURATION**

### **Android App (Java/Kotlin)**

**Update base URL:**

```java
// File: app/src/main/java/com/project/inet_mobile/util/ApiConfig.java

public class ApiConfig {
    // Development (ngrok)
    public static final String BASE_URL = "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/";

    // API endpoints
    public static final String API_V1 = BASE_URL + "api/v1/";

    // Endpoints
    public static final String CHECKOUT = API_V1 + "payments/checkout";
    public static final String PAYMENT_STATUS = API_V1 + "payments/status";
}
```

**Network Security Config** (untuk HTTPS ngrok):

File: `app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />

    <!-- Allow ngrok HTTPS (for development) -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">unthoroughly-arachidic-aaden.ngrok-free.dev</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>

    <!-- Allow localhost (for testing) -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain> <!-- Android Emulator -->
        <domain includeSubdomains="true">192.168.x.x</domain> <!-- Local network -->
    </domain-config>
</network-security-config>
```

**AndroidManifest.xml:**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

## 🧪 **TESTING FLOW VIA NGROK**

### **Test 1: API Reachability**

```bash
# Test checkout endpoint
curl -X POST https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/checkout \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"invoice_id": 123, "preferred_channel": "qris"}'

# Expected: 201 Created (or 401 if token invalid)
```

---

### **Test 2: Midtrans Webhook**

**Scenario:**
1. Mobile app call checkout → dapat snap_token
2. User bayar via Midtrans Snap
3. Midtrans send webhook ke ngrok URL
4. Ngrok forward ke localhost XAMPP
5. Webhook update database

**Monitor:**
```bash
# Terminal 1: Ngrok logs (lihat incoming requests)
# Ngrok dashboard: http://localhost:4040

# Terminal 2: Watch logs
tail -f C:\xampp\htdocs\Form-Handling\storage\logs\midtrans.log

# Terminal 3: Watch database
# Supabase Dashboard → Table Editor → payments (refresh manual)
```

---

### **Test 3: End-to-End Mobile Payment**

**Flow:**
```
1. Mobile App → POST /api/v1/payments/checkout
   ↓
2. Response: snap_token + redirect_url
   ↓
3. Mobile App → Open redirect_url in WebView
   ↓
4. User → Complete payment in Midtrans
   ↓
5. Midtrans → POST webhook to ngrok URL
   ↓
6. Ngrok → Forward to localhost/api/midtrans/notification.php
   ↓
7. Webhook → Update invoices + payments tables
   ↓
8. Mobile App → GET /api/v1/payments/status
   ↓
9. Response: status = 'settlement', show success
```

**Expected Result:**
- ✅ Payment status updated
- ✅ Invoice marked as paid
- ✅ Mobile app shows success screen

---

## ⚠️ **COMMON ISSUES & FIXES**

### **Issue 1: Ngrok "Visit Site" Warning**

**Symptom:**
Ngrok shows interstitial page: "You are about to visit..."

**Cause:**
Free tier ngrok anti-abuse protection

**Fix:**
- Click "Visit Site" button (satu kali saja)
- Atau upgrade ke ngrok paid tier (no warning)

**For Mobile App:**
Mobile app WebView tidak support interstitial page. Solusi:
1. Upgrade ngrok ke paid tier ($8/month)
2. Atau deploy ke hosting real (Vercel, Heroku, dll)

---

### **Issue 2: Webhook Not Received**

**Debugging:**

```bash
# Step 1: Check ngrok running
ngrok http 80
# Should show: Forwarding ... -> http://localhost:80

# Step 2: Check ngrok dashboard
http://localhost:4040/inspect/http
# Should show incoming POST to /api/midtrans/notification.php

# Step 3: Test manual webhook
curl -X POST https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/midtrans/notification.php \
  -H "Content-Type: application/json" \
  -d '{"order_id": "test"}'

# Expected: Error (no valid signature) but response artinya reachable

# Step 4: Check Midtrans notification URL config
# Dashboard → Settings → Payment Notification URL
# Harus sama dengan ngrok URL
```

---

### **Issue 3: XAMPP Not Running**

**Symptom:**
Ngrok shows "502 Bad Gateway"

**Fix:**
```bash
# Start XAMPP
# - Apache harus running di port 80
# - MySQL harus running

# Verify XAMPP:
http://localhost/Form-Handling/
# Should show homepage
```

---

### **Issue 4: CORS Error di Mobile App**

**Symptom:**
Mobile app error: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Fix:**
Already handled! CORS default `['*']` di middleware.

**If still error:**
Check mobile app network interceptor (OkHttp, Retrofit, etc) tidak block CORS headers.

---

## 📊 **MONITORING NGROK TRAFFIC**

### **Ngrok Web Interface**

```
http://localhost:4040
```

**Features:**
- See all HTTP requests in real-time
- Replay requests (testing)
- Inspect request/response headers
- Filter by status code

**Use Cases:**
- Debug webhook payload dari Midtrans
- Check request headers
- Verify CORS headers sent

---

### **Ngrok Logs (Terminal)**

```
ngrok http 80 --log=stdout --log-level=info
```

**Output:**
```
POST /Form-Handling/api/midtrans/notification.php 200 OK
GET  /Form-Handling/api/v1/payments/status?invoice_id=123 200 OK
```

---

## 🔐 **SECURITY CONSIDERATIONS**

### **Ngrok Free Tier Limitations**

⚠️ **WARNING:** Ngrok free tier URL adalah **public** dan siapapun bisa akses!

**Risks:**
- URL berubah setiap restart (unpredictable)
- Interstitial warning page (tidak cocok production)
- Rate limiting (40 connections/minute)
- No custom subdomain

**Mitigations:**
1. **Authentication:** API sudah pakai JWT (aman)
2. **Don't share URL:** Jangan share ngrok URL ke public
3. **Short-lived:** Matikan ngrok setelah testing
4. **Monitor logs:** Check suspicious access

---

### **Ngrok Paid Tier ($8/month)**

**Benefits:**
- ✅ Custom subdomain (permanent)
- ✅ No interstitial warning
- ✅ Higher rate limits
- ✅ Reserved domain
- ✅ IP whitelisting

**For Production Testing:** Worth it untuk testing mobile app stabil.

---

## 🚀 **PRODUCTION DEPLOYMENT (FUTURE)**

**Alternatives to Ngrok:**

1. **Vercel** (Free tier available)
   - Deploy PHP via Vercel Functions
   - Custom domain support
   - Auto HTTPS

2. **Heroku** (Free tier deprecated, $7/month)
   - Deploy full XAMPP stack
   - Custom domain
   - PostgreSQL addon

3. **DigitalOcean** ($5/month)
   - Full VPS control
   - Install XAMPP/LAMP
   - Custom domain

4. **Shared Hosting** (Niagahoster, Hostinger, etc)
   - Rp 10k-50k/month
   - cPanel + PHP support
   - Custom domain included

**Recommendation untuk project ini:**
- **Testing:** Ngrok free tier (cukup)
- **Beta/Staging:** Ngrok paid tier atau Vercel
- **Production:** Shared hosting Indonesia (customer lokal, latency rendah)

---

## 📋 **QUICK REFERENCE**

### **URLs untuk Testing**

```bash
# Ngrok URL (base)
https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/

# API Endpoints
POST   /api/v1/payments/checkout
GET    /api/v1/payments/status?invoice_id={id}
GET    /api/v1/payments/status?payment_id={id}

# Webhook (Midtrans callback)
POST   /api/midtrans/notification.php

# Web UI (testing)
GET    /public/LandingPages.php
GET    /public/Login.php
```

---

### **Important Ports**

```
Port 80   : Apache (XAMPP)
Port 3306 : MySQL (XAMPP)
Port 4040 : Ngrok Web Interface (monitoring)
```

---

### **Testing Commands**

```bash
# Start ngrok
ngrok http 80

# Test API
curl https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/status?invoice_id=1

# Watch logs
tail -f storage/logs/midtrans.log

# Monitor ngrok traffic
# Open: http://localhost:4040
```

---

## ✅ **READY TO TEST?**

**Checklist:**
- [x] Ngrok installed & running
- [x] XAMPP Apache running (port 80)
- [x] Midtrans webhook URL configured
- [x] Mobile app base URL updated
- [x] Type errors fixed di checkout.php
- [x] CORS configured (default allow all)

**Next Steps:**
1. Start ngrok: `ngrok http 80`
2. Update Midtrans webhook URL (jika berubah)
3. Test checkout API via Postman/mobile app
4. Complete payment di Midtrans
5. Verify webhook update database
6. Test status API

**Happy Testing!** 🚀

---

**Notes:**
- Ngrok URL valid selama ngrok process jalan
- Restart ngrok = URL berubah (free tier)
- Save ngrok URL untuk update Midtrans config
- Monitor http://localhost:4040 untuk debug

**Support:**
- Ngrok Docs: https://ngrok.com/docs
- Midtrans Docs: https://docs.midtrans.com
- Testing Guide: `Doc/MobilePaymentTesting.md`
