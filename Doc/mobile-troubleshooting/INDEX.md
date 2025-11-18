# 📱 Mobile Troubleshooting - Complete Index

**Problem:** "Gagal memuat invoice" di Android app
**Created:** 2025-11-16
**Status:** STEP 1 - Fix domain blocking

---

## 🎯 START HERE

### For Mobile Team AI:

**👉 Current Task:** Fix domain blocking issue

**⚡ Quick Path:**
1. Open `01-STEP1-DOMAIN-BLOCKING.md`
2. Follow 4 actions (10 minutes)
3. Report results
4. Move to next step

---

## 📁 Folder Structure

```
mobile-troubleshooting/
│
├── INDEX.md                            ← You are here
├── README.md                           ← Overview & quick links
│
├── 📘 STEP-BY-STEP GUIDES
│   ├── 01-STEP1-DOMAIN-BLOCKING.md    ← **START HERE** (Current)
│   ├── 02-STEP2-LOGGING.md            ← Next (after STEP 1)
│   ├── 03-STEP3-RETROFIT.md           ← Testing Retrofit
│   └── 04-STEP4-ENDPOINTS.md          ← Testing endpoints
│
├── 📚 REFERENCE DOCS
│   ├── complete-troubleshooting-guide.md
│   └── quick-test-guide.md
│
└── 📂 templates/
    ├── network_security_config.xml     ← Copy to res/xml/
    └── NetworkTestActivity.kt          ← Test code
```

---

## 🚀 Quick Start Guide

### Problem: "Gagal memuat invoice"

**Diagnosis:**
```
Symptom: Request tidak keluar dari app
Cause: Android blocking ngrok domain
Fix: Add network security config (5 min)
```

**Action:**
```
1. Open: 01-STEP1-DOMAIN-BLOCKING.md
2. Copy: templates/network_security_config.xml → res/xml/
3. Edit: AndroidManifest.xml (add 2 attributes)
4. Rebuild: Clean + Rebuild project
5. Test: Run test code from guide
6. Report: Success/Failure with Logcat
```

---

## 📖 Documentation Quick Reference

### By Task:

| Task | Document | Time |
|------|----------|------|
| Fix domain blocking | `01-STEP1-DOMAIN-BLOCKING.md` | 10 min |
| Add debug logging | `02-STEP2-LOGGING.md` | 5 min |
| Test Retrofit setup | `03-STEP3-RETROFIT.md` | 10 min |
| Test API endpoints | `04-STEP4-ENDPOINTS.md` | 15 min |

### By Problem:

| Problem | Solution | Document |
|---------|----------|----------|
| No OkHttp logs | Add network config | STEP 1 |
| SSL handshake error | Fix network config | STEP 1 |
| Unknown host error | Check ngrok URL | STEP 1 |
| 401 Unauthorized | Check token | STEP 2 |
| 404 Not Found | Check BASE_URL | STEP 3 |
| App crashes | Check Logcat | complete-guide |

---

## 🎓 File Descriptions

### STEP 1: Domain Blocking (Current)
**File:** `01-STEP1-DOMAIN-BLOCKING.md`
**Goal:** Allow app to connect to ngrok backend
**Time:** 10 minutes
**Actions:** 4 steps
**Success:** Logcat shows "Response Code: 200"

### STEP 2: Logging (Next)
**File:** `02-STEP2-LOGGING.md`
**Goal:** Add OkHttp logging for debugging
**Time:** 5 minutes
**Prerequisite:** STEP 1 complete

### STEP 3: Retrofit Testing
**File:** `03-STEP3-RETROFIT.md`
**Goal:** Test Retrofit API client setup
**Time:** 10 minutes
**Prerequisite:** STEP 2 complete

### STEP 4: Endpoint Testing
**File:** `04-STEP4-ENDPOINTS.md`
**Goal:** Test all invoice & payment endpoints
**Time:** 15 minutes
**Prerequisite:** STEP 3 complete

### Complete Guide
**File:** `complete-troubleshooting-guide.md`
**Purpose:** A-Z troubleshooting reference
**Use when:** Stuck or need deep dive

### Quick Test Guide
**File:** `quick-test-guide.md`
**Purpose:** Fast testing procedures
**Use when:** Need to verify specific issue

---

## 🛠️ Templates Available

### 1. network_security_config.xml
**Location:** `templates/network_security_config.xml`
**Use:** Fix domain blocking
**Copy to:** `app/src/main/res/xml/network_security_config.xml`
**Required in:** STEP 1

### 2. NetworkTestActivity.kt
**Location:** `templates/NetworkTestActivity.kt`
**Use:** Test network connectivity
**Optional:** Can use code from STEP 1 instead

---

## 📊 Progress Tracker

| Step | Status | Time | Completed |
|------|--------|------|-----------|
| 1. Domain Blocking | 🔄 In Progress | 10 min | ⬜ |
| 2. Logging | ⏸️ Waiting | 5 min | ⬜ |
| 3. Retrofit Test | ⏸️ Waiting | 10 min | ⬜ |
| 4. Endpoints Test | ⏸️ Waiting | 15 min | ⬜ |

**Total:** ~40 minutes

---

## 🔍 Quick Diagnosis Tree

```
"Gagal memuat invoice"
│
├─ No Logcat logs at all?
│  └─ → DO STEP 1 (Domain blocking)
│
├─ Logcat shows SSL error?
│  └─ → DO STEP 1 (Network config issue)
│
├─ Logcat shows Unknown Host?
│  └─ → Check ngrok URL (see STEP 1)
│
├─ Logcat shows 401?
│  └─ → DO STEP 2 (Token issue)
│
├─ Logcat shows 404?
│  └─ → DO STEP 3 (Wrong URL/path)
│
└─ Logcat shows 500?
   └─ → Contact backend team
```

---

## 🎯 Success Criteria

### STEP 1 Success:
- [x] Logcat shows "Response Code: 200"
- [x] Response contains `"success":true`
- [x] No SSL/network errors
- [x] Backend ping successful

### All Steps Success:
- [x] Invoice list loads
- [x] Invoice detail shows
- [x] Payment checkout works
- [x] Payment status updates
- [x] No "Gagal memuat invoice" error

---

## 🏗️ Backend Endpoints

### Debug Endpoints (Ready ✅)

**Ping (No auth)**
```
GET https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/ping.php
```

**Token Check (With auth)**
```
GET .../api/v1/debug/token-check.php
Header: Authorization: Bearer {token}
```

**User Info (With auth)**
```
GET .../api/v1/debug/me.php
Header: Authorization: Bearer {token}
```

### Production Endpoints (All tested ✅)

1. **Invoice List** - `/api/v1/invoices/index.php`
2. **Invoice Detail** - `/api/v1/invoices/detail.php?id=X`
3. **Payment Checkout** - `/api/v1/payments/checkout.php` (POST)
4. **Payment Status** - `/api/v1/payments/status.php?invoice_id=X`

---

## 📝 How to Report

### Success Report Format:
```
✅ STEP [number] COMPLETE

Test results:
- Logcat output: [paste]
- Response code: 200
- Response body: {"success":true...}

Ready for next step: STEP [number+1]
```

### Failure Report Format:
```
❌ STEP [number] FAILED

Error type: [SSLHandshakeException / UnknownHostException / etc]

Logcat output:
[paste full error logs including stack trace]

What I tried:
- [x] Action 1
- [x] Action 2
- [ ] Still failing

Files:
- network_security_config.xml: [exists / not exists]
- AndroidManifest.xml: [has attribute / missing]
```

---

## 💡 Tips for Success

1. **Read STEP 1 completely** before starting
2. **Follow steps in order** - don't skip
3. **Copy files exactly** as shown
4. **Always rebuild** after XML changes
5. **Check Logcat** for detailed errors
6. **Report early** if stuck > 15 min
7. **Test in browser** first (ping endpoint)

---

## 📞 Support & Help

### Backend Team
- Status: ✅ Available
- Response time: < 5 minutes
- All endpoints: ✅ Verified working

### Common Questions

**Q: Which step should I start with?**
A: Always start with STEP 1 (domain blocking)

**Q: Can I skip STEP 1 if I have network config?**
A: No - verify it works with test in STEP 1

**Q: How long should each step take?**
A: STEP 1: 10 min, STEP 2: 5 min, STEP 3: 10 min, STEP 4: 15 min

**Q: What if I get stuck?**
A: Report with Logcat output, we'll help

---

## 🚀 Ready to Start?

### Next Action:
**👉 Open: `01-STEP1-DOMAIN-BLOCKING.md`**

### Quick checklist:
- [ ] I understand the problem (domain blocking)
- [ ] I have Android Studio open
- [ ] I can access templates folder
- [ ] I'm ready to follow 4 steps
- [ ] I'll report results after testing

---

**Let's fix this! Good luck! 🎯**
