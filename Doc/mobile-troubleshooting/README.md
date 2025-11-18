# Mobile App Troubleshooting Center

**Problem:** "Gagal memuat invoice" di aplikasi Android
**Status:** STEP 1 - Fixing Domain Blocking
**Updated:** 2025-11-16

---

## 📁 Folder Contents

```
mobile-troubleshooting/
├── README.md                           ← You are here (Start here!)
├── 01-STEP1-DOMAIN-BLOCKING.md         ← Current step (Instructions)
├── 02-STEP2-LOGGING.md                 ← Next (After STEP 1 success)
├── 03-STEP3-RETROFIT.md                ← Testing Retrofit
├── 04-STEP4-ENDPOINTS.md               ← Testing actual endpoints
├── complete-troubleshooting-guide.md   ← Full reference guide
├── quick-test-guide.md                 ← Quick testing procedures
└── templates/                          ← Android code templates
    ├── network_security_config.xml
    ├── NetworkTestActivity.kt
    └── RetrofitTestCode.kt
```

---

## 🚀 Quick Start

### Current Issue: "Gagal memuat invoice"

**Most Likely Cause:** Android blocking ngrok domain (network security)

**Quick Fix:**
1. Open: `01-STEP1-DOMAIN-BLOCKING.md`
2. Follow 4 steps (5 minutes)
3. Test & report back

---

## 📖 Documentation Guide

### For Mobile Team AI Agents:

**Start here:**
1. **STEP 1:** `01-STEP1-DOMAIN-BLOCKING.md` ← **DO THIS FIRST**
   - Fix domain blocking
   - Add network security config
   - Test backend connectivity
   - **Expected:** 5-10 minutes

2. **STEP 2:** `02-STEP2-LOGGING.md` (After STEP 1 success)
   - Add OkHttp logging
   - Debug Retrofit calls
   - **Expected:** 5 minutes

3. **STEP 3:** `03-STEP3-RETROFIT.md` (After STEP 2 success)
   - Test Retrofit integration
   - Verify API client setup
   - **Expected:** 10 minutes

4. **STEP 4:** `04-STEP4-ENDPOINTS.md` (After STEP 3 success)
   - Test invoice endpoints
   - Test payment endpoints
   - **Expected:** 15 minutes

---

### Reference Materials:

- **complete-troubleshooting-guide.md** - Complete A-Z troubleshooting
- **quick-test-guide.md** - Fast testing procedures
- **templates/** - Ready-to-use code snippets

---

## 🎯 Current Status

### ✅ Backend Status: READY
- All 4 endpoints tested: ✅
- Debug endpoints created: ✅
- CORS configured: ✅
- Documentation complete: ✅

### ⏳ Mobile Status: WAITING FOR STEP 1

**Waiting for:**
- [ ] Mobile team implement STEP 1
- [ ] Test results from mobile team
- [ ] Logcat output showing success/failure

---

## 📊 Progress Tracker

| Step | Task | Status | Time Est. |
|------|------|--------|-----------|
| **1** | Fix Domain Blocking | 🔄 In Progress | 5-10 min |
| 2 | Add Logging | ⏸️ Waiting | 5 min |
| 3 | Test Retrofit | ⏸️ Waiting | 10 min |
| 4 | Test Endpoints | ⏸️ Waiting | 15 min |

**Total estimated time:** 35-40 minutes

---

## 🔍 Quick Diagnosis

**Symptom → Action:**

| Symptom | Root Cause | Action |
|---------|------------|--------|
| No OkHttp logs in Logcat | Domain blocking | → Do STEP 1 |
| Request not leaving app | Network security config missing | → Do STEP 1 |
| "Gagal memuat invoice" toast | Any of above | → Start STEP 1 |
| 401 Unauthorized | Token expired | → Check token-check endpoint |
| 404 Not Found | Wrong URL/path | → Verify BASE_URL |
| 500 Server Error | Backend issue | → Contact backend team |

---

## 🛠️ Backend Endpoints Available

### Debug Endpoints (For Testing)

**1. Ping (No Auth)**
```
GET https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/ping.php
```
**Use:** Test basic connectivity

**2. Token Check (With Auth)**
```
GET https://.../api/v1/debug/token-check.php
Header: Authorization: Bearer {token}
```
**Use:** Verify token validity

**3. User Info (With Auth)**
```
GET https://.../api/v1/debug/me.php
Header: Authorization: Bearer {token}
```
**Use:** Get user data

---

### Production Endpoints (Tested ✅)

**1. Invoice List**
```
GET /api/v1/invoices/index.php
Status: ✅ Working
```

**2. Invoice Detail**
```
GET /api/v1/invoices/detail.php?id=10
Status: ✅ Working
```

**3. Payment Checkout**
```
POST /api/v1/payments/checkout.php
Body: {"invoice_id": 10, "preferred_channel": "qris"}
Status: ✅ Working
```

**4. Payment Status**
```
GET /api/v1/payments/status.php?invoice_id=10
Status: ✅ Working
```

---

## 📝 How to Report Issues

When reporting errors, include:

1. **Step number** (e.g., "STEP 1 failed")
2. **Error type** (e.g., SSLHandshakeException)
3. **Logcat output** (full error trace)
4. **What you tried** (which fixes applied)

**Example Good Report:**
```
❌ STEP 1 FAILED
Error: javax.net.ssl.SSLHandshakeException
Logcat:
E/NETWORK_TEST: ❌ FAILED: SSL Handshake error
E/NETWORK_TEST: Message: Trust anchor for certification path not found

Tried:
- Added network_security_config.xml ✅
- Updated AndroidManifest.xml ✅
- Rebuilt project ✅
- Still getting SSL error

AndroidManifest.xml snippet:
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

## 💡 Tips

1. **Always rebuild** after adding XML files
2. **Check Logcat** for detailed errors
3. **Test ping endpoint** in browser first
4. **One step at a time** - don't skip steps
5. **Report early** if stuck > 15 minutes

---

## 📞 Support

**Backend Team Status:**
- All endpoints: ✅ Verified working
- Debug tools: ✅ Available
- Response time: < 5 minutes

**Current backend ngrok URL:**
```
https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/
```

**Verify backend alive:**
Open in browser: https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/ping.php

---

## 🎯 Success Criteria

**STEP 1 Complete when:**
- [ ] Logcat shows `Response Code: 200`
- [ ] Response body contains `"success":true`
- [ ] No SSL/network errors
- [ ] Backend ping successful from app

**All Steps Complete when:**
- [ ] Invoice list loads in app
- [ ] Invoice detail shows correctly
- [ ] Payment checkout creates session
- [ ] Payment status updates
- [ ] No "Gagal memuat invoice" errors

---

## 🏁 Let's Start!

**👉 Open:** `01-STEP1-DOMAIN-BLOCKING.md`

**Follow the steps, test, and report back!**

Good luck! 🚀
