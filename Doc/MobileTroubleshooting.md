# Mobile App Troubleshooting Guide

**Issue:** "Gagal memuat invoice" di aplikasi Android
**Status:** Backend API verified ✅ (Postman test sukses)
**Root Cause:** Mobile client connectivity issue

---

## Quick Diagnosis Checklist

### 1. Verify Ngrok URL Masih Aktif ⚠️

**Problem:** Ngrok free tier URL berubah setiap restart
**Check:** Buka browser di HP, akses:
```
https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/headers.php
```

**Expected:** Jika muncul JSON → ngrok OK
**If fails:** Ngrok sudah berubah, perlu update `ApiConfig.BASE_URL`

---

### 2. Test Connectivity dari Mobile

**Create simple test endpoint untuk mobile:**

```kotlin
// Test 1: Ping test (no auth)
GET https://<ngrok>/Form-Handling/api/v1/debug/headers.php

// Test 2: Auth test (with token)
GET https://<ngrok>/Form-Handling/api/v1/debug/me.php
Header: Authorization: Bearer <token>
```

**Add logging di Android:**
```kotlin
// Di requestInvoiceDetail()
Log.d("API", "URL: ${ApiConfig.BASE_URL}")
Log.d("API", "Token: ${SessionManager.getToken()?.take(20)}...")

paymentApi.getInvoiceDetail(invoiceId).enqueue(object : Callback<ApiResponse<InvoiceDetailResponseData>> {
    override fun onResponse(call: Call<...>, response: Response<...>) {
        Log.d("API", "Response Code: ${response.code()}")
        Log.d("API", "Response Body: ${response.body()}")
        Log.d("API", "Error Body: ${response.errorBody()?.string()}")

        if (response.isSuccessful) {
            // existing code
        } else {
            Log.e("API", "Failed: ${response.code()} - ${response.message()}")
        }
    }

    override fun onFailure(call: Call<...>, t: Throwable) {
        Log.e("API", "Network Error: ${t.message}", t)
        Log.e("API", "Exception Type: ${t.javaClass.simpleName}")
    }
})
```

---

### 3. Common Android Issues

#### A. Network Security Config (REQUIRED untuk ngrok)

**File:** `res/xml/network_security_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow ngrok domains -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">ngrok-free.app</domain>
        <domain includeSubdomains="true">ngrok.app</domain>
        <domain includeSubdomains="true">ngrok-free.dev</domain>
        <domain includeSubdomains="true">ngrok.io</domain>
    </domain-config>

    <!-- Trust all certificates for development -->
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**File:** `AndroidManifest.xml`
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

#### B. Internet Permission

**File:** `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

#### C. OkHttp Logging (untuk debug)

**File:** `PaymentApiClient.kt` atau `ApiConfig.kt`
```kotlin
import okhttp3.logging.HttpLoggingInterceptor

val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY // HEADERS or BODY
}

val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)  // ← Pastikan ini ada
    .addInterceptor(authInterceptor)
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
```

**Dependency:** `build.gradle`
```gradle
implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
```

---

### 4. Token Issues

#### Check Token di Android:

```kotlin
// Di PembayaranFragment onCreate()
val token = SessionManager.getToken()
Log.d("TOKEN_CHECK", "Token exists: ${token != null}")
Log.d("TOKEN_CHECK", "Token value: ${token?.take(50)}...")

if (token.isNullOrEmpty()) {
    Log.e("TOKEN_CHECK", "NO TOKEN! User not logged in?")
    // Redirect to login
}
```

#### Verify Token Belum Expired:

Token JWT expire dalam 1 jam. Cara check:
```kotlin
// Decode JWT (tanpa verify signature)
fun isTokenExpired(token: String): Boolean {
    try {
        val parts = token.split(".")
        if (parts.size != 3) return true

        val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
        val json = JSONObject(payload)
        val exp = json.getLong("exp")
        val now = System.currentTimeMillis() / 1000

        Log.d("TOKEN_EXPIRY", "Expires: ${Date(exp * 1000)}")
        Log.d("TOKEN_EXPIRY", "Now: ${Date(now * 1000)}")

        return now > exp
    } catch (e: Exception) {
        return true
    }
}
```

**If expired:** Redirect ke login untuk refresh token

---

### 5. API Base URL

**Check current config:**
```kotlin
object ApiConfig {
    const val BASE_URL = "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/"
    //                    ↑ Pastikan ini sesuai ngrok terbaru
}
```

**Test di browser HP dulu:**
- Buka: `https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/headers.php`
- Jika error → ngrok berubah
- Jika sukses → BASE_URL sudah benar

---

### 6. InvoiceId Validation

**Check apakah invoiceId valid:**
```kotlin
// Di PembayaranFragment
companion object {
    fun newInstance(invoiceId: Int): PembayaranFragment {
        return PembayaranFragment().apply {
            arguments = Bundle().apply {
                putInt("INVOICE_ID", invoiceId)
            }
        }
    }
}

// Di onCreate/onViewCreated
val invoiceId = arguments?.getInt("INVOICE_ID") ?: 0
Log.d("INVOICE", "Received invoiceId: $invoiceId")

if (invoiceId == 0) {
    Log.w("INVOICE", "No invoiceId! Will fetch from API")
    // Fallback: call GET /api/v1/invoices
} else {
    requestInvoiceDetail(invoiceId)
}
```

---

## Step-by-Step Debugging

### Step 1: Test Simple Endpoint (No Auth)
```kotlin
// Create test function
fun testConnection() {
    val url = "${ApiConfig.BASE_URL}../debug/headers.php"
    Log.d("TEST", "Testing: $url")

    // Manual OkHttp call
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: okhttp3.Response) {
            Log.d("TEST", "Success! Code: ${response.code}")
            Log.d("TEST", "Body: ${response.body?.string()}")
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e("TEST", "Failed: ${e.message}", e)
        }
    })
}

// Call from onCreate
testConnection()
```

**Expected:** Log shows `Success! Code: 200`
**If fails:** Network/URL issue

### Step 2: Test Auth Endpoint
```kotlin
fun testAuth() {
    val token = SessionManager.getToken()
    val url = "${ApiConfig.BASE_URL}../debug/me.php"

    val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: okhttp3.Response) {
            val body = response.body?.string()
            Log.d("AUTH_TEST", "Code: ${response.code}")
            Log.d("AUTH_TEST", "Body: $body")

            if (response.code == 401) {
                Log.e("AUTH_TEST", "UNAUTHORIZED - Token invalid/expired")
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e("AUTH_TEST", "Failed: ${e.message}", e)
        }
    })
}
```

**Expected:** Code 200 + user data
**If 401:** Token issue
**If fails:** Network issue

### Step 3: Test Invoice Endpoint
```kotlin
fun testInvoice(invoiceId: Int) {
    val token = SessionManager.getToken()
    val url = "${ApiConfig.BASE_URL}invoices/detail.php?id=$invoiceId"

    Log.d("INVOICE_TEST", "URL: $url")
    Log.d("INVOICE_TEST", "Token: ${token?.take(30)}...")

    val request = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $token")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onResponse(call: Call, response: okhttp3.Response) {
            val body = response.body?.string()
            Log.d("INVOICE_TEST", "Code: ${response.code}")
            Log.d("INVOICE_TEST", "Body: $body")
        }

        override fun onFailure(call: Call, e: IOException) {
            Log.e("INVOICE_TEST", "Failed: ${e.message}", e)
        }
    })
}

// Call with valid ID
testInvoice(10) // Leon's invoice
```

---

## Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `Unable to resolve host` | Ngrok URL berubah / No internet | Update BASE_URL / Check WiFi |
| `CLEARTEXT communication not permitted` | Android block HTTP | Add network_security_config.xml |
| `401 Unauthorized` | Token expired/invalid | Re-login untuk get new token |
| `403 Forbidden` | Token valid tapi bukan customer | Check user role di Supabase |
| `404 Not Found` | Wrong endpoint path | Check BASE_URL ends with `/api/v1/` |
| `SSL handshake failed` | Certificate issue | Add ngrok cert to trust anchors |
| `java.net.SocketTimeoutException` | Server lambat/down | Increase timeout / Check server |

---

## Quick Fix Checklist

- [ ] Network security config added
- [ ] Internet permission in manifest
- [ ] OkHttp logging interceptor enabled
- [ ] Token exists and not expired
- [ ] BASE_URL matches current ngrok URL
- [ ] Test endpoint dari browser HP works
- [ ] InvoiceId passed correctly to fragment
- [ ] Logcat shows API calls (URL, code, body)

---

## Contact Backend Team

Jika semua checklist OK tapi masih error, kirim ke backend team:

1. **Logcat output** dari test functions above
2. **Screenshot** error toast
3. **Current ngrok URL** yang dipakai
4. **Token** (first 50 chars)
5. **Android version** & device model

Backend akan verify dari sisi server logs.
