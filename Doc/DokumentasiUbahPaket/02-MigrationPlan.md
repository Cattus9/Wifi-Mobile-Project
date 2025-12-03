# 02 - Migration Plan: Step-by-Step Guide

> **Purpose:** Complete roadmap untuk migrate Change Package dari PHP ke Supabase-First

---

## 📋 **MIGRATION OVERVIEW**

**Total Steps:** 8
**Estimated Time:** 2-3 days (dengan testing)
**Risk Level:** 🟢 Low (can run parallel, easy rollback)
**Approach:** Incremental (deploy new, test, switch, deprecate old)

---

## 🎯 **MIGRATION STRATEGY**

```
Phase 1: BUILD (Steps 1-4)
  └─ Create new Supabase-based solution

Phase 2: TEST (Step 5-6)
  └─ Verify everything works

Phase 3: SWITCH (Step 7)
  └─ Point mobile app to new endpoint

Phase 4: CLEANUP (Step 8)
  └─ Monitor & deprecate old PHP endpoint
```

---

## 📝 **DETAILED STEPS**

### **STEP 1: Prerequisites & Setup** ⏱️ 30 minutes

#### **1.1 Verify Supabase Access**

**Actions:**
- [ ] Login to https://supabase.com/dashboard
- [ ] Access project: `rqmzvonjytyjdfhpqwvc`
- [ ] Verify permissions (can create functions)

**Test:**
```sql
-- Run in SQL Editor
SELECT current_user, current_database();
```

**Expected:** Shows your user and database name

---

#### **1.2 Verify Database Schema**

**Actions:**
- [ ] Check tables exist: `users`, `customers`, `service_packages`, `invoices`, `tickets`, `ticket_perubahan_paket`
- [ ] Verify columns match migration requirements

**Test:**
```sql
-- Verify tables
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_name IN (
    'users', 'customers', 'service_packages',
    'invoices', 'tickets', 'ticket_perubahan_paket'
  )
ORDER BY table_name;
```

**Expected:** Returns 6 rows (all tables exist)

---

#### **1.3 Prepare Test Data**

**Actions:**
- [ ] Create test user (if not exists)
- [ ] Ensure test user has customer record
- [ ] Ensure at least 2 active packages exist

**Test:**
```sql
-- Get test user
SELECT
  u.id,
  u.auth_user_id,
  u.email,
  c.id as customer_id,
  c.service_package_id,
  sp.name as current_package
FROM users u
LEFT JOIN customers c ON u.customer_id = c.id
LEFT JOIN service_packages sp ON c.service_package_id = sp.id
WHERE u.email = 'YOUR_TEST_EMAIL@example.com';
```

**Save:**
- `auth_user_id` → Will use for testing
- `customer_id` → For validation
- `service_package_id` → Current package

---

### **STEP 2: Create RPC Functions** ⏱️ 1 hour

#### **2.1 Apply Migration SQL**

**Actions:**
- [ ] Open file: `supabase/migrations/20251203000001_submit_change_package_rpc.sql`
- [ ] Copy entire content
- [ ] Go to Supabase Dashboard → SQL Editor
- [ ] Paste SQL
- [ ] Click "Run"

**Expected Output:**
```
Success. No rows returned
```

Or:
```
CREATE FUNCTION
```

**Verification:**
```sql
-- Check functions created
SELECT routine_name, routine_type
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN (
    'submit_change_package',
    'get_active_change_package_status'
  );
```

**Expected:** 2 rows (both functions exist)

---

#### **2.2 Grant Permissions**

**Actions:**
- [ ] Verify authenticated users can execute functions

**Test:**
```sql
-- Check grants
SELECT routine_name, grantee, privilege_type
FROM information_schema.routine_privileges
WHERE routine_name IN (
  'submit_change_package',
  'get_active_change_package_status'
)
AND grantee = 'authenticated';
```

**Expected:** Shows EXECUTE permission for authenticated role

---

### **STEP 3: Test RPC Functions** ⏱️ 1 hour

#### **3.1 Test Success Case**

**Test SQL:**
```sql
-- Replace with your actual auth_user_id
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID_HERE'::uuid,
  p_package_id := 2,  -- Different from current
  p_notes := 'Test from SQL Editor'
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
  "message": "Permintaan berhasil..."
}
```

✅ **Success!** Function works!

---

#### **3.2 Test Validation Cases**

**Test A: Same Package (should fail)**
```sql
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 1,  -- Same as current
  p_notes := 'Test same package'
);
```

**Expected:** Error containing `PACKAGE_SAME_AS_CURRENT`

---

**Test B: Pending Request (should fail)**
```sql
-- Try submit again (ticket still pending)
SELECT submit_change_package(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid,
  p_package_id := 2,
  p_notes := 'Duplicate request'
);
```

**Expected:** Error containing `PENDING_REQUEST`

---

**Test C: Get Status**
```sql
SELECT get_active_change_package_status(
  p_auth_user_id := 'YOUR_AUTH_USER_ID'::uuid
);
```

**Expected:** Returns ticket JSON or NULL

---

#### **3.3 Cleanup Test Data**

**After testing:**
```sql
-- Delete test tickets
DELETE FROM ticket_perubahan_paket
WHERE ticket_id IN (
  SELECT id FROM tickets
  WHERE description LIKE '%Test%'
);

DELETE FROM tickets
WHERE description LIKE '%Test%';
```

---

### **STEP 4: Create Edge Function** ⏱️ 1 hour

#### **4.1 Create Function Directory**

**Manual approach (no CLI needed):**

Create file: `supabase/functions/change-package/index.ts`

See **[04-EdgeFunctions.md](./04-EdgeFunctions.md)** for complete code.

---

#### **4.2 Edge Function Code**

**File:** `supabase/functions/change-package/index.ts`

```typescript
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // Get Supabase client with user's token
    const authHeader = req.headers.get('Authorization')!
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      { global: { headers: { Authorization: authHeader } } }
    )

    // Get user
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()
    if (userError || !user) {
      throw new Error('Unauthorized')
    }

    // Parse request
    const { package_id, notes } = await req.json()

    // Call RPC function
    const { data, error } = await supabaseClient.rpc('submit_change_package', {
      p_auth_user_id: user.id,
      p_package_id: package_id,
      p_notes: notes || null
    })

    if (error) throw error

    return new Response(
      JSON.stringify({
        success: true,
        data: data
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    return new Response(
      JSON.stringify({
        success: false,
        message: error.message,
        error_code: error.message.split(':')[0] || 'UNKNOWN_ERROR'
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400
      }
    )
  }
})
```

---

#### **4.3 Deploy Edge Function**

**Option A: Via Dashboard (Manual)**

1. Go to: https://supabase.com/dashboard/project/rqmzvonjytyjdfhpqwvc/functions
2. Click "Create Function"
3. Name: `change-package`
4. Paste code from above
5. Click "Deploy"

**Option B: Via API (Using curl)**

See **[04-EdgeFunctions.md](./04-EdgeFunctions.md)** for deployment via API

---

#### **4.4 Test Edge Function**

**Using curl:**
```bash
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Test from curl"
  }'
```

**Expected:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "ticket_id": 124,
    ...
  }
}
```

---

### **STEP 5: Create Mobile Repository** ⏱️ 2 hours

See **[05-MobileIntegration.md](./05-MobileIntegration.md)** for complete implementation.

**Summary:**

1. Create `ChangePackageSupabaseRepository.java`
2. Use `SupabaseApiClient` with Edge Function endpoint
3. Update DTOs to match response format
4. Handle errors properly

---

### **STEP 6: Update Fragment** ⏱️ 1 hour

**Actions:**
- [ ] Update `ChangePackageFragment` to use new repository
- [ ] Add feature flag for A/B testing
- [ ] Test UI flow

See **[05-MobileIntegration.md](./05-MobileIntegration.md)** for details.

---

### **STEP 7: End-to-End Testing** ⏱️ 2 hours

See **[06-Testing.md](./06-Testing.md)** for complete test scenarios.

**Test Cases:**
- [ ] Navigation works
- [ ] Package list loads
- [ ] Active request check works
- [ ] Package selection works
- [ ] Submit success case
- [ ] Submit validation errors
- [ ] Notes input
- [ ] Back navigation
- [ ] Performance (< 1 second)
- [ ] Error handling

---

### **STEP 8: Deployment & Monitoring** ⏱️ Ongoing

#### **8.1 Gradual Rollout**

```
Day 1: Deploy to production
  └─ Feature flag OFF (use PHP)

Day 2-3: Internal testing
  └─ Feature flag ON for test accounts

Day 4-5: Beta users (10%)
  └─ Feature flag ON for 10% users
  └─ Monitor errors & performance

Day 6-7: Gradual rollout (50%)
  └─ If stable, increase to 50%

Day 8: Full rollout (100%)
  └─ Feature flag ON for all users
  └─ Monitor for 24 hours

Day 9: Deprecate PHP
  └─ Keep PHP as backup for 1 week
  └─ Then remove
```

---

#### **8.2 Monitoring Checklist**

**Metrics to watch:**
- [ ] Response time (target: < 1s, 95th percentile)
- [ ] Error rate (target: < 0.1%)
- [ ] Success rate (target: > 99.9%)
- [ ] User feedback (complaints, support tickets)

**Tools:**
- Supabase Dashboard → Functions → Logs
- Mobile Analytics (Firebase/Crashlytics)
- Backend monitoring (if available)

---

#### **8.3 Success Criteria**

Migration considered **SUCCESSFUL** when:

✅ **Performance:**
- Response time < 1 second (vs 1.8s before)
- 95th percentile < 1.5 seconds

✅ **Reliability:**
- Error rate < 0.1%
- No data inconsistencies reported
- All validations working correctly

✅ **User Experience:**
- No increase in support tickets
- Positive user feedback
- Feature adoption maintained

✅ **Technical:**
- All tests passing
- Documentation complete
- Rollback tested and ready

---

## ⚠️ **ROLLBACK PLAN**

If issues arise, follow **[07-Rollback.md](./07-Rollback.md)**

**Quick Rollback (5 minutes):**
```java
// In ChangePackageFragment.java
private static final boolean USE_SUPABASE_BACKEND = false; // ← Set to false

// This will use old PHP backend immediately
```

---

## 📊 **PROGRESS TRACKING**

Use **[08-Progress.md](./08-Progress.md)** to track current status.

**Current Status:**
```
✅ Step 1: Prerequisites         [COMPLETED]
✅ Step 2: RPC Functions          [COMPLETED]
🟡 Step 3: Test RPC Functions     [IN PROGRESS]
⬜ Step 4: Edge Function          [PENDING]
⬜ Step 5: Mobile Repository      [PENDING]
⬜ Step 6: Update Fragment        [PENDING]
⬜ Step 7: Testing                [PENDING]
⬜ Step 8: Deployment             [PENDING]
```

---

## 🔗 **DEPENDENCIES**

**Prerequisites:**
- ✅ Supabase project access
- ✅ SQL migration file created
- ⬜ Test user with data
- ⬜ Edge Function deployed
- ⬜ Mobile app updated

**External Dependencies:**
- Supabase platform availability
- No breaking changes in Supabase API
- Mobile build/release process

---

## 📅 **TIMELINE**

**Optimistic (everything smooth):** 2 days
**Realistic (with debugging):** 3 days
**Pessimistic (major issues):** 5 days

**Breakdown:**
```
Day 1:
  ├─ Morning: RPC functions & testing (Steps 2-3)
  └─ Afternoon: Edge Function creation & deploy (Step 4)

Day 2:
  ├─ Morning: Mobile repository implementation (Step 5)
  └─ Afternoon: Fragment update & initial testing (Step 6)

Day 3:
  ├─ Morning: Complete E2E testing (Step 7)
  └─ Afternoon: Deploy & monitor (Step 8)
```

---

## 🎯 **NEXT STEPS**

**Current Status:** Step 3 (Test RPC Functions)

**Next Actions:**
1. Apply migration SQL in Supabase Dashboard
2. Test RPC functions with real data
3. Create Edge Function
4. Continue with Step 4

**Read Next:** [03-RPCFunctions.md](./03-RPCFunctions.md) - Detailed RPC implementation

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [03-RPCFunctions.md](./03-RPCFunctions.md)
