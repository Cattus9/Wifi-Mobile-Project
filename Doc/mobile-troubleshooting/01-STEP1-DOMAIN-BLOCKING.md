# STEP 1: Fix Android Domain Blocking

**Time:** 5-10 minutes
**Difficulty:** Easy
**Goal:** Allow Android app to connect to ngrok backend

---

## Problem

Android blocks ngrok domain by default → Request tidak keluar dari app → "Gagal memuat invoice"

**Symptoms:**
- ❌ No OkHttp logs in Logcat
- ❌ Request tidak sampai ke backend
- ❌ Toast error "Gagal memuat invoice"
- ❌ No network activity visible

---

## Solution Overview

Add **Network Security Config** file to whitelist ngrok domains.

**What we'll do:**
1. Create XML config file (2 min)
2. Update AndroidManifest.xml (1 min)
3. Rebuild project (2 min)
4. Test connection (5 min)

---

## Step-by-Step Instructions

### ✅ Action 1: Create Network Security Config

**Create folder (if not exists):**
```
app/src/main/res/xml/
```

**Create file:**
```
app/src/main/res/xml/network_security_config.xml
```

**Content:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow ngrok domains -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">ngrok-free.dev</domain>
        <domain includeSubdomains="true">ngrok-free.app</domain>
        <domain includeSubdomains="true">ngrok.app</domain>
        <domain includeSubdomains="true">ngrok.io</domain>
    </domain-config>

    <!-- Default config -->
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
</network-security-config>
```

**OR copy from template:**
```
templates/network_security_config.xml → res/xml/
```

---

### ✅ Action 2: Update AndroidManifest.xml

**Open:** `app/src/main/AndroidManifest.xml`

**Add 2 attributes to `<application>` tag:**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="true"
    ... existing attributes ...>
```

**⚠️ IMPORTANT:**
- No `.xml` extension in the reference!
- Must be `@xml/network_security_config` (NOT `@xml/network_security_config.xml`)

**Full example:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.yourpackage">

    <!-- Verify these exist -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <application
        android:name=".MyApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme"

        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="true">

        <!-- Activities -->
        <activity android:name=".MainActivity">
            <!-- ... -->
        </activity>

    </application>
</manifest>
```

---

### ✅ Action 3: Rebuild Project

**In Android Studio:**
1. Click: **Build** → **Clean Project** (wait until done)
2. Click: **Build** → **Rebuild Project** (wait until done)
3. Run app

**Command line:**
```bash
./gradlew clean
./gradlew build
```

⚠️ **Don't skip this!** XML files not picked up without rebuild.

---

### ✅ Action 4: Test the Fix

**Add test code to MainActivity:**

```kotlin
package com.yourpackage

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Test backend connectivity
        testNetworkFix()
    }

    private fun testNetworkFix() {
        lifecycleScope.launch {
            try {
                Log.d("STEP1_TEST", "Testing backend connection...")

                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/ping.php")
                    val connection = url.openConnection() as HttpURLConnection

                    connection.apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                    }

                    val code = connection.responseCode
                    val body = connection.inputStream.bufferedReader().use { it.readText() }

                    connection.disconnect()

                    Pair(code, body)
                }

                // Success!
                Log.d("STEP1_TEST", "════════════════════════════════")
                Log.d("STEP1_TEST", "✅ STEP 1 SUCCESS!")
                Log.d("STEP1_TEST", "════════════════════════════════")
                Log.d("STEP1_TEST", "Response Code: ${result.first}")
                Log.d("STEP1_TEST", "Response Body: ${result.second}")
                Log.d("STEP1_TEST", "")
                Log.d("STEP1_TEST", "✅ Domain blocking fixed!")
                Log.d("STEP1_TEST", "✅ Network connectivity working!")
                Log.d("STEP1_TEST", "✅ Ready for STEP 2")
                Log.d("STEP1_TEST", "════════════════════════════════")

            } catch (e: javax.net.ssl.SSLHandshakeException) {
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "❌ STEP 1 FAILED: SSL Error")
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "Error: ${e.message}")
                Log.e("STEP1_TEST", "")
                Log.e("STEP1_TEST", "Possible causes:")
                Log.e("STEP1_TEST", "1. network_security_config.xml not found")
                Log.e("STEP1_TEST", "2. AndroidManifest missing attribute")
                Log.e("STEP1_TEST", "3. Project not rebuilt")
                Log.e("STEP1_TEST", "")
                Log.e("STEP1_TEST", "Fix:")
                Log.e("STEP1_TEST", "- Verify res/xml/network_security_config.xml exists")
                Log.e("STEP1_TEST", "- Check AndroidManifest has:")
                Log.e("STEP1_TEST", "  android:networkSecurityConfig=\"@xml/network_security_config\"")
                Log.e("STEP1_TEST", "- Build → Clean Project → Rebuild Project")
                Log.e("STEP1_TEST", "════════════════════════════════")

            } catch (e: java.net.UnknownHostException) {
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "❌ STEP 1 FAILED: Unknown Host")
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "Error: ${e.message}")
                Log.e("STEP1_TEST", "")
                Log.e("STEP1_TEST", "Possible causes:")
                Log.e("STEP1_TEST", "1. Ngrok URL expired/changed")
                Log.e("STEP1_TEST", "2. No internet connection")
                Log.e("STEP1_TEST", "3. DNS resolution failed")
                Log.e("STEP1_TEST", "")
                Log.e("STEP1_TEST", "Fix:")
                Log.e("STEP1_TEST", "- Test URL in browser:")
                Log.e("STEP1_TEST", "  https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/ping.php")
                Log.e("STEP1_TEST", "- Check internet connection")
                Log.e("STEP1_TEST", "- Update ApiConfig.BASE_URL if ngrok changed")
                Log.e("STEP1_TEST", "════════════════════════════════")

            } catch (e: java.net.SocketTimeoutException) {
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "❌ STEP 1 FAILED: Timeout")
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "Error: ${e.message}")
                Log.e("STEP1_TEST", "")
                Log.e("STEP1_TEST", "Possible causes:")
                Log.e("STEP1_TEST", "1. Backend server down")
                Log.e("STEP1_TEST", "2. Network too slow")
                Log.e("STEP1_TEST", "")
                Log.e("STEP1_TEST", "Fix:")
                Log.e("STEP1_TEST", "- Contact backend team")
                Log.e("STEP1_TEST", "- Check XAMPP is running")
                Log.e("STEP1_TEST", "════════════════════════════════")

            } catch (e: Exception) {
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "❌ STEP 1 FAILED: Unknown Error")
                Log.e("STEP1_TEST", "════════════════════════════════")
                Log.e("STEP1_TEST", "Error Type: ${e.javaClass.simpleName}")
                Log.e("STEP1_TEST", "Message: ${e.message}")
                Log.e("STEP1_TEST", "")
                e.printStackTrace()
                Log.e("STEP1_TEST", "════════════════════════════════")
            }
        }
    }
}
```

---

## Expected Results

### ✅ Success

**Logcat output:**
```
D/STEP1_TEST: ════════════════════════════════
D/STEP1_TEST: ✅ STEP 1 SUCCESS!
D/STEP1_TEST: ════════════════════════════════
D/STEP1_TEST: Response Code: 200
D/STEP1_TEST: Response Body: {"success":true,"message":"Backend is alive!"...}
D/STEP1_TEST:
D/STEP1_TEST: ✅ Domain blocking fixed!
D/STEP1_TEST: ✅ Network connectivity working!
D/STEP1_TEST: ✅ Ready for STEP 2
D/STEP1_TEST: ════════════════════════════════
```

**Next Action:**
→ Open `02-STEP2-LOGGING.md`

---

### ❌ Failure - SSL Error

**Logcat shows:**
```
E/STEP1_TEST: ❌ STEP 1 FAILED: SSL Error
```

**Fix:**
1. Check file exists: `res/xml/network_security_config.xml`
2. Check AndroidManifest has attribute (no typo!)
3. Rebuild: Build → Clean → Rebuild
4. Run again

---

### ❌ Failure - Unknown Host

**Logcat shows:**
```
E/STEP1_TEST: ❌ STEP 1 FAILED: Unknown Host
```

**Fix:**
1. Test ngrok URL in browser
2. Check internet connection
3. Update BASE_URL if ngrok changed

---

## Verification Checklist

Before reporting success:

- [ ] File `res/xml/network_security_config.xml` exists
- [ ] AndroidManifest has `android:networkSecurityConfig="@xml/network_security_config"`
- [ ] AndroidManifest has `android:usesCleartextTraffic="true"`
- [ ] AndroidManifest has `<uses-permission android:name="android.permission.INTERNET" />`
- [ ] Project rebuilt (Clean + Rebuild)
- [ ] Test code added to MainActivity
- [ ] App runs without crash
- [ ] Logcat shows success message
- [ ] Response code = 200
- [ ] Response body contains `"success":true`

---

## Report Results

**If SUCCESS:**
```
✅ STEP 1 COMPLETE
Logcat confirms: Response Code: 200
Ready for STEP 2
```

**If FAIL:**
```
❌ STEP 1 FAILED
Error: [SSLHandshakeException / UnknownHostException / etc]
Logcat output:
[paste full error logs]
```

---

## Next Steps

**After SUCCESS:**
→ Continue to: `02-STEP2-LOGGING.md`

**If STUCK:**
→ Read: `complete-troubleshooting-guide.md`
→ Or contact backend team with Logcat output

---

**Time estimate:** 5-10 minutes
**Success rate:** 95% (if followed correctly)

Good luck! 🚀
