# 06 - Comprehensive Testing Guide

> **Purpose:** Complete testing scenarios untuk ensure migration berhasil dan tidak ada regressions.

---

## 📋 **TEST OVERVIEW**

**Total Test Cases:** 10
**Testing Levels:**
1. Unit Tests (Database RPC)
2. Integration Tests (Edge Function)
3. End-to-End Tests (Mobile App)

**Success Criteria:**
- ✅ All 10 test cases pass
- ✅ Performance < 1 second (95th percentile)
- ✅ Error rate < 0.1%
- ✅ No data inconsistencies

---

## 🎯 **TEST CATEGORIES**

```
Level 1: Database RPC (SQL)
  ├─ Test 1: Success case
  ├─ Test 2: Same package validation
  ├─ Test 3: Pending request validation
  └─ Test 4: Outstanding invoice validation

Level 2: Edge Function (curl/Postman)
  ├─ Test 5: Authentication
  ├─ Test 6: Error handling
  └─ Test 7: Response format

Level 3: Mobile App (UI/E2E)
  ├─ Test 8: Navigation & UI
  ├─ Test 9: Submit flow
  └─ Test 10: Error scenarios
```

---

## 🧪 **LEVEL 1: DATABASE RPC TESTS**

### **Setup: Prepare Test Data**

Run this first in Supabase SQL Editor:

```sql
-- Get your test user
SELECT
  u.id as user_id,
  u.auth_user_id,
  u.email,
  c.id as customer_id,
  c.service_package_id as current_package_id,
  sp.name as current_package_name
FROM users u
LEFT JOIN customers c ON u.customer_id = c.id
LEFT JOIN service_packages sp ON c.service_package_id = sp.id
WHERE u.email = 'YOUR_EMAIL@example.com';
```

**Save these values:**
- `auth_user_id` → Use for testing
- `customer_id` → For validation
- `current_package_id` → To pick different package

---

### **TEST 1: Success Case** ✅

**Objective:** Verify successful change package submission

**Prerequisites:**
- No outstanding invoices
- No pending change requests
- Different package selected

**SQL:**
```sql
-- Clean slate first
DELETE FROM ticket_perubahan_paket WHERE customer_id = YOUR_CUSTOMER_ID;
DELETE FROM tickets WHERE customer_id = YOUR_CUSTOMER_ID AND kategori = 'perubahan_paket';

-- Submit request
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 2,  -- Different from current
  p_notes := 'Test 1: Success case'
);
```

**Expected Result:**
```json
{
  "success": true,
  "ticket_id": 123,
  "status": "pending",
  "current_package": "Basic 20 Mbps",
  "requested_package": "Super 50 Mbps",
  "notes": "Test 1: Success case",
  "message": "Permintaan berhasil dikirim..."
}
```

**Validation:**
```sql
-- Verify ticket created
SELECT * FROM tickets
WHERE id = 123;  -- Use ticket_id from response

-- Verify detail created
SELECT * FROM ticket_perubahan_paket
WHERE ticket_id = 123;
```

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 2: Same Package Validation** ✅

**Objective:** Verify error when selecting current package

**SQL:**
```sql
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 1,  -- Same as current
  p_notes := 'Test 2: Same package'
);
```

**Expected Result:**
```
ERROR: PACKAGE_SAME_AS_CURRENT: Paket yang dipilih sama dengan paket saat ini
```

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 3: Pending Request Validation** ✅

**Objective:** Verify error when active request exists

**Prerequisites:**
- Run Test 1 first (creates pending request)

**SQL:**
```sql
-- Try to submit another request
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 3,
  p_notes := 'Test 3: Duplicate request'
);
```

**Expected Result:**
```
ERROR: PENDING_REQUEST: Masih ada permintaan aktif yang sedang diproses
```

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 4: Outstanding Invoice Validation** ✅

**Objective:** Verify error when unpaid invoices exist

**Setup:**
```sql
-- Create test invoice
INSERT INTO invoices (customer_id, status, total_amount, due_date)
VALUES (YOUR_CUSTOMER_ID, 'issued', 100000, CURRENT_DATE + INTERVAL '7 days')
RETURNING id;
```

**SQL:**
```sql
-- Close previous ticket first
UPDATE tickets SET status = 'closed'
WHERE customer_id = YOUR_CUSTOMER_ID AND kategori = 'perubahan_paket';

-- Try to submit
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 2,
  p_notes := 'Test 4: Has invoice'
);
```

**Expected Result:**
```
ERROR: OUTSTANDING_INVOICE: Harap selesaikan 1 tagihan tertunda
```

**Cleanup:**
```sql
-- Delete test invoice
DELETE FROM invoices WHERE customer_id = YOUR_CUSTOMER_ID AND status = 'issued';
```

**Result:** ✅ PASS / ❌ FAIL

---

## 🌐 **LEVEL 2: EDGE FUNCTION TESTS**

### **Setup: Get JWT Token**

**Option 1: From Mobile App**
```java
// In mobile app, after login:
String token = tokenStorage.getAccessToken();
Log.d("TEST", "JWT Token: " + token);
```

**Option 2: From Supabase Dashboard**
```
Go to: Authentication > Users
Click on user > Copy Access Token
```

Save token as: `JWT_TOKEN`

---

### **TEST 5: Authentication** ✅

**Objective:** Verify JWT validation works

**Test 5a: Valid Token**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Test 5a: Valid token"
  }'
```

**Expected:** HTTP 200, success response

**Test 5b: Missing Token**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Test 5b: No token"
  }'
```

**Expected:** HTTP 401, `{"error_code": "UNAUTHORIZED", "message": "Missing authorization header"}`

**Test 5c: Invalid Token**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer invalid_token_here" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Test 5c: Invalid token"
  }'
```

**Expected:** HTTP 401, `{"error_code": "UNAUTHORIZED", "message": "Invalid or expired token"}`

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 6: Error Handling** ✅

**Objective:** Verify all error codes return properly

**Test 6a: Missing package_id**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "notes": "No package_id"
  }'
```

**Expected:** HTTP 400, `{"error_code": "VALIDATION_ERROR", ...}`

**Test 6b: Same package**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 1,
    "notes": "Same as current"
  }'
```

**Expected:** HTTP 400, `{"error_code": "PACKAGE_SAME_AS_CURRENT", ...}`

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 7: Response Format** ✅

**Objective:** Verify response matches documentation

**SQL:**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Test 7: Response format"
  }' | jq .
```

**Expected Structure:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "ticket_id": 123,
    "status": "pending",
    "current_package": "...",
    "requested_package": "...",
    "notes": "...",
    "message": "..."
  }
}
```

**Validate:**
- [ ] `success` field exists (boolean)
- [ ] `data` object exists
- [ ] `data.ticket_id` is number
- [ ] `data.status` is string
- [ ] All expected fields present

**Result:** ✅ PASS / ❌ FAIL

---

## 📱 **LEVEL 3: MOBILE APP TESTS**

### **Setup: Build & Install**

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install to device/emulator
./gradlew installDebug
```

---

### **TEST 8: Navigation & UI** ✅

**Objective:** Verify UI flow works correctly

**Steps:**
1. Open app
2. Login with test credentials
3. Navigate to "Akun" tab
4. Click "Ubah Paket" button

**Expected:**
- ✅ ChangePackageFragment opens
- ✅ Loading indicator shows while fetching packages
- ✅ Package list displays
- ✅ Current package highlighted/disabled
- ✅ Notes input field visible
- ✅ Submit button enabled

**Validation:**
```
Logcat filter: ChangePackageFragment
Expected logs:
D/ChangePackageFragment: Using Supabase backend
D/ChangePackageFragment: Loading packages...
D/ChangePackageFragment: Packages loaded: X items
```

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 9: Submit Flow** ✅

**Objective:** Verify end-to-end submit works

**Prerequisites:**
- Clean test data (no pending requests)
- No outstanding invoices

**Steps:**
1. Navigate to Change Package screen
2. Select different package (e.g., "Super 50 Mbps")
3. Enter notes: "Test 9: E2E submission"
4. Click "Submit" button

**Expected During:**
- ✅ Loading indicator shows
- ✅ Submit button disabled
- ✅ Network request sent

**Expected After Success:**
- ✅ Success dialog appears
- ✅ Dialog shows ticket ID
- ✅ Dialog shows package names
- ✅ Click "OK" returns to Account screen

**Validation:**
```
Logcat filter: ChangePackageSupabase
Expected logs:
D/ChangePackageSupabase: Submitting change package request to Supabase
D/ChangePackageSupabase: Package ID: 2, Notes: Test 9: E2E submission
I/ChangePackageFragment: Request completed in XXXms
D/ChangePackageFragment: Success! Ticket ID: 123
```

**Database Validation:**
```sql
-- Verify in Supabase
SELECT * FROM tickets
WHERE description LIKE '%Test 9%'
ORDER BY created_at DESC LIMIT 1;

SELECT * FROM ticket_perubahan_paket
WHERE catatan_pelanggan LIKE '%Test 9%'
ORDER BY dibuat_pada DESC LIMIT 1;
```

**Performance Check:**
- Request time should be < 1000ms

**Result:** ✅ PASS / ❌ FAIL

---

### **TEST 10: Error Scenarios** ✅

**Objective:** Verify all error cases handled properly

**Test 10a: Same Package Error**

**Steps:**
1. Note your current package
2. Select the same package
3. Click Submit

**Expected:**
- ✅ Error dialog appears
- ✅ Message: "Paket yang dipilih sama dengan paket aktif saat ini"
- ✅ Click OK dismisses dialog
- ✅ Can try again

---

**Test 10b: Pending Request Error**

**Steps:**
1. Submit a valid change request (creates pending)
2. Try to submit another request immediately

**Expected:**
- ✅ Error dialog: "Masih ada permintaan aktif yang sedang diproses"

**Cleanup:**
```sql
-- Close the ticket
UPDATE tickets
SET status = 'closed'
WHERE customer_id = YOUR_CUSTOMER_ID
  AND kategori = 'perubahan_paket'
  AND status = 'open';
```

---

**Test 10c: No Internet**

**Steps:**
1. Turn off WiFi & mobile data
2. Try to submit request

**Expected:**
- ✅ Error dialog: "Tidak ada koneksi internet" (or similar)
- ✅ Turn internet back on
- ✅ Can retry successfully

---

**Test 10d: Session Expired**

**Steps:**
1. Get JWT token
2. Wait for token to expire (or manually invalidate)
3. Try to submit request

**Expected:**
- ✅ Error dialog: "Sesi telah berakhir. Silakan login kembali."
- ✅ Redirected to login screen

**Result:** ✅ PASS / ❌ FAIL

---

## 📊 **PERFORMANCE BENCHMARKING**

### **Test: Response Time Comparison**

**Setup:**
Add timing logs to both repositories:

```java
long startTime = System.currentTimeMillis();
ChangePackageStatusResponse response = repository.submitChangePackage(packageId, notes);
long duration = System.currentTimeMillis() - startTime;
Log.i(TAG, "Backend: " + (USE_SUPABASE_BACKEND ? "Supabase" : "PHP") + ", Duration: " + duration + "ms");
```

**Test Procedure:**
1. Run 10 requests with Supabase backend
2. Run 10 requests with PHP backend
3. Calculate average, min, max, p95

**Expected Results:**

| Metric | Supabase | PHP | Improvement |
|--------|----------|-----|-------------|
| Average | 600ms | 1800ms | 67% faster |
| Min | 400ms | 1200ms | 67% faster |
| Max | 1000ms | 2500ms | 60% faster |
| P95 | 800ms | 2200ms | 64% faster |

**Record Results:**
```
Test Run: [DATE]
Supabase avg: ___ms
PHP avg: ___ms
Improvement: ___%
```

**Result:** ✅ PASS (Supabase faster) / ❌ FAIL

---

## 🐛 **DEBUGGING TIPS**

### **Issue: Test fails but unclear why**

**Solution: Enable verbose logging**

```java
// In ChangePackageSupabaseRepository
Log.d(TAG, "=== REQUEST START ===");
Log.d(TAG, "Package ID: " + packageId);
Log.d(TAG, "Notes: " + notes);
Log.d(TAG, "Token: " + accessToken.substring(0, 20) + "...");

// After response
Log.d(TAG, "Response code: " + response.code());
Log.d(TAG, "Response body: " + response.body());
Log.d(TAG, "=== REQUEST END ===");
```

---

### **Issue: Different results in SQL vs Mobile**

**Solution: Compare request parameters**

```sql
-- Check what mobile sent
SELECT
  ticket_id,
  customer_id,
  paket_sekarang_id,
  paket_diminta_id,
  catatan_pelanggan,
  dibuat_pada
FROM ticket_perubahan_paket
ORDER BY dibuat_pada DESC
LIMIT 1;
```

Compare with mobile logs.

---

### **Issue: Flaky tests (sometimes pass, sometimes fail)**

**Common Causes:**
1. **Race conditions:** Multiple tests running simultaneously
2. **Data not cleaned up:** Previous test data interferes
3. **Network issues:** Timeout or slow connection
4. **Token expiry:** Token expires during test

**Solution:**
- Clean data before each test
- Add delays between tests
- Use fresh tokens
- Add retry logic for network errors

---

## ✅ **TEST REPORT TEMPLATE**

```markdown
# Change Package Migration - Test Report

**Date:** 2025-12-03
**Tester:** [Your Name]
**Build:** Debug / Release
**Device:** [Device Model]

## Test Results

| # | Test Name | Status | Notes |
|---|-----------|--------|-------|
| 1 | RPC Success Case | ✅ PASS | |
| 2 | Same Package Validation | ✅ PASS | |
| 3 | Pending Request Validation | ✅ PASS | |
| 4 | Outstanding Invoice Validation | ✅ PASS | |
| 5 | Authentication | ✅ PASS | |
| 6 | Error Handling | ✅ PASS | |
| 7 | Response Format | ✅ PASS | |
| 8 | Navigation & UI | ✅ PASS | |
| 9 | Submit Flow | ✅ PASS | |
| 10 | Error Scenarios | ✅ PASS | |

**Overall:** 10/10 PASS ✅

## Performance

- Supabase avg: 620ms
- PHP avg: 1850ms
- Improvement: 66% faster ⚡

## Issues Found

[List any issues found during testing]

## Recommendations

[Any recommendations for improvement]

## Sign-off

- [ ] All tests passing
- [ ] Performance acceptable
- [ ] Ready for production

**Signed:** _____________
**Date:** _____________
```

---

## 📚 **REFERENCES**

- **SQL Testing:** 03-RPCFunctions.md
- **Edge Function Testing:** 04-EdgeFunctions.md
- **Mobile Testing:** 05-MobileIntegration.md

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [07-Rollback.md](./07-Rollback.md)
