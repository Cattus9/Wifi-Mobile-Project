# 07 - Rollback Plan & Emergency Response

> **Purpose:** Complete procedures untuk rollback migration jika terjadi masalah critical.

---

## 📋 **ROLLBACK OVERVIEW**

**When to Rollback:**
- ❌ Critical bugs affecting users
- ❌ Data corruption or inconsistency
- ❌ Performance worse than before
- ❌ Error rate > 5%
- ❌ Supabase service outage

**When NOT to Rollback:**
- ✅ Minor UI issues (can be fixed forward)
- ✅ Individual user errors (can be debugged)
- ✅ Performance slightly slower but acceptable
- ✅ Low error rate (< 1%)

---

## 🎯 **ROLLBACK LEVELS**

```
Level 1: INSTANT (5 minutes)
  └─ Feature flag switch (mobile only)

Level 2: QUICK (30 minutes)
  └─ Redeploy old APK + feature flag

Level 3: FULL (2 hours)
  └─ Remove all Supabase code + database cleanup
```

---

## 🚨 **LEVEL 1: INSTANT ROLLBACK (5 MINUTES)**

### **Scenario:**
- Production users experiencing errors
- Need immediate fix
- Can't wait for new build

### **Solution: Feature Flag Switch**

This is the **fastest** rollback method because it doesn't require redeploying the app. The feature flag is already in the code.

#### **Step 1: Identify Build Version**

Check which app version users have:
```
Dashboard: Firebase/Analytics > App Versions
Or: Play Store Console > Release Management > App Releases
```

#### **Step 2: Check Feature Flag in Code**

```java
// In ChangePackageFragment.java
private static final boolean USE_SUPABASE_BACKEND = true; // ← Current value
```

**If already deployed with `true`:**
- Users are using Supabase backend
- Cannot change without new build

**If deployed with `false`:**
- Users are using PHP backend
- Already rolled back ✅

#### **Step 3: Emergency Hotfix**

If already deployed with Supabase enabled, create hotfix:

```java
// ChangePackageFragment.java
private static final boolean USE_SUPABASE_BACKEND = false; // ← Change to false

// Commit and build
git add .
git commit -m "Hotfix: Rollback to PHP backend"
git tag -a v1.0.1-hotfix -m "Emergency rollback"
```

#### **Step 4: Emergency Build & Deploy**

```bash
# Build release
./gradlew assembleRelease

# Sign APK (use your keystore)
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore your-keystore.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  your-key-alias

# Upload to Play Store as emergency update
# Enable staged rollout: 10% → 50% → 100%
```

**Time to users:** 2-4 hours (Play Store review + rollout)

---

## ⚡ **LEVEL 2: QUICK ROLLBACK (30 MINUTES)**

### **Scenario:**
- Issues discovered before full production rollout
- Beta testing phase
- Internal testing

### **Solution: Redeploy Previous APK**

#### **Step 1: Get Previous Working Build**

```bash
# List recent builds
git tag -l

# Checkout previous version
git checkout v1.0.0  # Before Supabase changes
```

#### **Step 2: Rebuild & Redeploy**

```bash
# Clean build
./gradlew clean
./gradlew assembleRelease

# Install to test devices
./gradlew installRelease
```

#### **Step 3: Verify Rollback**

```
Logcat should show:
D/ChangePackageFragment: Using PHP backend
```

#### **Step 4: Notify Users**

```
In-app message:
"We've reverted to the previous version while we investigate issues.
Your data is safe. Thank you for your patience."
```

---

## 🔄 **LEVEL 3: FULL ROLLBACK (2 HOURS)**

### **Scenario:**
- Complete removal of Supabase integration
- Permanent rollback decision
- Major architectural issue discovered

### **Solution: Remove All Supabase Code**

#### **Step 1: Database Cleanup**

**Supabase Dashboard → SQL Editor:**

```sql
-- =============================================
-- CAUTION: This removes all Supabase changes!
-- =============================================

-- Drop Edge Function (do this in Functions dashboard manually)
-- Dashboard > Functions > change-package > Delete

-- Drop RPC Functions
DROP FUNCTION IF EXISTS submit_change_package(UUID, BIGINT, TEXT);
DROP FUNCTION IF EXISTS get_active_change_package_status(UUID);

-- Verify cleanup
SELECT routine_name
FROM information_schema.routines
WHERE routine_schema = 'public'
  AND routine_name IN (
    'submit_change_package',
    'get_active_change_package_status'
  );

-- Should return 0 rows
```

#### **Step 2: Remove Mobile Code**

```bash
# Remove Supabase-specific files
rm app/src/main/java/com/project/inet_mobile/data/packages/ChangePackageSupabaseRepository.java
rm app/src/main/java/com/project/inet_mobile/data/remote/SupabaseChangePackageService.java
rm app/src/main/java/com/project/inet_mobile/data/remote/dto/SupabaseChangePackageResponse.java

# Remove migration files
rm -rf supabase/migrations/20251203000001_submit_change_package_rpc.sql
rm -rf supabase/functions/change-package/

# Remove documentation
rm -rf Doc/DokumentasiUbahPaket/
```

#### **Step 3: Restore Original Code**

```java
// ChangePackageFragment.java
// Remove feature flag completely

@Override
public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
    binding = FragmentChangePackageBinding.inflate(inflater, container, false);

    // Always use PHP backend
    TokenStorage tokenStorage = new TokenStorage(requireContext());
    repository = new ChangePackageRepositoryImpl(tokenStorage);

    setupUI();
    loadPackages();
    checkActiveRequest();

    return binding.getRoot();
}
```

#### **Step 4: Update Dependencies**

```gradle
// app/build.gradle
// Remove Supabase dependencies if added

dependencies {
    // Keep existing dependencies
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    // ... other dependencies
}
```

#### **Step 5: Test PHP Backend**

```bash
# Verify PHP endpoint still works
curl -X POST \
  https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/customer/change-package.php \
  -H "Authorization: Bearer YOUR_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Rollback test"
  }'
```

#### **Step 6: Git Cleanup**

```bash
# Create rollback branch
git checkout -b rollback/remove-supabase

# Commit changes
git add .
git commit -m "Rollback: Remove Supabase integration, restore PHP backend"

# Merge to main
git checkout main
git merge rollback/remove-supabase

# Tag rollback
git tag -a v1.0.2-rollback -m "Full rollback to PHP backend"
git push origin main --tags
```

---

## 🔍 **ROLLBACK DECISION MATRIX**

| Severity | Impact | Users Affected | Rollback Level | Time |
|----------|--------|----------------|----------------|------|
| **P0 - Critical** | App crashes | > 50% | Level 1 (Hotfix) | 5 min |
| **P0 - Critical** | Data loss | Any | Level 1 (Hotfix) | 5 min |
| **P1 - High** | Feature broken | > 25% | Level 2 (Redeploy) | 30 min |
| **P1 - High** | Major errors | 10-25% | Level 2 (Redeploy) | 30 min |
| **P2 - Medium** | Slow performance | < 10% | Fix Forward | N/A |
| **P2 - Medium** | Minor bugs | < 5% | Fix Forward | N/A |
| **P3 - Low** | UI glitches | Any | Fix Forward | N/A |

---

## 📊 **MONITORING & ALERTS**

### **Key Metrics to Watch**

Set up alerts for these metrics:

#### **Error Rate Alert**
```
Threshold: > 5% error rate
Action: Investigate immediately
Rollback if: > 10% error rate for > 10 minutes
```

**Check via:**
- Firebase Crashlytics
- Supabase Functions logs
- Play Store crash reports

#### **Performance Alert**
```
Threshold: > 2 seconds response time (p95)
Action: Investigate performance
Rollback if: > 5 seconds consistently
```

**Check via:**
```java
// Add to repository
long startTime = System.currentTimeMillis();
response = repository.submitChangePackage(packageId, notes);
long duration = System.currentTimeMillis() - startTime;

if (duration > 2000) {
    Log.w(TAG, "SLOW REQUEST: " + duration + "ms");
    // Send to analytics
}
```

#### **User Feedback Alert**
```
Threshold: > 10 negative reviews mentioning "change package"
Action: Check Play Store reviews
Rollback if: Consistent complaints about feature
```

---

## 🔧 **TROUBLESHOOTING ROLLBACK ISSUES**

### **Issue 1: PHP Backend Down After Rollback**

**Symptoms:**
- Rolled back to PHP
- Still getting errors
- Endpoint unreachable

**Diagnosis:**
```bash
# Check PHP server status
curl -I https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/

# Check Ngrok status
curl https://api.ngrok.com/tunnels
```

**Solution:**
- Restart PHP server
- Restart Ngrok tunnel
- Update endpoint URL if Ngrok changed

---

### **Issue 2: Database in Inconsistent State**

**Symptoms:**
- Orphaned tickets
- Missing detail records
- Duplicate requests

**Diagnosis:**
```sql
-- Find orphaned tickets
SELECT t.id, t.customer_id, t.status, t.created_at
FROM tickets t
LEFT JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
WHERE t.kategori = 'perubahan_paket'
  AND tpp.ticket_id IS NULL;

-- Find incomplete requests
SELECT * FROM ticket_perubahan_paket
WHERE ticket_id NOT IN (SELECT id FROM tickets);
```

**Solution:**
```sql
-- Clean up orphaned tickets
DELETE FROM tickets
WHERE id IN (
  SELECT t.id FROM tickets t
  LEFT JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
  WHERE t.kategori = 'perubahan_paket'
    AND tpp.ticket_id IS NULL
);

-- Clean up orphaned details
DELETE FROM ticket_perubahan_paket
WHERE ticket_id NOT IN (SELECT id FROM tickets);
```

---

### **Issue 3: Users Have Mixed Versions**

**Symptoms:**
- Some users on Supabase
- Some users on PHP
- Inconsistent behavior

**Diagnosis:**
```
Check app versions in Play Store Console:
- Version 1.0.0 (PHP only)
- Version 1.1.0 (Supabase with flag)
- Version 1.2.0 (Supabase only)
```

**Solution:**
- Force update to latest version
- Use staged rollout to manage migration
- Communicate clearly in release notes

---

## 📝 **ROLLBACK CHECKLIST**

### **Before Rollback:**
- [ ] Document the issue (screenshots, logs, error messages)
- [ ] Identify severity (P0/P1/P2/P3)
- [ ] Check how many users affected
- [ ] Notify team/stakeholders
- [ ] Backup current state (git commit/tag)

### **During Rollback:**
- [ ] Choose appropriate rollback level
- [ ] Follow step-by-step procedures
- [ ] Test rollback on staging first (if possible)
- [ ] Monitor metrics during rollback
- [ ] Communicate with users (in-app/email)

### **After Rollback:**
- [ ] Verify PHP backend working
- [ ] Check error rates normalized
- [ ] Monitor for 24 hours
- [ ] Conduct post-mortem
- [ ] Update documentation
- [ ] Plan fix for original issue

---

## 📋 **POST-MORTEM TEMPLATE**

```markdown
# Post-Mortem: Change Package Rollback

**Date:** 2025-12-XX
**Duration:** X hours
**Impact:** X users affected

## Timeline

- **00:00** - Issue first detected
- **00:05** - Investigation started
- **00:15** - Decision to rollback
- **00:20** - Rollback completed
- **00:30** - Service restored

## Root Cause

[Detailed explanation of what went wrong]

## Impact

- Users affected: X
- Transactions failed: X
- Revenue impact: $X
- Reputation impact: X negative reviews

## Resolution

- Rolled back to PHP backend
- [Other steps taken]

## Lessons Learned

### What Went Well
- [Things that worked during rollback]

### What Went Wrong
- [Things that didn't work]

### Action Items
- [ ] [Action 1] - Owner: [Name] - Deadline: [Date]
- [ ] [Action 2] - Owner: [Name] - Deadline: [Date]

## Prevention

How to prevent this in the future:
- [Prevention measure 1]
- [Prevention measure 2]
```

---

## 🔗 **EMERGENCY CONTACTS**

### **Technical Team**
- Backend Developer: [Name/Contact]
- Mobile Developer: [Name/Contact]
- DevOps: [Name/Contact]

### **External Services**
- **Supabase Support:** https://supabase.com/dashboard/support
- **Supabase Status:** https://status.supabase.com/
- **Play Store Support:** https://support.google.com/googleplay/android-developer

### **Escalation Path**
```
Issue Detected
    ↓
Mobile Developer (15 min)
    ↓
Tech Lead (30 min)
    ↓
CTO (1 hour)
    ↓
CEO (2 hours)
```

---

## 📚 **REFERENCES**

- **Migration Plan:** 02-MigrationPlan.md
- **Testing Guide:** 06-Testing.md
- **Original Architecture:** 01-Context.md

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [08-Progress.md](./08-Progress.md)
