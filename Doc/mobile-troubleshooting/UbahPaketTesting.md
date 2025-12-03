# 📱 Testing Guide - Fitur Ubah Paket

> **Status:** Ready for Testing ✅
> **Last Updated:** 2025-12-03
> **Endpoint:** `POST /api/v1/customer/change-package.php`

---

## 🎯 **OBJECTIVE**

Memastikan fitur "Ubah Paket" berfungsi dengan baik dari UI hingga backend integration.

---

## 🛠️ **PRE-TESTING SETUP**

### **1. Build & Sync Project**

```bash
# Di Android Studio
Build → Clean Project
Build → Rebuild Project

# Or via Gradle
./gradlew clean assembleDebug
```

**Expected:** No compile errors ✅

---

### **2. Verify Backend Ready**

**Check Backend Status:**
```bash
# Test PHP backend reachable
curl -X GET "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/customer/change-package.php" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"

# Expected: 200 OK or 404 (if no pending request)
```

**Check Supabase Packages:**
```bash
# Test Supabase packages endpoint
curl -X GET "https://rqmzvonjytyjdfhpqwvc.supabase.co/rest/v1/service_packages?select=*&order=id.asc" \
  -H "apikey: YOUR_SUPABASE_ANON_KEY"

# Expected: 200 OK with package list
```

---

## 📋 **TESTING SCENARIOS**

### **SCENARIO 1: Navigation** 🧭

**Steps:**
1. Login ke app
2. Navigate ke tab "Akun" (Profile)
3. Scroll ke section "Identitas Paket"
4. Click button **"Ubah Paket"**

**Expected Results:**
- ✅ Navigate to `ChangePackageFragment`
- ✅ Show header "Ubah Paket" dengan back button
- ✅ Show loading indicator briefly

**Screenshot Locations:**
- `fragment_akun.xml` (line 296: buttonChangePackage)
- `AkunFragment.java` (line 91-97: onClick listener)

---

### **SCENARIO 2: Load Packages** 📦

**Steps:**
1. Di ChangePackageFragment
2. Wait for packages to load

**Expected Results:**
- ✅ Show list of available packages (from Supabase)
- ✅ Each item shows:
  - Package name (bold)
  - Speed + description
  - Price (formatted Rupiah)
  - RadioButton for selection
- ✅ Current active package is **grayed out** (50% opacity)
- ✅ Current package RadioButton is **disabled**

**Test Data:**
```
Expected packages (example):
- Basic 20 Mbps - Rp150.000
- Super 50 Mbps - Rp350.000
- Premium 100 Mbps - Rp500.000
```

**Error Cases:**
| Error | Expected UI |
|-------|-------------|
| Network timeout | Toast: "Gagal memuat paket: Network error" |
| 401 Unauthorized | Toast: "User not logged in" |
| Empty list | RecyclerView empty (no items) |

---

### **SCENARIO 3: Check Active Request** 🔍

**Condition A: No Pending Request**

**Steps:**
1. User has NO active change-package request
2. Open ChangePackageFragment

**Expected Results:**
- ✅ Status card is **HIDDEN** (`cardStatus.visibility = GONE`)
- ✅ "Ajukan Perubahan" button is **ENABLED**
- ✅ Can select packages freely

---

**Condition B: Has Pending Request**

**Steps:**
1. User already submitted a change-package request (status: `menunggu`/`disetujui`/`dijadwalkan`)
2. Open ChangePackageFragment

**Expected Results:**
- ✅ Status card is **VISIBLE** with info:
  ```
  Status Permintaan
  Status: menunggu • Jadwal: [date if exists]
  ```
- ✅ "Ajukan Perubahan" button is **DISABLED**
- ✅ Cannot submit new request

**Backend Response (200 OK):**
```json
{
  "ticket_id": 123,
  "status": "open",
  "status_keputusan": "menunggu",
  "paket_sekarang_id": 1,
  "paket_diminta_id": 2,
  "catatan_pelanggan": "Ingin upgrade",
  "jadwal_aktivasi": "2025-12-10 00:00:00"
}
```

---

### **SCENARIO 4: Package Selection** ✅

**Steps:**
1. Click on a package item (not current package)
2. RadioButton should be checked
3. Click another package
4. Previous selection unchecked, new one checked

**Expected Results:**
- ✅ Only ONE package can be selected at a time
- ✅ Current package cannot be selected (disabled)
- ✅ "Ajukan Perubahan" button:
  - **DISABLED** if no selection
  - **DISABLED** if selected == current
  - **ENABLED** if selected != current

**UI Updates:**
```java
// From ChangePackageAdapter.java:94-103
itemView.setOnClickListener(onClick);  // Whole card clickable
radio.setOnClickListener(onClick);     // Radio clickable
```

---

### **SCENARIO 5: Submit Change Package** 📤

#### **5A: Success Case** ✅

**Steps:**
1. Select a different package (e.g., Super 50 Mbps)
2. (Optional) Enter notes: "Butuh kecepatan lebih cepat"
3. Click **"Ajukan Perubahan"**

**Expected Results:**
- ✅ Show loading (progress bar + disable UI)
- ✅ Send POST request:
  ```json
  POST /api/v1/customer/change-package.php
  Header: Authorization: Bearer <token>
  Body: {
    "package_id": 2,
    "notes": "Butuh kecepatan lebih cepat"
  }
  ```
- ✅ On success (200/201):
  - Toast: "Permintaan dikirim. Paket akan diproses admin."
  - Reload status → Show status card
  - Button disabled (cannot submit again)

**Backend Response (201 Created):**
```json
{
  "success": true,
  "message": "Permintaan perubahan paket berhasil dikirim",
  "data": {
    "ticket_id": 456,
    "status": "pending",
    "current_package": "Basic 20 Mbps",
    "requested_package": "Super 50 Mbps",
    "notes": "Butuh kecepatan lebih cepat",
    "message": "Paket baru akan diproses admin..."
  }
}
```

---

#### **5B: Validation Errors** ❌

**Error 1: Outstanding Invoice**

**Request:**
```json
{
  "package_id": 2,
  "notes": ""
}
```

**Backend Response (400):**
```json
{
  "success": false,
  "message": "Tidak dapat mengajukan perubahan paket",
  "error_code": "OUTSTANDING_INVOICE",
  "errors": {
    "invoice": "Harap selesaikan tagihan yang tertunda terlebih dahulu"
  }
}
```

**Expected UI:**
- ✅ Toast: "Tidak dapat mengajukan perubahan paket"
- ✅ Button re-enabled (can retry after paying invoice)

---

**Error 2: Pending Request Already Exists**

**Backend Response (400):**
```json
{
  "success": false,
  "message": "Permintaan perubahan paket aktif sudah ada",
  "error_code": "PENDING_REQUEST"
}
```

**Expected UI:**
- ✅ Toast: "Permintaan perubahan paket aktif sudah ada"
- ✅ Reload status to show existing request

---

**Error 3: Same Package**

**Backend Response (400):**
```json
{
  "success": false,
  "message": "Paket yang dipilih sama dengan paket aktif",
  "error_code": "PACKAGE_SAME_AS_CURRENT"
}
```

**Expected UI:**
- ✅ This should NOT happen if frontend validation works
- ✅ If happens: Toast with error message

---

**Error 4: Package Not Available**

**Backend Response (400):**
```json
{
  "success": false,
  "message": "Paket tidak tersedia",
  "error_code": "PACKAGE_NOT_AVAILABLE"
}
```

**Expected UI:**
- ✅ Toast: "Paket tidak tersedia"

---

**Error 5: Customer Not Found**

**Backend Response (404):**
```json
{
  "success": false,
  "message": "Data customer tidak ditemukan",
  "error_code": "CUSTOMER_NOT_FOUND"
}
```

**Expected UI:**
- ✅ Toast: "Data customer tidak ditemukan"
- ✅ Suggest user to re-login

---

**Error 6: Network/Server Error**

**Scenarios:**
- Network timeout (10s+)
- Server 500 error
- Connection lost

**Expected UI:**
- ✅ Toast: "Jaringan bermasalah: [error message]"
- ✅ Hide loading, re-enable button
- ✅ User can retry

---

### **SCENARIO 6: Notes Input** 📝

**Steps:**
1. Click on notes field (`etNote`)
2. Enter multi-line text (max 3 lines configured)
3. Submit request

**Expected Results:**
- ✅ TextInputEditText accepts multi-line input
- ✅ Notes are optional (can be empty)
- ✅ Notes sent to backend in `notes` field

**UI Specs:**
```xml
<!-- fragment_change_package.xml:143-149 -->
<TextInputEditText
    android:id="@+id/etNote"
    android:maxLines="3"
    android:minLines="2"
    android:inputType="textMultiLine" />
```

---

### **SCENARIO 7: Back Navigation** ⬅️

**Steps:**
1. In ChangePackageFragment
2. Click back button (top-left arrow)

**Expected Results:**
- ✅ Return to AkunFragment
- ✅ No data loss in AkunFragment (profile still loaded)

**Implementation:**
```java
// ChangePackageFragment.java:67
binding.btnBackChangePackage.setOnClickListener(v ->
    requireActivity().onBackPressed()
);
```

---

## 🐛 **DEBUGGING TIPS**

### **Issue 1: Packages Not Loading**

**Check:**
1. Logcat for `ServicePackagesRepo` tag
   ```
   D/ServicePackagesRepo: fetchPackages success size=3 latencyMs=850
   ```
2. Token is valid (not expired)
3. Supabase URL correct in `conn.java`

**Fix:**
- Refresh token (logout/login)
- Check network connection
- Verify Supabase API key

---

### **Issue 2: Submit Returns 404**

**Check:**
1. Endpoint URL in ChangePackageService
   ```java
   @POST("customer/change-package.php")  // Must have "customer/" prefix
   ```
2. Base URL in ApiConfig
   ```java
   public static final String API_V1 = BASE_URL + "api/v1/";
   ```
3. Full URL should be:
   ```
   https://.../Form-Handling/api/v1/customer/change-package.php
   ```

**Fix:**
- Verify endpoint path
- Check ngrok URL is active
- Test with curl first

---

### **Issue 3: Field Name Mismatch**

**Check:**
1. ChangePackageRequest.java
   ```java
   @SerializedName("package_id")  // NOT "target_package_id"
   @SerializedName("notes")       // NOT "note"
   ```
2. Backend expects exact field names

**Fix:**
- Ensure DTO matches endpoint spec
- Test with Postman/curl to verify

---

### **Issue 4: Current Package Not Disabled**

**Check:**
1. `currentPackageId` is set correctly
   ```java
   // ChangePackageFragment.java:104
   currentPackageId = status.getPaketSekarangId() != null ?
                      status.getPaketSekarangId().intValue() : null;
   ```
2. Adapter receives correct ID
   ```java
   // ChangePackageFragment.java:83
   adapter.setCurrentPackageId(currentPackageId == null ? -1 : currentPackageId);
   ```

**Fix:**
- Check status API returns correct `paket_sekarang_id`
- Verify adapter bind logic (line 88-91)

---

## 📊 **TEST REPORT TEMPLATE**

```markdown
## Ubah Paket - Test Report

**Tester:** [Your Name]
**Date:** 2025-12-03
**Build:** [Build Number]
**Device:** [Device Model + Android Version]

### Test Results

| Scenario | Status | Notes |
|----------|--------|-------|
| Navigation | ✅ PASS | - |
| Load Packages | ✅ PASS | Loaded 3 packages in 850ms |
| Check Active Request (None) | ✅ PASS | - |
| Check Active Request (Exists) | ✅ PASS | - |
| Package Selection | ✅ PASS | - |
| Submit Success | ✅ PASS | - |
| Submit Error (Outstanding Invoice) | ✅ PASS | - |
| Submit Error (Pending Request) | ✅ PASS | - |
| Notes Input | ✅ PASS | - |
| Back Navigation | ✅ PASS | - |

### Issues Found

1. [Issue description]
   - **Severity:** High/Medium/Low
   - **Repro Steps:** [Steps]
   - **Expected:** [Expected behavior]
   - **Actual:** [Actual behavior]
   - **Screenshot:** [Attach if applicable]

### Summary

- **Total Tests:** 10
- **Passed:** 10
- **Failed:** 0
- **Blocked:** 0

**Recommendation:** ✅ Ready for Production / ⚠️ Needs Fixes
```

---

## 🎯 **SUCCESS CRITERIA**

Fitur dianggap **READY FOR PRODUCTION** jika:

- ✅ All 10 scenarios PASS
- ✅ No critical bugs
- ✅ Error messages user-friendly
- ✅ Loading states responsive (< 3s)
- ✅ UI matches design specs
- ✅ Backend validations work correctly
- ✅ No crashes or ANR (Application Not Responding)

---

## 📞 **SUPPORT**

**Issues/Questions:**
- Backend Team: Check PHP API logs
- Mobile Team: Check Logcat with tags:
  - `ChangePackageRepo`
  - `ServicePackagesRepo`
  - `AUTH_DEBUG`

**Documentation:**
- [MobileChangePackageEndpoint.md](./MobileChangePackageEndpoint.md)
- [UbahPaketPlan.md](./UbahPaketPlan.md)

---

**Happy Testing! 🚀**
