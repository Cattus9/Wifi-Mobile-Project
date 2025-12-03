# 05 - Mobile App Integration

> **Purpose:** Complete guide untuk integrate Supabase Edge Function ke dalam Android mobile app.

---

## 📋 **OVERVIEW**

**What needs to change:**
- Create new repository using `SupabaseApiClient`
- Update DTOs to match Edge Function response
- Add feature flag for gradual rollout
- Update Fragment to use new repository
- Handle errors properly

**What stays the same:**
- UI/UX (no changes to layout)
- Navigation flow
- User experience
- ViewModel logic (minimal changes)

---

## 🎯 **ARCHITECTURE**

### **Before (PHP Backend):**
```
ChangePackageFragment
    ↓
ChangePackageRepository (interface)
    ↓
ChangePackageRepositoryImpl
    ↓
ChangePackageService (Retrofit)
    ↓
PaymentApiClient
    ↓
PHP Backend (change-package.php)
```

### **After (Supabase):**
```
ChangePackageFragment
    ↓
ChangePackageRepository (interface)
    ↓
ChangePackageSupabaseRepository (NEW!)
    ↓
SupabaseChangePackageService (NEW!)
    ↓
SupabaseApiClient
    ↓
Supabase Edge Function
```

### **Transition (Feature Flag):**
```
ChangePackageFragment
    ↓
if (USE_SUPABASE_BACKEND)
    ├─→ ChangePackageSupabaseRepository
    └─→ ChangePackageRepositoryImpl (old)
```

---

## 📝 **IMPLEMENTATION STEPS**

### **STEP 1: Create New Service Interface**

**File:** `app/src/main/java/com/project/inet_mobile/data/remote/SupabaseChangePackageService.java`

```java
package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.ChangePackageRequest;
import com.project.inet_mobile.data.remote.dto.SupabaseChangePackageResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SupabaseChangePackageService {

    /**
     * Submit change package request via Supabase Edge Function
     *
     * @param authHeader JWT token (automatically injected by AuthInterceptor)
     * @param request Request body containing package_id and notes
     * @return Response from Edge Function
     */
    @POST("functions/v1/change-package")
    Call<SupabaseChangePackageResponse> submitChangePackage(
        @Header("Authorization") String authHeader,
        @Body ChangePackageRequest request
    );
}
```

---

### **STEP 2: Create Response DTO**

**File:** `app/src/main/java/com/project/inet_mobile/data/remote/dto/SupabaseChangePackageResponse.java`

```java
package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response from Supabase Edge Function
 * Matches format from 04-EdgeFunctions.md
 */
public class SupabaseChangePackageResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ChangePackageData data;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("message")
    private String message;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public ChangePackageData getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Inner class for nested "data" object
     */
    public static class ChangePackageData {
        @SerializedName("success")
        private boolean success;

        @SerializedName("ticket_id")
        private long ticketId;

        @SerializedName("status")
        private String status;

        @SerializedName("current_package")
        private String currentPackage;

        @SerializedName("requested_package")
        private String requestedPackage;

        @SerializedName("notes")
        private String notes;

        @SerializedName("message")
        private String message;

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public long getTicketId() {
            return ticketId;
        }

        public String getStatus() {
            return status;
        }

        public String getCurrentPackage() {
            return currentPackage;
        }

        public String getRequestedPackage() {
            return requestedPackage;
        }

        public String getNotes() {
            return notes;
        }

        public String getMessage() {
            return message;
        }
    }
}
```

---

### **STEP 3: Create Supabase Repository**

**File:** `app/src/main/java/com/project/inet_mobile/data/packages/ChangePackageSupabaseRepository.java`

```java
package com.project.inet_mobile.data.packages;

import android.util.Log;

import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.remote.SupabaseChangePackageService;
import com.project.inet_mobile.data.remote.dto.ChangePackageRequest;
import com.project.inet_mobile.data.remote.dto.ChangePackageStatusResponse;
import com.project.inet_mobile.data.remote.dto.SupabaseChangePackageResponse;
import com.project.inet_mobile.data.session.TokenStorage;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Repository implementation using Supabase Edge Functions
 */
public class ChangePackageSupabaseRepository implements ChangePackageRepository {

    private static final String TAG = "ChangePackageSupabase";

    private final SupabaseChangePackageService service;
    private final TokenStorage tokenStorage;

    public ChangePackageSupabaseRepository(TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
        this.service = SupabaseApiClient.getInstance().create(SupabaseChangePackageService.class);
    }

    @Override
    public ChangePackageStatusResponse submitChangePackage(long packageId, String notes) throws IOException {
        Log.d(TAG, "Submitting change package request to Supabase");
        Log.d(TAG, "Package ID: " + packageId + ", Notes: " + notes);

        // Get JWT token
        String accessToken = tokenStorage.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            throw new IOException("User not authenticated");
        }

        // Create request
        ChangePackageRequest request = new ChangePackageRequest(packageId, notes);

        // Call Edge Function
        Call<SupabaseChangePackageResponse> call = service.submitChangePackage(
            "Bearer " + accessToken,
            request
        );

        Response<SupabaseChangePackageResponse> response = call.execute();

        // Handle response
        if (response.isSuccessful() && response.body() != null) {
            SupabaseChangePackageResponse supabaseResponse = response.body();

            if (supabaseResponse.isSuccess() && supabaseResponse.getData() != null) {
                // Convert to legacy response format
                return convertToLegacyResponse(supabaseResponse);
            } else {
                // Error from Edge Function
                String errorMsg = supabaseResponse.getMessage() != null
                    ? supabaseResponse.getMessage()
                    : "Unknown error";
                Log.e(TAG, "Edge Function error: " + errorMsg);
                throw new IOException(errorMsg);
            }
        } else {
            // HTTP error
            String errorBody = response.errorBody() != null
                ? response.errorBody().string()
                : "Unknown error";
            Log.e(TAG, "HTTP error: " + response.code() + " - " + errorBody);
            throw new IOException("Failed to submit request: " + errorBody);
        }
    }

    /**
     * Convert Supabase response to legacy format
     * This maintains compatibility with existing Fragment code
     */
    private ChangePackageStatusResponse convertToLegacyResponse(
        SupabaseChangePackageResponse supabaseResponse
    ) {
        SupabaseChangePackageResponse.ChangePackageData data = supabaseResponse.getData();

        ChangePackageStatusResponse legacyResponse = new ChangePackageStatusResponse();
        legacyResponse.setSuccess(true);
        legacyResponse.setMessage(data.getMessage());
        legacyResponse.setTicketId(data.getTicketId());
        legacyResponse.setStatus(data.getStatus());
        legacyResponse.setCurrentPackage(data.getCurrentPackage());
        legacyResponse.setRequestedPackage(data.getRequestedPackage());
        legacyResponse.setNotes(data.getNotes());

        return legacyResponse;
    }
}
```

---

### **STEP 4: Add Feature Flag to Fragment**

**File:** `app/src/main/java/com/project/inet_mobile/ui/account/ChangePackageFragment.java`

Add at the top of the class:

```java
public class ChangePackageFragment extends Fragment {

    private static final String TAG = "ChangePackageFragment";

    // ========================================
    // FEATURE FLAG: Switch between backends
    // ========================================
    private static final boolean USE_SUPABASE_BACKEND = true; // ← Change this to switch

    private FragmentChangePackageBinding binding;
    private ChangePackageAdapter adapter;
    private ChangePackageRepository repository; // ← Will be initialized based on flag

    // ... rest of code
}
```

Update `onCreateView` method:

```java
@Override
public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                         Bundle savedInstanceState) {
    binding = FragmentChangePackageBinding.inflate(inflater, container, false);

    // Initialize repository based on feature flag
    TokenStorage tokenStorage = new TokenStorage(requireContext());

    if (USE_SUPABASE_BACKEND) {
        Log.d(TAG, "Using Supabase backend");
        repository = new ChangePackageSupabaseRepository(tokenStorage);
    } else {
        Log.d(TAG, "Using PHP backend");
        repository = new ChangePackageRepositoryImpl(tokenStorage);
    }

    setupUI();
    loadPackages();
    checkActiveRequest();

    return binding.getRoot();
}
```

---

### **STEP 5: Update Error Handling**

Update the `submitChangePackage` method in Fragment to handle Supabase error codes:

```java
private void submitChangePackage(long packageId, String notes) {
    // Show loading
    binding.progressBar.setVisibility(View.VISIBLE);
    binding.buttonSubmit.setEnabled(false);

    new Thread(() -> {
        try {
            ChangePackageStatusResponse response = repository.submitChangePackage(packageId, notes);

            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSubmit.setEnabled(true);

                if (response.isSuccess()) {
                    showSuccessDialog(response);
                } else {
                    showErrorDialog(response.getMessage());
                }
            });

        } catch (IOException e) {
            requireActivity().runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);
                binding.buttonSubmit.setEnabled(true);

                // Parse error message
                String errorMessage = parseErrorMessage(e.getMessage());
                showErrorDialog(errorMessage);
            });
        }
    }).start();
}

/**
 * Parse error message from Supabase
 * Format: "ERROR_CODE: Human message"
 */
private String parseErrorMessage(String rawError) {
    if (rawError == null || rawError.isEmpty()) {
        return "Terjadi kesalahan. Silakan coba lagi.";
    }

    // Check for known error codes
    if (rawError.contains("OUTSTANDING_INVOICE")) {
        return "Harap selesaikan tagihan tertunda terlebih dahulu";
    } else if (rawError.contains("PENDING_REQUEST")) {
        return "Masih ada permintaan aktif yang sedang diproses";
    } else if (rawError.contains("PACKAGE_SAME_AS_CURRENT")) {
        return "Paket yang dipilih sama dengan paket aktif saat ini";
    } else if (rawError.contains("PACKAGE_NOT_AVAILABLE")) {
        return "Paket tidak tersedia";
    } else if (rawError.contains("CUSTOMER_NOT_FOUND")) {
        return "Data customer tidak ditemukan";
    } else if (rawError.contains("UNAUTHORIZED")) {
        return "Sesi telah berakhir. Silakan login kembali.";
    }

    // Extract message after colon if exists
    String[] parts = rawError.split(":");
    if (parts.length > 1) {
        return parts[1].trim();
    }

    return rawError;
}
```

---

## 🧪 **TESTING ON MOBILE**

### **Test 1: Build & Run**

```bash
# Via Android Studio:
# 1. Sync Gradle
# 2. Build > Make Project
# 3. Run > Run 'app'

# Via command line:
./gradlew assembleDebug
./gradlew installDebug
```

---

### **Test 2: Success Case**

**Steps:**
1. Login to app
2. Navigate to Account → Ubah Paket
3. Select different package
4. Add notes (optional)
5. Click Submit

**Expected:**
- Loading indicator shows
- Success dialog appears
- Shows ticket ID
- Shows package names
- Can go back to Account

**Logcat:**
```
D/ChangePackageFragment: Using Supabase backend
D/ChangePackageSupabase: Submitting change package request to Supabase
D/ChangePackageSupabase: Package ID: 2, Notes: Upgrade request
D/ChangePackageFragment: Success! Ticket ID: 123
```

---

### **Test 3: Error Cases**

#### **Test 3a: Same Package**
1. Note your current package
2. Try to change to same package
3. Click Submit

**Expected:**
- Error dialog: "Paket yang dipilih sama dengan paket aktif saat ini"

#### **Test 3b: Pending Request**
1. Submit a change request
2. Immediately submit another one
3. Click Submit

**Expected:**
- Error dialog: "Masih ada permintaan aktif yang sedang diproses"

#### **Test 3c: No Internet**
1. Turn off WiFi & mobile data
2. Try to submit
3. Click Submit

**Expected:**
- Error dialog: Network error message

---

### **Test 4: Feature Flag Switch**

**Test PHP Backend:**
```java
private static final boolean USE_SUPABASE_BACKEND = false;
```

**Test Supabase Backend:**
```java
private static final boolean USE_SUPABASE_BACKEND = true;
```

Both should work! (Until we deprecate PHP)

---

## 📊 **PERFORMANCE TESTING**

### **Measure Response Time**

Add timing logs:

```java
private void submitChangePackage(long packageId, String notes) {
    long startTime = System.currentTimeMillis();

    new Thread(() -> {
        try {
            ChangePackageStatusResponse response = repository.submitChangePackage(packageId, notes);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            Log.i(TAG, "Request completed in " + duration + "ms");

            // ... rest of code
        }
    }).start();
}
```

**Expected Performance:**
- **Supabase:** 500-1000ms
- **PHP:** 1500-2500ms

**Target:** Supabase should be ~40-60% faster

---

## 🐛 **TROUBLESHOOTING**

### **Issue 1: "User not authenticated"**

**Cause:** Token is null or expired

**Solution:**
```java
// Check token before calling
String token = tokenStorage.getAccessToken();
Log.d(TAG, "Access token: " + (token != null ? "exists" : "null"));

// If null, refresh or re-login
```

---

### **Issue 2: "Failed to submit request: 404"**

**Cause:** Edge Function not deployed or wrong URL

**Solution:**
```java
// Verify endpoint in SupabaseApiClient
// Should be: https://rqmzvonjytyjdfhpqwvc.supabase.co/

// Check Edge Function exists:
// Dashboard > Functions > change-package
```

---

### **Issue 3: Network errors**

**Cause:** No internet or Supabase down

**Solution:**
```java
// Add better error handling
try {
    response = repository.submitChangePackage(packageId, notes);
} catch (IOException e) {
    if (e instanceof java.net.UnknownHostException) {
        // No internet
        showErrorDialog("Tidak ada koneksi internet");
    } else if (e instanceof java.net.SocketTimeoutException) {
        // Timeout
        showErrorDialog("Koneksi timeout. Silakan coba lagi.");
    } else {
        // Other errors
        showErrorDialog(parseErrorMessage(e.getMessage()));
    }
}
```

---

### **Issue 4: JSON parsing errors**

**Cause:** Response format mismatch

**Solution:**
```java
// Add logging in repository
Log.d(TAG, "Raw response: " + response.body().toString());

// Check DTO annotations match Edge Function response
```

---

## ✅ **DEPLOYMENT CHECKLIST**

Before deploying to production:

**Code Quality:**
- [ ] All new files created
- [ ] Feature flag added
- [ ] Error handling comprehensive
- [ ] Logging added for debugging
- [ ] No hardcoded values

**Testing:**
- [ ] Build succeeds without errors
- [ ] Success case works
- [ ] All error cases handled
- [ ] Feature flag switch works
- [ ] Performance acceptable (< 1s)

**Documentation:**
- [ ] Code comments added
- [ ] TODOs resolved
- [ ] Team notified of changes

---

## 🚀 **GRADUAL ROLLOUT STRATEGY**

### **Phase 1: Development (Day 1-2)**
```java
// app/build.gradle
buildTypes {
    debug {
        buildConfigField "boolean", "USE_SUPABASE", "true"
    }
    release {
        buildConfigField "boolean", "USE_SUPABASE", "false"
    }
}
```

Test Supabase in debug builds only.

---

### **Phase 2: Beta Testing (Day 3-4)**
```java
// Enable for specific users
private boolean shouldUseSupabase() {
    String email = tokenStorage.getUserEmail();
    return email.endsWith("@company.com"); // Internal users only
}
```

---

### **Phase 3: Canary Release (Day 5-6)**
```java
// Enable for 10% of users
private boolean shouldUseSupabase() {
    String userId = tokenStorage.getUserId();
    int hash = userId.hashCode();
    return (hash % 100) < 10; // 10% of users
}
```

Monitor error rates and performance.

---

### **Phase 4: Full Rollout (Day 7+)**
```java
private static final boolean USE_SUPABASE_BACKEND = true; // All users
```

Monitor for 24-48 hours.

---

### **Phase 5: Deprecate PHP (Day 14+)**

Remove old code:
```java
// Delete files:
// - ChangePackageRepositoryImpl.java
// - ChangePackageService.java (PHP version)

// Remove feature flag
// Keep only Supabase implementation
```

---

## 📚 **REFERENCES**

- **Retrofit Documentation:** https://square.github.io/retrofit/
- **Gson Documentation:** https://github.com/google/gson
- **Android Threading:** https://developer.android.com/guide/background

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [06-Testing.md](./06-Testing.md)
