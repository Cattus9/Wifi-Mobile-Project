# 03 - RPC Functions Detail

> **Purpose:** Complete reference untuk database RPC functions yang di-create untuk Change Package feature.

---

## 📋 **OVERVIEW**

RPC Functions adalah **PostgreSQL functions** yang run directly di database. Benefits:
- ⚡ Single database transaction (atomic)
- 🔒 Server-side security (SECURITY DEFINER)
- 🚀 Fast execution (no network overhead)
- ✅ Built-in error handling

---

## 🎯 **FUNCTIONS CREATED**

### **1. submit_change_package**
Main function untuk submit change package request dengan validations.

### **2. get_active_change_package_status**
Helper function untuk get current active request status.

---

## 📝 **FUNCTION 1: submit_change_package**

### **Purpose**
Handle complete business logic untuk change package submission:
- Validate user & customer
- Check outstanding invoices
- Check pending requests
- Validate package selection
- Create ticket & detail atomically

### **Signature**

```sql
CREATE OR REPLACE FUNCTION submit_change_package(
  p_auth_user_id UUID,      -- User's auth ID from JWT
  p_package_id BIGINT,       -- New package to change to
  p_notes TEXT DEFAULT NULL  -- Optional customer notes
)
RETURNS JSON
```

### **Parameters**

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `p_auth_user_id` | UUID | ✅ Yes | User's UUID from auth.users | `'550e8400-e29b-41d4-a716-446655440000'` |
| `p_package_id` | BIGINT | ✅ Yes | ID of new package | `2` |
| `p_notes` | TEXT | ❌ No | Customer notes/reason | `'Butuh kecepatan lebih cepat'` |

### **Return Type**

Returns `JSON` object:

**Success Response:**
```json
{
  "success": true,
  "ticket_id": 123,
  "status": "pending",
  "current_package": "Basic 20 Mbps",
  "requested_package": "Super 50 Mbps",
  "notes": "Customer note here",
  "message": "Permintaan berhasil dikirim..."
}
```

**Error Response:**
```
EXCEPTION: ERROR_CODE: Human readable message
```

### **Business Logic Flow**

```
1. Get customer_id from auth_user_id
   └─ Query: users table
   └─ Validate: user.is_active = true

2. Validate new package exists and active
   └─ Query: service_packages table
   └─ Check: is_active = true

3. Get current package
   └─ Query: customers table
   └─ Get: service_package_id

4. Validate package is different
   └─ Check: new_id != current_id

5. Check outstanding invoices
   └─ Query: invoices table
   └─ Check: status IN ('issued', 'overdue', 'draft')
   └─ Count must be 0

6. Check pending change requests
   └─ Query: tickets table
   └─ Check: kategori='perubahan_paket', status IN ('open', 'in_progress')
   └─ Count must be 0

7. Create ticket (BEGIN TRANSACTION)
   └─ Insert into: tickets
   └─ Get: ticket_id

8. Create ticket detail
   └─ Insert into: ticket_perubahan_paket
   └─ Link to: ticket_id

9. Build success response
   └─ Return: JSON with details

10. COMMIT or ROLLBACK on error
```

### **Error Codes**

| Error Code | Reason | User Message | HTTP Status |
|------------|--------|--------------|-------------|
| `CUSTOMER_NOT_FOUND` | User has no customer record | "Data customer tidak ditemukan" | 404 |
| `PACKAGE_NOT_AVAILABLE` | Package ID doesn't exist or inactive | "Paket tidak tersedia" | 400 |
| `PACKAGE_SAME_AS_CURRENT` | Selected same package as current | "Paket sudah aktif" | 400 |
| `OUTSTANDING_INVOICE` | Has unpaid invoices | "Harap selesaikan tagihan tertunda" | 400 |
| `PENDING_REQUEST` | Already has active change request | "Permintaan aktif sudah ada" | 400 |

### **SQL Implementation**

**File Location:** `supabase/migrations/20251203000001_submit_change_package_rpc.sql`

**Key Code Sections:**

#### **Step 1: Get Customer ID**
```sql
SELECT customer_id
INTO v_customer_id
FROM users
WHERE auth_user_id = p_auth_user_id
  AND is_active = true;

IF v_customer_id IS NULL THEN
  RAISE EXCEPTION 'CUSTOMER_NOT_FOUND: Data customer tidak ditemukan';
END IF;
```

#### **Step 5: Check Outstanding Invoices**
```sql
SELECT COUNT(*)
INTO v_outstanding_count
FROM invoices
WHERE customer_id = v_customer_id
  AND status IN ('issued', 'overdue', 'draft');

IF v_outstanding_count > 0 THEN
  RAISE EXCEPTION 'OUTSTANDING_INVOICE: Harap selesaikan % tagihan tertunda', v_outstanding_count;
END IF;
```

#### **Step 7-8: Create Records (Atomic)**
```sql
-- Create ticket
INSERT INTO tickets (...) VALUES (...) RETURNING id INTO v_ticket_id;

-- Create detail
INSERT INTO ticket_perubahan_paket (...) VALUES (...);

-- If any fails, automatic ROLLBACK!
```

### **Testing Examples**

#### **Test 1: Success Case**
```sql
SELECT submit_change_package(
  p_auth_user_id := '550e8400-e29b-41d4-a716-446655440000'::uuid,
  p_package_id := 2,
  p_notes := 'Upgrade untuk kebutuhan WFH'
);
```

**Expected:** Returns JSON with `success: true`

#### **Test 2: Same Package Error**
```sql
SELECT submit_change_package(
  p_auth_user_id := '550e8400-e29b-41d4-a716-446655440000'::uuid,
  p_package_id := 1,  -- Assume current package is 1
  p_notes := 'Test'
);
```

**Expected:** Exception `PACKAGE_SAME_AS_CURRENT`

#### **Test 3: Pending Request Error**
```sql
-- Run success test first, then immediately run again
SELECT submit_change_package(
  p_auth_user_id := '550e8400-e29b-41d4-a716-446655440000'::uuid,
  p_package_id := 2,
  p_notes := 'Duplicate'
);
```

**Expected:** Exception `PENDING_REQUEST`

---

## 📝 **FUNCTION 2: get_active_change_package_status**

### **Purpose**
Get current active change package request for user (if exists).

### **Signature**

```sql
CREATE OR REPLACE FUNCTION get_active_change_package_status(
  p_auth_user_id UUID
)
RETURNS JSON
```

### **Return Type**

**If Active Request Exists:**
```json
{
  "ticket_id": 123,
  "status": "open",
  "status_keputusan": "menunggu",
  "paket_sekarang_id": 1,
  "paket_diminta_id": 2,
  "catatan_admin": null,
  "catatan_pelanggan": "Customer notes",
  "jadwal_aktivasi": null,
  "diterapkan_pada": null,
  "created_at": "2025-12-03T10:00:00Z"
}
```

**If No Active Request:**
```
NULL
```

### **Business Logic**

```
1. Get customer_id from auth_user_id
   └─ Same as submit_change_package

2. Query active ticket
   └─ JOIN: tickets + ticket_perubahan_paket
   └─ Filter: kategori='perubahan_paket'
   └─ Filter: status IN ('open', 'in_progress')
   └─ Order: created_at DESC
   └─ Limit: 1

3. Return result
   └─ If found: Return ticket JSON
   └─ If not found: Return NULL
```

### **Testing Examples**

#### **Test 1: No Active Request**
```sql
-- Before submitting any request
SELECT get_active_change_package_status(
  p_auth_user_id := '550e8400-e29b-41d4-a716-446655440000'::uuid
);
```

**Expected:** `NULL`

#### **Test 2: Has Active Request**
```sql
-- After submit_change_package success
SELECT get_active_change_package_status(
  p_auth_user_id := '550e8400-e29b-41d4-a716-446655440000'::uuid
);
```

**Expected:** JSON object with ticket details

---

## 🔧 **DEPLOYMENT GUIDE**

### **Step-by-Step Deployment**

#### **1. Open Supabase Dashboard**
```
URL: https://supabase.com/dashboard/project/rqmzvonjytyjdfhpqwvc
Navigate: SQL Editor (left sidebar)
```

#### **2. Open Migration File**
```
File: supabase/migrations/20251203000001_submit_change_package_rpc.sql
Action: Open in text editor (Notepad, VS Code, etc.)
```

#### **3. Copy SQL Content**
```
Select All (Ctrl+A)
Copy (Ctrl+C)
```

#### **4. Paste to SQL Editor**
```
Paste (Ctrl+V) into Supabase SQL Editor
```

#### **5. Execute**
```
Click: "Run" button (bottom right)
Or: Press Ctrl+Enter
```

#### **6. Verify Success**
```
Expected Output:
  - "Success. No rows returned"
  Or
  - "CREATE FUNCTION"

Both indicate success! ✅
```

### **Verification Steps**

#### **Check Functions Exist**
```sql
SELECT
  routine_name,
  routine_type,
  data_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN (
    'submit_change_package',
    'get_active_change_package_status'
  )
ORDER BY routine_name;
```

**Expected:** 2 rows

#### **Check Permissions**
```sql
SELECT
  routine_name,
  grantee,
  privilege_type
FROM information_schema.routine_privileges
WHERE routine_name IN (
  'submit_change_package',
  'get_active_change_package_status'
)
AND grantee = 'authenticated';
```

**Expected:** Shows `EXECUTE` privileges

---

## 🧪 **COMPREHENSIVE TESTING**

### **Setup Test Environment**

```sql
-- 1. Get your test user
SELECT
  u.id,
  u.auth_user_id,
  u.email,
  c.id as customer_id,
  c.service_package_id as current_package
FROM users u
LEFT JOIN customers c ON u.customer_id = c.id
WHERE u.email = 'YOUR_EMAIL@example.com';

-- Save auth_user_id for tests below
```

```sql
-- 2. Get available packages
SELECT id, name, price, is_active
FROM service_packages
WHERE is_active = true
ORDER BY id;

-- Pick a different package_id from current
```

### **Test Suite**

#### **Test 1: Happy Path**
```sql
-- Clean slate first
DELETE FROM ticket_perubahan_paket WHERE ticket_id IN (
  SELECT id FROM tickets WHERE description LIKE '%Test%'
);
DELETE FROM tickets WHERE description LIKE '%Test%';

-- Submit request
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 2,
  p_notes := 'Test: Happy path'
);

-- ✅ Should return success JSON
```

#### **Test 2: Duplicate Request**
```sql
-- Run immediately after Test 1
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 2,
  p_notes := 'Test: Duplicate'
);

-- ❌ Should throw PENDING_REQUEST error
```

#### **Test 3: Get Status**
```sql
SELECT get_active_change_package_status(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid
);

-- ✅ Should return ticket JSON
```

#### **Test 4: Close Ticket & Retry**
```sql
-- Close the ticket
UPDATE tickets
SET status = 'closed'
WHERE id = (
  SELECT ticket_id FROM ticket_perubahan_paket
  WHERE customer_id = YOUR_CUSTOMER_ID
  ORDER BY dibuat_pada DESC LIMIT 1
);

-- Now get status should return NULL
SELECT get_active_change_package_status(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid
);

-- ✅ Should return NULL

-- And can submit new request
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 3,
  p_notes := 'Test: After close'
);

-- ✅ Should succeed
```

### **Cleanup After Testing**

```sql
-- Delete all test tickets
DELETE FROM ticket_perubahan_paket
WHERE catatan_pelanggan LIKE '%Test%';

DELETE FROM tickets
WHERE description LIKE '%Test%';
```

---

## 🐛 **TROUBLESHOOTING**

### **Issue 1: "Function does not exist"**

**Cause:** Migration not applied correctly

**Solution:**
```sql
-- Check if function exists
SELECT routine_name
FROM information_schema.routines
WHERE routine_name = 'submit_change_package';

-- If empty, re-run migration SQL
```

### **Issue 2: "Permission denied"**

**Cause:** Missing EXECUTE grant

**Solution:**
```sql
-- Grant permissions
GRANT EXECUTE ON FUNCTION submit_change_package TO authenticated;
GRANT EXECUTE ON FUNCTION get_active_change_package_status TO authenticated;
```

### **Issue 3: "Column does not exist"**

**Cause:** Database schema mismatch

**Solution:**
```sql
-- Verify table structure
\d users
\d customers
\d tickets
\d ticket_perubahan_paket

-- Compare with schema in Doc/SchemaSupabase.md
```

---

## 📚 **REFERENCES**

- **PostgreSQL Functions:** https://www.postgresql.org/docs/current/xfunc-sql.html
- **Supabase Database Functions:** https://supabase.com/docs/guides/database/functions
- **Schema Reference:** `../SchemaSupabase.md`

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [04-EdgeFunctions.md](./04-EdgeFunctions.md)
