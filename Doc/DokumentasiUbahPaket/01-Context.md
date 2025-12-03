# 01 - Context & Background

> **Purpose:** Understand why we're migrating Change Package feature from PHP to Supabase-First architecture.

---

## 🎯 **PROBLEM STATEMENT**

Fitur "Ubah Paket" saat ini menggunakan **Dual Backend Architecture** yang menyebabkan:
- 🐌 Slow performance (2+ seconds per request)
- 🐛 Hard to debug (2 systems to check)
- 💰 Higher costs ($65/month vs $25/month)
- 🔧 Complex maintenance (3 failure points)
- ⚡ Data consistency risks (no distributed transactions)

---

## 🏗️ **CURRENT ARCHITECTURE (Dual Backend)**

### **System Diagram**

```
┌──────────────────────────────────────────────────────────────┐
│                      MOBILE APP                               │
│                 (ChangePackageFragment)                       │
└──────────────────────────────────────────────────────────────┘
            │                                │
            │ (1) GET Packages               │ (2) POST Submit
            ↓                                ↓
   ┌──────────────────┐            ┌─────────────────────┐
   │  SUPABASE REST   │            │   PHP BACKEND       │
   │  (PostgreSQL)    │            │   (Ngrok/Custom)    │
   └──────────────────┘            └─────────────────────┘
            │                                │
            │                                │ (3-7) Validate & Create
            ↓                                ↓
   ┌───────────────────────────────────────────────────┐
   │             SUPABASE DATABASE                      │
   │  - users, customers, service_packages             │
   │  - invoices, tickets, ticket_perubahan_paket      │
   └───────────────────────────────────────────────────┘
```

### **Request Flow Detail**

```
User clicks "Ubah Paket"
  │
  ├─→ (1) Mobile → Supabase REST
  │   GET /rest/v1/service_packages
  │   Response Time: ~500ms
  │   Purpose: Get available packages
  │
  └─→ (2) Mobile → PHP Backend
      POST /api/v1/customer/change-package.php
      │
      ├─→ (3) PHP → Supabase: Get customer_id
      │   GET /rest/v1/users?auth_user_id=...
      │   Time: ~200ms
      │
      ├─→ (4) PHP → Supabase: Check invoices
      │   GET /rest/v1/invoices?customer_id=...&status=...
      │   Time: ~150ms
      │
      ├─→ (5) PHP → Supabase: Check pending tickets
      │   GET /rest/v1/tickets?customer_id=...&kategori=...
      │   Time: ~150ms
      │
      ├─→ (6) PHP → Supabase: Get current package
      │   GET /rest/v1/customers?id=...
      │   Time: ~100ms
      │
      ├─→ (7) PHP → Supabase: Create ticket
      │   POST /rest/v1/tickets
      │   Time: ~200ms
      │
      └─→ (8) PHP → Supabase: Create detail
          POST /rest/v1/ticket_perubahan_paket
          Time: ~200ms

Total Latency: ~1.8 seconds (8 network round-trips!)
```

---

## ⚠️ **CURRENT PROBLEMS**

### **1. Performance Issues**

**Network Overhead:**
- Mobile → Supabase: 500ms
- Mobile → PHP: 300ms
- PHP → Supabase (6 queries): 1000ms
- **Total: ~1.8 seconds** ⏱️

**Why it's slow:**
```
Each API call = Network latency + Processing time
PHP must query Supabase 6 times sequentially
No caching, no optimization
```

---

### **2. Debugging Complexity**

**When error occurs:**
```
Error: "Failed to submit change package"

Where is the problem? 🤔
❓ Mobile network issue?
❓ PHP server down (Ngrok expired)?
❓ Supabase rate limit exceeded?
❓ Token expired/invalid?
❓ Validation failed (which one: invoice? ticket? package?)?
❓ Database constraint violated?
❓ Race condition (concurrent requests)?

Must check 7 different logs! 📋
```

**Real Example:**
```php
// PHP Backend (change-package.php)
try {
    $customer = querySupabase("/rest/v1/users?auth_user_id=...");
    $invoices = querySupabase("/rest/v1/invoices?customer_id=...");
    // ...more queries
} catch (Exception $e) {
    // Which query failed? 🤷‍♂️
    log_error($e->getMessage());
    return ["error" => "Something went wrong"];
}
```

---

### **3. Data Consistency Risks**

**Scenario: Partial Failure**

```php
// PHP execution:
✅ Create ticket → SUCCESS (ticket_id: 123)
   Network timeout occurs here...
❌ Create ticket_perubahan_paket → FAILED

Result:
- Ticket exists (orphaned data) 💀
- No detail record (incomplete transaction) ⚠️
- User sees error ❌
- Data inconsistent! Must manual cleanup 🔧
```

**Why it happens:**
- PHP and Supabase are separate systems
- No distributed transaction support
- HTTP calls can fail at any step
- Rollback requires manual intervention

---

### **4. Maintenance Burden**

**Adding New Validation:**

```
Timeline for adding "Customer must be active for 3 months" validation:

Day 1-2: Mobile App
  ├─ Update validation logic
  ├─ Update error messages
  └─ Update UI tests

Day 3-4: PHP Backend
  ├─ Add query to check customer age
  ├─ Add error handling
  ├─ Update API response
  └─ Test PHP → Supabase integration

Day 5-6: Integration Testing
  ├─ Test Mobile ↔ PHP
  ├─ Test PHP ↔ Supabase
  └─ Test all error scenarios

Total: 6 days, 3 systems to update! ⏰
```

---

### **5. Cost Overhead**

**Monthly Costs:**

```
Current (Dual Backend):
├─ Supabase Pro:       $25/month
├─ PHP Server (VPS):   $30/month
├─ Ngrok Static:       $10/month
└─ Total:              $65/month 💸

If Supabase-First:
├─ Supabase Pro:       $25/month
└─ Total:              $25/month 💰

Savings: $40/month (62% reduction!) 📉
```

---

## 🎯 **PROPOSED SOLUTION: Supabase-First**

### **New Architecture Diagram**

```
┌──────────────────────────────────────────────────────────────┐
│                      MOBILE APP                               │
│                 (ChangePackageFragment)                       │
└──────────────────────────────────────────────────────────────┘
            │                                │
            │ (1) GET Packages               │ (2) POST Submit
            ↓                                ↓
   ┌──────────────────┐            ┌────────────────────┐
   │  Supabase REST   │            │  Supabase Edge Fn  │
   │  PostgREST       │            │  change-package.ts │
   └──────────────────┘            └────────────────────┘
            │                                │
            └────────────┬───────────────────┘
                         │ (3) Single RPC Call
                         ↓
              ┌──────────────────────┐
              │   DATABASE RPC       │
              │  submit_change_pkg() │
              │  (Atomic TX)         │
              └──────────────────────┘
                         │
                         ↓
              ┌──────────────────────┐
              │  PostgreSQL Tables   │
              │  Single Transaction  │
              └──────────────────────┘
```

### **New Request Flow**

```
User clicks "Ubah Paket"
  │
  ├─→ (1) Mobile → Supabase REST
  │   GET /rest/v1/service_packages
  │   Time: ~500ms
  │   Purpose: Get available packages
  │
  └─→ (2) Mobile → Supabase Edge Function
      POST /functions/v1/change-package
      │
      └─→ (3) Edge Function → RPC Function (single database call)
          SELECT submit_change_package(
            p_auth_user_id := auth.uid(),
            p_package_id := 2,
            p_notes := 'Upgrade request'
          )
          │
          └─ ATOMIC TRANSACTION (all validations + inserts):
             ✅ Get customer_id
             ✅ Check invoices
             ✅ Check pending tickets
             ✅ Validate package
             ✅ Create ticket
             ✅ Create detail

          Time: ~200ms

Total Latency: ~700ms (65% faster!) ⚡
```

---

## ✅ **EXPECTED BENEFITS**

### **1. Performance Improvement**

```
Before: 1.8 seconds (8 network hops)
After:  0.7 seconds (2 network hops)
Improvement: 61% faster! 🚀
```

### **2. Simpler Debugging**

```
Error: "OUTSTANDING_INVOICE"

Where to check? ✅
1. Check Supabase logs (Edge Function + RPC)
2. Done!

All logic in ONE place, one log stream! 📋
```

### **3. Data Consistency Guaranteed**

```sql
-- Everything in atomic transaction:
BEGIN;
  -- All validations
  -- All inserts
  -- Either ALL succeed or ALL rollback
COMMIT;

No partial failures possible! ✅
```

### **4. Easier Maintenance**

```
Timeline for adding new validation:

Day 1: Update RPC Function
  └─ Add validation logic in SQL

Day 2: Test & Deploy
  └─ Test via SQL Editor → Deploy

Total: 2 days, 1 system to update! ⚡
```

### **5. Cost Reduction**

```
Current: $65/month
New:     $25/month
Savings: $40/month × 12 = $480/year! 💰
```

---

## 📊 **COMPARISON TABLE**

| Aspect | Current (Dual Backend) | New (Supabase-First) | Improvement |
|--------|----------------------|---------------------|-------------|
| **Response Time** | ~1.8s | ~0.7s | **61% faster** ⚡ |
| **Network Hops** | 8 hops | 2 hops | **75% reduction** |
| **Systems to Debug** | 2 systems | 1 system | **50% simpler** 🐛 |
| **Data Consistency** | Risk of partial failures | Atomic transactions | **100% reliable** ✅ |
| **Monthly Cost** | $65 | $25 | **$40 savings** 💰 |
| **Deployment Steps** | 3 places | 1 place | **67% faster** 🚀 |
| **Code Complexity** | High (PHP + Retrofit) | Medium (TypeScript + SQL) | **Moderate** |
| **Scalability** | Manual (VPS limits) | Auto-scaling | **Infinite** 📈 |

---

## 🎯 **WHY START WITH CHANGE PACKAGE?**

**Strategic Reasons:**

1. **Small Scope** ✅
   - Single feature
   - Clear boundaries
   - Easy to test

2. **High Impact** 💪
   - Performance critical feature
   - Frequently used
   - User-facing

3. **Learning Opportunity** 📚
   - Pilot project for larger migration
   - Establish patterns
   - Build confidence

4. **Low Risk** 🛡️
   - Easy rollback (PHP still there)
   - Can run parallel during migration
   - No breaking changes for users

5. **Foundation for Future** 🏗️
   - Prove Supabase-First works
   - Template for other features:
     - Payment processing
     - Invoice management
     - Support tickets
     - Profile updates

---

## 🚀 **NEXT STEPS**

After understanding this context, proceed to:

👉 **[02-MigrationPlan.md](./02-MigrationPlan.md)** - See detailed step-by-step migration plan

---

## 📚 **REFERENCES**

- `../ArchitectureRefactoring.md` - Full architecture migration strategy
- `../mobile-troubleshooting/MobileChangePackageEndpoint.md` - Current PHP endpoint spec
- `../mobile-troubleshooting/UbahPaketPlan.md` - Feature business requirements

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [02-MigrationPlan.md](./02-MigrationPlan.md)
