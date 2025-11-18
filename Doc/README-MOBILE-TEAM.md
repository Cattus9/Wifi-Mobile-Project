# 📱 WiFiNet Mobile App - Backend Integration Package

> **Package Version:** 1.0
> **Backend Team:** Backend Development Team
> **Mobile Team:** Android Development Team
> **Handoff Date:** 2025-11-15

---

## 🎯 **OVERVIEW**

Ini adalah **complete documentation package** untuk integrasi mobile app (Android) dengan backend WiFiNet Management System.

**Fitur yang Ready:**
- ✅ Payment Checkout API (Midtrans Snap)
- ✅ Payment Status Checking
- ✅ Webhook Integration (auto-update status)
- ✅ JWT Authentication
- ✅ Error Handling

---

## 📚 **DOCUMENTATION STRUCTURE**

### **📖 WAJIB DIBACA (Priority 1)**

1. **`MobileAPIDocumentation.md`** ⭐⭐⭐⭐⭐
   - **Main documentation untuk mobile team**
   - Complete API reference
   - Request/response examples (Java/Kotlin)
   - Error handling guide
   - Testing checklist
   - **START HERE!**

2. **`NgrokSetup.md`** ⭐⭐⭐⭐
   - Setup ngrok untuk testing
   - URL configuration
   - Mobile app network config
   - Troubleshooting guide

### **📋 Reference Documentation (Priority 2)**

3. **`MobilePaymentTesting.md`**
   - End-to-end testing guide
   - Test scenarios
   - Database queries untuk debugging
   - Success criteria

4. **`PaymentIntegration.md`**
   - Technical specification
   - Payload structure detail
   - Payment methods configuration
   - Metadata structure (JSONB)

5. **`APIRequirements.md`**
   - Overall API requirements
   - Future endpoints (tickets, profile, dll)
   - Mobile app feature mapping

6. **`AppOverview.md`**
   - Mobile app architecture overview
   - Flow pengguna
   - Dependencies & packages

---

## 🚀 **QUICK START GUIDE**

### **Step 1: Setup Base URL (5 minutes)**

**Android - Update `ApiConfig.java` atau `Constants.java`:**

```java
public class ApiConfig {
    // Development (Ngrok)
    public static final String BASE_URL =
        "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/";

    public static final String API_V1 = BASE_URL + "api/v1/";

    // Payment Endpoints
    public static final String ENDPOINT_CHECKOUT = API_V1 + "payments/checkout";
    public static final String ENDPOINT_STATUS = API_V1 + "payments/status";
}
```

**Kotlin:**

```kotlin
object ApiConfig {
    const val BASE_URL = "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/"
    const val API_V1 = "${BASE_URL}api/v1/"

    const val ENDPOINT_CHECKOUT = "${API_V1}payments/checkout"
    const val ENDPOINT_STATUS = "${API_V1}payments/status"
}
```

---

### **Step 2: Setup Network Security (Android) (5 minutes)**

**File:** `app/src/main/res/xml/network_security_config.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow ngrok HTTPS -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">unthoroughly-arachidic-aaden.ngrok-free.dev</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>

    <!-- Allow localhost untuk testing -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

**Update `AndroidManifest.xml`:**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ...>
```

---

### **Step 3: Test API Connection (10 minutes)**

**Simple Test (cURL):**

```bash
# Test endpoint reachable
curl https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/payments/status?invoice_id=1

# Expected: 401 Unauthorized (artinya endpoint work, hanya butuh auth)
```

**Test dari Android:**

```java
// Simple connectivity test
OkHttpClient client = new OkHttpClient();
Request request = new Request.Builder()
    .url(ApiConfig.ENDPOINT_STATUS + "?invoice_id=1")
    .build();

client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response) {
        Log.d("API_TEST", "Response code: " + response.code());
        // Expected: 401 (endpoint reachable)
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.e("API_TEST", "Connection failed: " + e.getMessage());
    }
});
```

---

### **Step 4: Get Test Credentials (5 minutes)**

**Contact backend team untuk:**

1. **Test Customer Account:**
   ```
   Email: customer.test@example.com
   Password: [ask backend team]
   ```

2. **Test Invoice ID:**
   ```
   Invoice ID: [ask backend team]
   Status: issued (belum paid)
   Amount: Rp 350,000
   ```

3. **Access Token:**
   ```
   Login via Supabase Auth SDK untuk dapat access_token
   Atau backend team bisa provide sample token
   ```

---

### **Step 5: First API Call (15 minutes)**

**Create Payment Session:**

```java
// Retrofit Interface
public interface ApiService {
    @POST("payments/checkout")
    Call<PaymentResponse> createPayment(
        @Header("Authorization") String token,
        @Body CheckoutRequest request
    );
}

// Request Model
public class CheckoutRequest {
    @SerializedName("invoice_id")
    private int invoiceId;

    @SerializedName("preferred_channel")
    private String preferredChannel;

    @SerializedName("return_url")
    private String returnUrl;

    public CheckoutRequest(int invoiceId, String preferredChannel, String returnUrl) {
        this.invoiceId = invoiceId;
        this.preferredChannel = preferredChannel;
        this.returnUrl = returnUrl;
    }
}

// Usage
CheckoutRequest request = new CheckoutRequest(
    123,                    // invoice_id dari backend team
    "qris",                 // payment method
    "inet://payment-result" // deep link app kamu
);

apiService.createPayment("Bearer " + accessToken, request)
    .enqueue(new Callback<PaymentResponse>() {
        @Override
        public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
            if (response.isSuccessful()) {
                String redirectUrl = response.body().getData().getRedirectUrl();
                Log.d("PAYMENT", "Snap URL: " + redirectUrl);
                // Open WebView dengan URL ini
            } else {
                Log.e("PAYMENT", "Error: " + response.code());
            }
        }

        @Override
        public void onFailure(Call<PaymentResponse> call, Throwable t) {
            Log.e("PAYMENT", "Failed: " + t.getMessage());
        }
    });
```

**Expected Result:**
```json
{
  "success": true,
  "data": {
    "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/abc123",
    "snap_token": "abc123xyz...",
    "payment_id": 456
  }
}
```

---

### **Step 6: Integration Checklist**

**Backend Team:**
- [x] Payment API endpoints ready
- [x] Ngrok tunnel running
- [x] Midtrans webhook configured
- [x] Documentation complete
- [ ] Provide test credentials to mobile team
- [ ] Provide sample invoice_id

**Mobile Team:**
- [ ] Update base URL di app
- [ ] Setup network security config
- [ ] Test API connectivity
- [ ] Implement checkout API call
- [ ] Implement WebView untuk Midtrans
- [ ] Implement deep link handling
- [ ] Implement status checking
- [ ] Test end-to-end flow

---

## 📖 **HOW TO READ DOCUMENTATION**

### **For Backend Integration (Payment Feature)**

```
1. Start with: MobileAPIDocumentation.md
   ├─ Read "Quick Start" section
   ├─ Read "Payment Endpoints" section
   ├─ Copy code examples (Java/Kotlin)
   └─ Implement in app

2. Setup environment: NgrokSetup.md
   ├─ Configure base URL
   ├─ Setup network security
   └─ Test connectivity

3. Testing: MobilePaymentTesting.md
   ├─ Test checkout flow
   ├─ Test payment success
   └─ Test error cases

4. Troubleshooting: All docs have troubleshooting section
   └─ Check common issues & fixes
```

### **For Future Features (Not Ready Yet)**

```
- Customer Dashboard API: See APIRequirements.md
- Ticket System API: See APIRequirements.md
- Package Management: See APIRequirements.md

These will be implemented in Phase 2.
```

---

## 🔑 **KEY ENDPOINTS SUMMARY**

| Endpoint | Method | Purpose | Status |
|----------|--------|---------|--------|
| `/api/v1/invoices` | GET | List customer invoices (paginated) | ✅ Ready |
| `/api/v1/invoices/detail` | GET | Invoice detail + payment methods | ✅ Ready |
| `/api/v1/payments/checkout` | POST | Create payment session | ✅ Ready |
| `/api/v1/payments/status` | GET | Check payment status | ✅ Ready |
| `/api/midtrans/notification.php` | POST | Webhook (backend only) | ✅ Ready |
| `/api/v1/customer/profile` | GET | Get customer profile | ⏳ Phase 2 |
| `/api/v1/tickets` | GET/POST | Ticket system | ⏳ Phase 2 |

**Legend:**
- ✅ Ready = Implemented & tested
- ⏳ Phase 2 = Planned for next sprint

**Updated:** 2025-11-15 - Added Invoice endpoints (list & detail)

---

## 🧪 **TESTING ENVIRONMENT**

### **Base URL (Development)**

```
https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/
```

**⚠️ IMPORTANT:**
- Ngrok free tier URL **berubah setiap restart**
- Backend team akan notify di group jika URL berubah
- Check ngrok status: `http://localhost:4040` (backend side)

### **Midtrans Sandbox**

```
Environment: Sandbox
Server Key: [contact backend team]
```

**Test Cards:**
- QRIS: Auto-success di sandbox
- Credit Card: 4811 1111 1111 1114 / CVV 123
- Bank Transfer: Auto-generate VA number

---

## 📞 **CONTACT & SUPPORT**

### **Backend Team Contact**

**For:**
- API issues
- Test credentials
- Ngrok URL changes
- Documentation questions
- Bug reports

**Contact via:** Group chat (preferred) atau email

### **Response Time SLA**

- Critical issues (API down): < 1 hour
- Test credentials request: < 4 hours
- Documentation questions: < 1 day
- Feature requests: Will be scheduled

---

## 🐛 **KNOWN ISSUES**

### **1. Ngrok Interstitial Warning (Free Tier)**

**Issue:**
Ngrok free tier shows "You are about to visit..." warning page

**Impact:**
Mobile WebView tidak bisa handle interstitial page

**Workaround:**
- Backend team akan upgrade ke paid tier ($8/month) jika diperlukan
- Atau deploy ke hosting real

**Status:** Low priority untuk development, akan diperbaiki sebelum production

---

### **2. Token Expiry**

**Issue:**
Access token expire setelah 1 hour

**Impact:**
API call return 401 setelah 1 hour

**Solution:**
- Implement token refresh di mobile app
- Atau re-login user

**Status:** Expected behavior, mobile team perlu handle

---

## 📦 **FILE STRUCTURE**

```
Doc/
├── README-MOBILE-TEAM.md          ← YOU ARE HERE (Start here!)
├── MobileAPIDocumentation.md      ← Main API reference
├── NgrokSetup.md                  ← Environment setup
├── MobilePaymentTesting.md        ← Testing guide
├── PaymentIntegration.md          ← Technical specs
├── APIRequirements.md             ← All endpoints (including future)
├── AppOverview.md                 ← App architecture overview
├── QueryPayments.md               ← Database schema (for backend team)
└── SchemaSupabase.md              ← Full database schema (for backend team)
```

**Which docs for mobile team:**
- ✅ Must read: `MobileAPIDocumentation.md`, `NgrokSetup.md`
- ✅ Recommended: `MobilePaymentTesting.md`
- ℹ️ Reference: `PaymentIntegration.md`, `APIRequirements.md`
- ❌ Skip: `QueryPayments.md`, `SchemaSupabase.md` (for backend team only)

---

## 🎯 **DEVELOPMENT WORKFLOW**

### **Day 1: Setup & First Test**

```
1. Read MobileAPIDocumentation.md (1 hour)
2. Update base URL di app (15 min)
3. Setup network security config (15 min)
4. Get test credentials dari backend team (request via chat)
5. Test first API call (30 min)
6. Report status to backend team
```

### **Day 2-3: Payment Integration**

```
1. Implement checkout API call
2. Implement WebView untuk Midtrans
3. Implement deep link handling
4. Test payment success flow
5. Test error cases
```

### **Day 4: Testing & Bug Fixing**

```
1. End-to-end testing
2. Report bugs to backend team
3. Fix integration issues
4. Re-test after fixes
```

### **Day 5: Code Review & Ready**

```
1. Code review
2. Final testing
3. Mark as ready for next phase
```

---

## ✅ **ACCEPTANCE CRITERIA**

Payment integration dianggap **COMPLETE** jika:

**Functional:**
- [ ] User bisa create payment session
- [ ] WebView bisa open Midtrans payment page
- [ ] User bisa complete payment (test via sandbox)
- [ ] App bisa detect payment success (via status API)
- [ ] App show success screen dengan invoice details
- [ ] App handle payment failure/cancellation
- [ ] App handle expired payment

**Technical:**
- [ ] All API calls have error handling
- [ ] Loading states implemented
- [ ] Retry mechanism implemented
- [ ] Deep link configured & working
- [ ] Network timeout handled
- [ ] Token expiry handled (401)

**Testing:**
- [ ] Tested with valid invoice
- [ ] Tested with invalid invoice (404)
- [ ] Tested with paid invoice (422)
- [ ] Tested payment success
- [ ] Tested payment cancellation
- [ ] Tested network offline scenario

---

## 🎉 **NEXT STEPS AFTER PAYMENT**

Setelah payment integration selesai, next features:

**Phase 2 (Priority):**
1. Customer Dashboard API
2. Invoice List API
3. Ticket System API
4. Profile Management API

**Phase 3 (Future):**
1. Push Notifications
2. Package Management
3. Payment History
4. Receipt Download (PDF)

Backend team akan provide documentation untuk Phase 2 setelah Phase 1 (payment) selesai.

---

## 📝 **VERSION HISTORY**

| Version | Date | Changes | By |
|---------|------|---------|-----|
| 1.0 | 2025-11-15 | Initial release - Payment API | Backend Team |
| - | TBD | Phase 2 - Dashboard & Tickets | TBD |
| - | TBD | Phase 3 - Notifications & More | TBD |

---

## 💡 **TIPS FOR MOBILE TEAM**

1. **Start Small:** Test checkout API dulu sebelum implement UI
2. **Use Postman:** Test API via Postman before coding
3. **Check Logs:** Monitor ngrok dashboard untuk debug request
4. **Ask Early:** Jangan ragu tanya backend team jika stuck
5. **Test Edge Cases:** Test error scenarios, tidak hanya happy path
6. **Save Tokens:** Save access token untuk testing (jangan login terus-terusan)

---

**Ready to Start? 🚀**

1. Read `MobileAPIDocumentation.md`
2. Setup base URL
3. Get test credentials
4. Start coding!

**Good Luck!** 💪

---

_Last updated: 2025-11-15 by Backend Development Team_
_Questions? Contact via group chat_
