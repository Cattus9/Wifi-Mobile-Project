# ✅ Mobile App Integration - COMPLETE

> **Date:** 2025-12-03
> **Status:** ✅ ALL FILES CREATED & UPDATED
> **Ready for:** Build & Testing

---

## 📝 FILES CREATED

### **1. SupabaseChangePackageService.java** ✅
**Location:** `app/src/main/java/com/project/inet_mobile/data/remote/SupabaseChangePackageService.java`

**Purpose:** Retrofit interface untuk Supabase Edge Function endpoint

**Endpoint:** `POST /functions/v1/change-package`

```java
@POST("functions/v1/change-package")
Call<SupabaseChangePackageResponse> submitChangePackage(
    @Header("Authorization") String authHeader,
    @Body ChangePackageRequest request
);
```

---

### **2. SupabaseChangePackageResponse.java** ✅
**Location:** `app/src/main/java/com/project/inet_mobile/data/remote/dto/SupabaseChangePackageResponse.java`

**Purpose:** Response DTO dari Edge Function

**Structure:**
```json
{
  "success": true,
  "data": {
    "ticket_id": 123,
    "status": "pending",
    "current_package": "...",
    "requested_package": "...",
    "message": "..."
  }
}
```

---

### **3. ChangePackageSupabaseRepository.java** ✅
**Location:** `app/src/main/java/com/project/inet_mobile/data/packages/ChangePackageSupabaseRepository.java`

**Purpose:** Repository implementation using Supabase Edge Function

**Features:**
- JWT authentication handling
- Error message parsing (OUTSTANDING_INVOICE, PENDING_REQUEST, etc.)
- Performance logging (latency tracking)
- User-friendly error messages in Indonesian

---

### **4. SupabaseApiClient.java** ✅ (UPDATED)
**Location:** `app/src/main/java/com/project/inet_mobile/data/remote/SupabaseApiClient.java`

**Changes:**
- Added `SupabaseChangePackageService` field
- Added `getSupabaseChangePackageService()` method

---

### **5. ChangePackageFragment.java** ✅ (UPDATED)
**Location:** `app/src/main/java/com/project/inet_mobile/ui/account/ChangePackageFragment.java`

**Changes:**
- Added feature flag: `USE_SUPABASE_BACKEND`
- Added `supabaseRepo` field
- Updated `onViewCreated()` - initialize correct repository based on flag
- Added `submitViaSupabase()` method
- Added `submitViaPHP()` method
- Updated `submitChange()` - route to correct backend
- Updated `loadStatus()` - skip for Supabase (not implemented yet)

---

## 🎛️ FEATURE FLAG

**Switch backend dengan mengubah constant:**

```java
// File: ChangePackageFragment.java (line ~41)

private static final boolean USE_SUPABASE_BACKEND = true;  // ← Ubah ini

// true  = Gunakan Supabase Edge Function (NEW)
// false = Gunakan PHP Backend (OLD)
```

**Keuntungan feature flag:**
- ✅ Easy A/B testing
- ✅ Quick rollback (just change to `false`)
- ✅ Gradual rollout possible
- ✅ Both backends can coexist

---

## 🔄 ARCHITECTURE FLOW

### **Supabase Backend Flow (NEW):**
```
ChangePackageFragment
    ↓ (submitViaSupabase)
ChangePackageSupabaseRepository
    ↓ (Retrofit HTTP call)
SupabaseChangePackageService
    ↓ (POST /functions/v1/change-package)
Supabase Edge Function
    ↓ (RPC call)
submit_change_package (PostgreSQL function)
    ↓ (Atomic transaction)
Database (tickets, ticket_perubahan_paket)
```

### **PHP Backend Flow (OLD):**
```
ChangePackageFragment
    ↓ (submitViaPHP)
ChangePackageRepository
    ↓ (Retrofit HTTP call)
ChangePackageService
    ↓ (POST /customer/change-package.php)
PHP Backend
    ↓ (Multiple HTTP calls to Supabase REST API)
Database
```

---

## 🎯 NEXT STEPS: BUILD & TEST

### **STEP 1: Build Project**

**Via Android Studio:**
1. Click **Build → Make Project** (Ctrl+F9)
2. Wait for build to complete
3. Check for compilation errors

**Via Gradle (Terminal):**
```bash
./gradlew assembleDebug
```

---

### **STEP 2: Fix Compilation Errors (If Any)**

**Common issues:**
- Missing imports → Add imports
- Type mismatches → Check DTO field types
- Null pointer warnings → Add @Nullable annotations

---

### **STEP 3: Install ke Device/Emulator**

**Via Android Studio:**
1. Connect device atau start emulator
2. Click **Run → Run 'app'** (Shift+F10)

**Via Gradle:**
```bash
./gradlew installDebug
```

---

### **STEP 4: Test Functionality**

#### **Test 1: Success Case**
1. Login ke app dengan user: `leon@gmail.com` / `Admin123`
2. Navigate to **Account → Ubah Paket**
3. Select package yang berbeda dari current package
4. Add notes (optional): "Test dari mobile app"
5. Click **Submit**

**Expected:**
- Loading indicator muncul
- Success toast: "Permintaan perubahan paket berhasil dikirim..."
- Submit button disabled

**Logcat:**
```
D/ChangePackageFragment: Using Supabase backend for Change Package
D/ChangePackageSupabase: Submitting change package request to Supabase Edge Function
D/ChangePackageSupabase: Package ID: 2, Notes: Test dari mobile app
D/ChangePackageSupabase: Submit change-package SUCCESS latencyMs=850
D/ChangePackageSupabase: Ticket ID: 456
D/ChangePackageFragment: Submit success via Supabase! Ticket ID: 456
```

---

#### **Test 2: Same Package Error**
1. Try to submit dengan package yang sama
2. Click **Submit**

**Expected:**
- Local validation blocks submit
- Toast: "Paket sudah aktif."

---

#### **Test 3: Pending Request Error**
1. Submit request pertama kali (success)
2. Immediately try submit request kedua kali
3. Click **Submit**

**Expected:**
- Error toast: "Masih ada permintaan aktif yang sedang diproses"

**Logcat:**
```
E/ChangePackageSupabase: Business validation error: Masih ada permintaan aktif...
```

---

#### **Test 4: Feature Flag Switch**

**Test Supabase:**
```java
private static final boolean USE_SUPABASE_BACKEND = true;
```
Submit → Should hit Edge Function

**Test PHP:**
```java
private static final boolean USE_SUPABASE_BACKEND = false;
```
Submit → Should hit PHP backend

**Both should work!** (until PHP deprecated)

---

#### **Test 5: Network Error**
1. Turn off WiFi & mobile data
2. Try to submit
3. Click **Submit**

**Expected:**
- Error toast: "Jaringan bermasalah: ..."

---

## 📊 PERFORMANCE COMPARISON

Monitor latency in Logcat:

**Target Metrics:**
| Metric | PHP Backend | Supabase Backend | Target |
|--------|-------------|------------------|--------|
| Submit latency | ~1800ms | ~700-900ms | < 1000ms |
| Success rate | ~99% | ~99.5% | > 99% |
| Error rate | ~1% | < 0.5% | < 0.5% |

**Check logs:**
```
D/ChangePackageRepo: submit change-package success latencyMs=1850 (PHP)
D/ChangePackageSupabase: Submit change-package SUCCESS latencyMs=850 (Supabase)
```

**Supabase should be ~50-60% faster!** ⚡

---

## 🐛 TROUBLESHOOTING

### **Issue 1: Compilation Error - Cannot find symbol**

**Error:**
```
error: cannot find symbol: class SupabaseChangePackageService
```

**Solution:**
- Check file created at correct path
- Sync Gradle: **File → Sync Project with Gradle Files**
- Rebuild: **Build → Rebuild Project**

---

### **Issue 2: NullPointerException at runtime**

**Error:**
```
java.lang.NullPointerException: Attempt to invoke virtual method on a null object
```

**Solution:**
```java
// Check initialization in onViewCreated:
if (USE_SUPABASE_BACKEND && supabaseRepo == null) {
    Log.e(TAG, "supabaseRepo is null!");
}
```

---

### **Issue 3: HTTP 401 Unauthorized**

**Error from Edge Function:**
```json
{"code": 401, "message": "Invalid JWT"}
```

**Solution:**
- Check token is valid (not expired)
- Check AuthInterceptor adds Bearer token correctly
- Re-login if needed

**Debug:**
```java
// In ChangePackageSupabaseRepository.submitChangePackage():
Log.d(TAG, "Auth header: " + (auth != null ? "present" : "null"));
```

---

### **Issue 4: Edge Function 404**

**Error:**
```
HTTP 404 - functions/v1/change-package not found
```

**Solution:**
- Verify Edge Function deployed di Supabase Dashboard
- Check URL correct: `https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package`
- Check SupabaseApiClient baseUrl includes trailing slash

---

### **Issue 5: Response parsing error**

**Error:**
```
com.google.gson.JsonSyntaxException: Expected BEGIN_OBJECT but was STRING
```

**Solution:**
- Check DTO fields match Edge Function response
- Add logging to see raw response:
```java
Log.d(TAG, "Raw response body: " + response.body().toString());
```

---

## ✅ CHECKLIST SEBELUM PRODUCTION

**Code Quality:**
- [ ] All files compiled without errors
- [ ] No unused imports
- [ ] Feature flag documented
- [ ] Logging added for debugging

**Testing:**
- [ ] Success case works
- [ ] All error cases handled
- [ ] Feature flag switch works (PHP ↔ Supabase)
- [ ] Performance acceptable (< 1s)
- [ ] Network error handled gracefully

**Documentation:**
- [ ] Code comments added (if needed)
- [ ] TODOs resolved or documented
- [ ] Team notified of changes

---

## 🚀 DEPLOYMENT STRATEGY

### **Phase 1: Debug Testing (Day 1)**
```java
// Debug build only
USE_SUPABASE_BACKEND = true
```
Test internally dengan development team.

---

### **Phase 2: Beta Testing (Day 2-3)**
```java
// Enable untuk beta testers
if (BuildConfig.DEBUG || isBetaTester()) {
    USE_SUPABASE_BACKEND = true;
}
```

---

### **Phase 3: Canary Release (Day 4-5)**
```java
// 10% of users
if (getUserId().hashCode() % 100 < 10) {
    USE_SUPABASE_BACKEND = true;
}
```
Monitor error rates & performance.

---

### **Phase 4: Full Rollout (Day 6+)**
```java
USE_SUPABASE_BACKEND = true; // All users
```
Monitor for 24-48 hours.

---

### **Phase 5: Deprecate PHP (Day 14+)**
Remove old code:
- Delete `ChangePackageRepository.java` (PHP version)
- Delete `ChangePackageService.java` (PHP version)
- Remove feature flag
- Keep only Supabase implementation

---

## 📈 SUCCESS METRICS

**Migration considered SUCCESSFUL when:**

✅ **Performance:**
- Response time < 1 second (avg)
- 95th percentile < 1.5 seconds

✅ **Reliability:**
- Error rate < 0.5%
- No data inconsistencies
- All validations working

✅ **User Experience:**
- No increase in support tickets
- Positive user feedback
- Feature adoption maintained

✅ **Technical:**
- All tests passing
- Build successful
- No regressions

---

## 🔗 RELATED DOCUMENTS

- **Architecture:** `Doc/DokumentasiUbahPaket/01-Context.md`
- **Migration Plan:** `Doc/DokumentasiUbahPaket/02-MigrationPlan.md`
- **RPC Functions:** `Doc/DokumentasiUbahPaket/03-RPCFunctions.md`
- **Edge Functions:** `Doc/DokumentasiUbahPaket/04-EdgeFunctions.md`
- **Testing Guide:** `Doc/DokumentasiUbahPaket/06-Testing.md`
- **Edge Function Test Results:** `Doc/DokumentasiUbahPaket/TEST_RESULTS_EdgeFunction.md`

---

## 📞 SUPPORT

**If you encounter issues:**
1. Check Logcat for error messages
2. Verify Edge Function logs in Supabase Dashboard
3. Check this troubleshooting section
4. Review related documentation

**Supabase Dashboard:**
- Functions Logs: https://supabase.com/dashboard/project/rqmzvonjytyjdfhpqwvc/functions/change-package/logs
- Database: https://supabase.com/dashboard/project/rqmzvonjytyjdfhpqwvc/database

---

**Document Version:** 1.0
**Integration Status:** ✅ COMPLETE
**Ready for Testing:** YES
**Last Updated:** 2025-12-03
