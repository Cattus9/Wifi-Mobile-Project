# WiFiNet Mobile API Documentation

> **API Version:** 1.0
> **Base URL (Development):** `https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/`
> **Base URL (Production):** `TBD`
> **Authentication:** Bearer Token (JWT via Supabase Auth)
> **Last Updated:** 2025-11-15

---

## 📋 **TABLE OF CONTENTS**

1. [Quick Start](#quick-start)
2. [Authentication](#authentication)
3. [Invoice Endpoints](#invoice-endpoints)
   - [List Invoices](#1-list-invoices)
   - [Invoice Detail](#2-invoice-detail)
4. [Payment Endpoints](#payment-endpoints)
   - [Create Payment Session](#1-create-payment-session)
   - [Check Payment Status](#2-check-payment-status)
5. [Error Handling](#error-handling)
6. [Complete Flow Diagram](#complete-flow-diagram)
7. [Testing Guide](#testing-guide)
8. [Environment Setup](#environment-setup)

---

## 🚀 **QUICK START**

### **Base URL Configuration**

```java
// Android - ApiConfig.java
public class ApiConfig {
    // Development (Ngrok)
    public static final String BASE_URL =
        "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/";

    public static final String API_V1 = BASE_URL + "api/v1/";

    // Invoice Endpoints
    public static final String ENDPOINT_INVOICES = API_V1 + "invoices";
    public static final String ENDPOINT_INVOICE_DETAIL = API_V1 + "invoices/detail";

    // Payment Endpoints
    public static final String ENDPOINT_CHECKOUT = API_V1 + "payments/checkout";
    public static final String ENDPOINT_STATUS = API_V1 + "payments/status";
}
```

```kotlin
// Kotlin - ApiConfig.kt
object ApiConfig {
    const val BASE_URL =
        "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/"
    const val API_V1 = "${BASE_URL}api/v1/"

    // Invoice Endpoints
    const val ENDPOINT_INVOICES = "${API_V1}invoices"
    const val ENDPOINT_INVOICE_DETAIL = "${API_V1}invoices/detail"

    // Payment Endpoints
    const val ENDPOINT_CHECKOUT = "${API_V1}payments/checkout"
    const val ENDPOINT_STATUS = "${API_V1}payments/status"
}
```

---

## 🔐 **AUTHENTICATION**

### **Authentication Method**

All API requests require **JWT Bearer Token** dari Supabase Auth.

### **How to Get Token**

**Option 1: Direct Supabase Auth** (Recommended)

```java
// Login menggunakan Supabase Auth SDK
SupabaseAuthService authService = new SupabaseAuthService(supabaseUrl, supabaseAnonKey);

SignInResult result = authService.signInWithPassword(email, password);
String accessToken = result.getAccessToken();
String refreshToken = result.getRefreshToken();
```

**Option 2: Via Backend Endpoint** (If implemented)

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "customer@example.com",
  "password": "password123"
}

Response:
{
  "success": true,
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "...",
    "user": {
      "id": 123,
      "email": "customer@example.com",
      "role": "customer"
    }
  }
}
```

### **Using Token in Requests**

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

```java
// OkHttp Interceptor
public class AuthInterceptor implements Interceptor {
    private String accessToken;

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json");

        return chain.proceed(builder.build());
    }
}
```

### **Token Refresh**

Access token expire setelah **1 hour**. Gunakan refresh token untuk mendapatkan access token baru.

```java
// Refresh token via Supabase
String newAccessToken = authService.refreshToken(refreshToken);
```

---

## 📄 **INVOICE ENDPOINTS**

### **1. List Invoices**

Mendapatkan daftar semua invoice customer dengan pagination.

#### **Endpoint**

```
GET /api/v1/invoices
```

#### **Headers**

```
Authorization: Bearer {access_token}
```

#### **Query Parameters**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `limit` | integer | No | 20 | Items per page (max: 100) |
| `offset` | integer | No | 0 | Skip N items |
| `status` | string | No | all | Filter by status: `issued`, `overdue`, `paid`, `cancelled` |

#### **Response Success (200 OK)**

```json
{
  "success": true,
  "message": "Invoices retrieved successfully",
  "data": {
    "items": [
      {
        "invoice_id": 789,
        "invoice_number": "INV-000789",
        "month_label": "November 2025",
        "amount": 350000.00,
        "due_date": "2025-11-30",
        "status": "issued",
        "description": "Paket Premium - November 2025",
        "paid_at": null,
        "created_at": "2025-11-01 10:00:00",
        "is_overdue": false,
        "can_pay": true
      },
      {
        "invoice_id": 788,
        "invoice_number": "INV-000788",
        "month_label": "Oktober 2025",
        "amount": 350000.00,
        "due_date": "2025-10-31",
        "status": "paid",
        "description": "Paket Premium - Oktober 2025",
        "paid_at": "2025-10-15 14:30:00",
        "created_at": "2025-10-01 10:00:00",
        "is_overdue": false,
        "can_pay": false
      }
    ],
    "pagination": {
      "total": 24,
      "limit": 20,
      "offset": 0,
      "current_page": 1,
      "total_pages": 2
    }
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `invoice_id` | integer | Invoice ID |
| `invoice_number` | string | Formatted invoice number (INV-XXXXXX) |
| `month_label` | string | Bulan tagihan (e.g., "November 2025") |
| `amount` | decimal | Total tagihan |
| `due_date` | date | Tanggal jatuh tempo |
| `status` | string | Status invoice: `issued`, `overdue`, `paid`, `cancelled` |
| `description` | string | Deskripsi tagihan |
| `paid_at` | datetime | Waktu pembayaran (null jika belum paid) |
| `is_overdue` | boolean | Apakah sudah lewat jatuh tempo |
| `can_pay` | boolean | Apakah bisa dibayar (true jika issued/overdue) |

#### **Invoice Status Values**

| Status | Description | Can Pay |
|--------|-------------|---------|
| `draft` | Draft, belum diterbitkan | No |
| `issued` | Sudah diterbitkan, belum dibayar | Yes |
| `overdue` | Lewat jatuh tempo, belum dibayar | Yes |
| `paid` | Sudah dibayar | No |
| `cancelled` | Dibatalkan | No |

#### **Example Usage (Java)**

```java
// Retrofit Interface
@GET("invoices")
Call<InvoiceListResponse> getInvoices(
    @Header("Authorization") String token,
    @Query("limit") Integer limit,
    @Query("offset") Integer offset,
    @Query("status") String status
);

// Usage - Get unpaid invoices
apiService.getInvoices("Bearer " + accessToken, 20, 0, "issued")
    .enqueue(new Callback<InvoiceListResponse>() {
        @Override
        public void onResponse(Call<InvoiceListResponse> call, Response<InvoiceListResponse> response) {
            if (response.isSuccessful()) {
                List<Invoice> invoices = response.body().getData().getItems();
                PaginationInfo pagination = response.body().getData().getPagination();

                // Display invoices in RecyclerView
                adapter.setInvoices(invoices);

                // Update pagination UI
                updatePagination(pagination);
            }
        }

        @Override
        public void onFailure(Call<InvoiceListResponse> call, Throwable t) {
            showError("Failed to load invoices");
        }
    });
```

#### **UI Implementation Tips**

**RecyclerView Adapter:**
```java
public class InvoiceAdapter extends RecyclerView.Adapter<InvoiceViewHolder> {
    @Override
    public void onBindViewHolder(InvoiceViewHolder holder, int position) {
        Invoice invoice = invoices.get(position);

        holder.monthLabel.setText(invoice.getMonthLabel());
        holder.amount.setText("Rp " + formatCurrency(invoice.getAmount()));
        holder.dueDate.setText("Jatuh tempo: " + formatDate(invoice.getDueDate()));

        // Status badge
        holder.statusBadge.setText(getStatusLabel(invoice.getStatus()));
        holder.statusBadge.setBackgroundColor(getStatusColor(invoice.getStatus()));

        // Show "Bayar" button only if can_pay = true
        if (invoice.canPay()) {
            holder.payButton.setVisibility(View.VISIBLE);
            holder.payButton.setOnClickListener(v -> {
                // Navigate to invoice detail
                Intent intent = new Intent(context, InvoiceDetailActivity.class);
                intent.putExtra("invoice_id", invoice.getInvoiceId());
                context.startActivity(intent);
            });
        } else {
            holder.payButton.setVisibility(View.GONE);
        }

        // Overdue indicator
        if (invoice.isOverdue()) {
            holder.overdueIndicator.setVisibility(View.VISIBLE);
        }
    }
}
```

---

### **2. Invoice Detail**

Mendapatkan detail invoice termasuk payment methods yang tersedia.

#### **Endpoint**

```
GET /api/v1/invoices/detail?id={invoice_id}
```

#### **Headers**

```
Authorization: Bearer {access_token}
```

#### **Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `id` | integer | **Yes** | Invoice ID yang ingin dilihat |

#### **Response Success (200 OK)**

```json
{
  "success": true,
  "message": "Invoice retrieved successfully",
  "data": {
    "invoice_id": 789,
    "invoice_number": "INV-000789",
    "month_label": "November 2025",
    "amount": 350000.00,
    "due_date": "2025-11-30",
    "status": "issued",
    "description": "Paket Premium - November 2025",
    "created_at": "2025-11-01 10:00:00",
    "paid_at": null,
    "is_overdue": false,
    "can_pay": true,
    "customer": {
      "name": "John Doe",
      "email": "john@example.com"
    },
    "payment_methods": [
      {
        "type": "qris",
        "label": "QRIS",
        "description": "Scan QR dengan aplikasi e-wallet",
        "icon": "qris",
        "fee": 0
      },
      {
        "type": "gopay",
        "label": "GoPay",
        "description": "Bayar dengan GoPay",
        "icon": "gopay",
        "fee": 0
      },
      {
        "type": "bank_transfer_bca",
        "label": "BCA Virtual Account",
        "description": "Transfer via BCA",
        "icon": "bca",
        "fee": 0
      }
    ],
    "latest_payment": {
      "payment_id": 456,
      "order_id": "INV-20251115-000789",
      "status": "pending",
      "preferred_channel": "qris",
      "expires_at": "2025-11-16 14:30:00",
      "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/abc123",
      "created_at": "2025-11-15 14:00:00"
    }
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `latest_payment` | object | Latest payment attempt (null jika belum pernah checkout) |
| `payment_methods` | array | List of available payment methods |

**Payment Methods:**

| Type | Label | Description |
|------|-------|-------------|
| `qris` | QRIS | Scan QR dengan e-wallet apapun |
| `gopay` | GoPay | Bayar dengan GoPay |
| `shopeepay` | ShopeePay | Bayar dengan ShopeePay |
| `bank_transfer_bca` | BCA Virtual Account | Transfer via BCA |
| `bank_transfer_bni` | BNI Virtual Account | Transfer via BNI |
| `bank_transfer_bri` | BRI Virtual Account | Transfer via BRI |
| `bank_transfer_permata` | Permata Virtual Account | Transfer via Permata |
| `bank_transfer_mandiri` | Mandiri Bill Payment | Bayar via Mandiri |

#### **Response Error**

**404 Not Found:**
```json
{
  "success": false,
  "message": "Invoice not found",
  "error_code": "NOT_FOUND"
}
```

**403 Forbidden:**
```json
{
  "success": false,
  "message": "You do not have access to this invoice",
  "error_code": "FORBIDDEN"
}
```

#### **Example Usage (Java)**

```java
// Retrofit Interface
@GET("invoices/detail")
Call<InvoiceDetailResponse> getInvoiceDetail(
    @Header("Authorization") String token,
    @Query("id") int invoiceId
);

// Usage
apiService.getInvoiceDetail("Bearer " + accessToken, invoiceId)
    .enqueue(new Callback<InvoiceDetailResponse>() {
        @Override
        public void onResponse(Call<InvoiceDetailResponse> call, Response<InvoiceDetailResponse> response) {
            if (response.isSuccessful()) {
                InvoiceDetail invoice = response.body().getData();

                // Display invoice info
                tvMonthLabel.setText(invoice.getMonthLabel());
                tvAmount.setText("Rp " + formatCurrency(invoice.getAmount()));
                tvDueDate.setText("Jatuh tempo: " + formatDate(invoice.getDueDate()));

                // Show payment methods
                PaymentMethodAdapter adapter = new PaymentMethodAdapter(invoice.getPaymentMethods());
                recyclerPaymentMethods.setAdapter(adapter);

                // Handle "Bayar" button
                if (invoice.canPay()) {
                    btnPay.setVisibility(View.VISIBLE);
                    btnPay.setOnClickListener(v -> {
                        // Show payment method selector
                        showPaymentMethodDialog(invoice);
                    });
                } else {
                    btnPay.setVisibility(View.GONE);
                    tvPaidInfo.setVisibility(View.VISIBLE);
                    tvPaidInfo.setText("Dibayar pada: " + formatDateTime(invoice.getPaidAt()));
                }

                // If there's pending payment, show option to continue
                if (invoice.getLatestPayment() != null &&
                    "pending".equals(invoice.getLatestPayment().getStatus())) {

                    btnContinuePayment.setVisibility(View.VISIBLE);
                    btnContinuePayment.setOnClickListener(v -> {
                        // Open existing payment session
                        String redirectUrl = invoice.getLatestPayment().getRedirectUrl();
                        openMidtransPayment(redirectUrl);
                    });
                }
            }
        }

        @Override
        public void onFailure(Call<InvoiceDetailResponse> call, Throwable t) {
            showError("Failed to load invoice detail");
        }
    });
```

#### **Payment Method Selector Dialog**

```java
private void showPaymentMethodDialog(InvoiceDetail invoice) {
    BottomSheetDialog dialog = new BottomSheetDialog(this);
    View view = getLayoutInflater().inflate(R.layout.dialog_payment_methods, null);

    RecyclerView recyclerView = view.findViewById(R.id.recycler_payment_methods);
    PaymentMethodAdapter adapter = new PaymentMethodAdapter(
        invoice.getPaymentMethods(),
        paymentMethod -> {
            // User selected payment method
            dialog.dismiss();

            // Call checkout API
            createPaymentSession(invoice.getInvoiceId(), paymentMethod.getType());
        }
    );

    recyclerView.setAdapter(adapter);
    dialog.setContentView(view);
    dialog.show();
}

private void createPaymentSession(int invoiceId, String paymentMethod) {
    // Call POST /payments/checkout
    // (See Payment Endpoints section below)
}
```

---

## 💳 **PAYMENT ENDPOINTS**

### **1. Create Payment Session**

Membuat Midtrans Snap token untuk pembayaran invoice.

#### **Endpoint**

```
POST /api/v1/payments/checkout
```

#### **Headers**

```
Authorization: Bearer {access_token}
Content-Type: application/json
```

#### **Request Body**

```json
{
  "invoice_id": 123,
  "preferred_channel": "qris",
  "return_url": "inet://payment-result"
}
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `invoice_id` | integer | **Yes** | ID invoice yang akan dibayar |
| `preferred_channel` | string | No | Payment method preference (default: all methods) |
| `return_url` | string | No | Deep link untuk redirect setelah payment |

**Available Payment Channels:**

| Channel Code | Payment Method | Description |
|--------------|----------------|-------------|
| `qris` | QRIS | Scan QR dengan e-wallet apapun |
| `gopay` | GoPay | GoPay e-wallet |
| `shopeepay` | ShopeePay | ShopeePay e-wallet |
| `bank_transfer_bca` | BCA Virtual Account | Transfer via BCA |
| `bank_transfer_bni` | BNI Virtual Account | Transfer via BNI |
| `bank_transfer_bri` | BRI Virtual Account | Transfer via BRI |
| `bank_transfer_permata` | Permata Virtual Account | Transfer via Permata |
| `bank_transfer_mandiri` | Mandiri Bill Payment | Bayar via Mandiri |
| `credit_card` | Credit Card | Visa/Mastercard/JCB |

#### **Response Success (201 Created)**

```json
{
  "success": true,
  "message": "Payment session created successfully",
  "data": {
    "payment_id": 456,
    "order_id": "INV-20251115-000123",
    "snap_token": "abc123xyz-def456-ghi789",
    "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/abc123xyz",
    "gross_amount": 350000.00,
    "preferred_channel": "qris",
    "expires_at": "2025-11-16 14:30:00",
    "invoice": {
      "id": 123,
      "description": "Paket Premium - November 2025",
      "due_date": "2025-11-30"
    }
  }
}
```

**Response Fields:**

| Field | Type | Description |
|-------|------|-------------|
| `payment_id` | integer | ID payment record di database |
| `order_id` | string | Unique order identifier |
| `snap_token` | string | Midtrans Snap token (for SDK) |
| `redirect_url` | string | Midtrans payment page URL |
| `gross_amount` | decimal | Total payment amount |
| `expires_at` | datetime | Payment session expiry time |

#### **Response Error (400/401/403/422)**

```json
{
  "success": false,
  "message": "Invoice already paid",
  "error_code": "VALIDATION_ERROR",
  "errors": {
    "invoice_id": "Invoice already paid"
  }
}
```

#### **Example Usage (Java)**

```java
// Retrofit Interface
@POST("payments/checkout")
Call<PaymentResponse> createPayment(
    @Header("Authorization") String token,
    @Body CheckoutRequest request
);

// Request Model
public class CheckoutRequest {
    @SerializedName("invoice_id")
    private int invoiceId;

    @SerializedName("preferred_channel")
    private String preferredChannel;

    @SerializedName("return_url")
    private String returnUrl;

    // Constructor, getters, setters
}

// Usage
CheckoutRequest request = new CheckoutRequest(123, "qris", "inet://payment-result");

apiService.createPayment("Bearer " + accessToken, request)
    .enqueue(new Callback<PaymentResponse>() {
        @Override
        public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
            if (response.isSuccessful()) {
                PaymentData data = response.body().getData();
                String snapToken = data.getSnapToken();
                String redirectUrl = data.getRedirectUrl();

                // Open Midtrans payment page
                openMidtransPayment(redirectUrl);
            }
        }

        @Override
        public void onFailure(Call<PaymentResponse> call, Throwable t) {
            // Handle error
        }
    });
```

#### **Opening Midtrans Payment Page**

**Option 1: WebView (Recommended)**

```java
public void openMidtransPayment(String redirectUrl) {
    Intent intent = new Intent(this, PaymentWebViewActivity.class);
    intent.putExtra("url", redirectUrl);
    intent.putExtra("return_url", "inet://payment-result");
    startActivityForResult(intent, PAYMENT_REQUEST_CODE);
}

// PaymentWebViewActivity.java
WebView webView = findViewById(R.id.webview);
webView.setWebViewClient(new WebViewClient() {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        if (url.startsWith("inet://")) {
            // Deep link detected, payment finished
            handlePaymentResult(url);
            return true;
        }
        return false;
    }
});

webView.loadUrl(redirectUrl);
```

**Option 2: External Browser**

```java
Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(redirectUrl));
startActivity(browserIntent);
```

**Option 3: Midtrans SDK (Alternative)**

```java
// Using Midtrans Android SDK
UiKitApi.getDefaultInstance().startPaymentUiFlow(
    this,
    launcher,
    snapToken
);
```

---

### **2. Check Payment Status**

Mengecek status pembayaran setelah redirect dari Midtrans.

#### **Endpoint**

```
GET /api/v1/payments/status
```

#### **Headers**

```
Authorization: Bearer {access_token}
```

#### **Query Parameters**

**Option 1: By Invoice ID**

```
GET /api/v1/payments/status?invoice_id=123
```

**Option 2: By Payment ID**

```
GET /api/v1/payments/status?payment_id=456
```

**Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `invoice_id` | integer | Either this or `payment_id` | Invoice ID |
| `payment_id` | integer | Either this or `invoice_id` | Payment ID |

#### **Response Success - Paid (200 OK)**

```json
{
  "success": true,
  "message": "Invoice has been paid",
  "data": {
    "invoice_id": 123,
    "invoice_status": "paid",
    "invoice_amount": 350000.00,
    "invoice_description": "Paket Premium - November 2025",
    "due_date": "2025-11-30",
    "paid_at": "2025-11-15 14:35:00",
    "created_at": "2025-11-01 10:00:00",
    "payment": {
      "payment_id": 456,
      "order_id": "INV-20251115-000123",
      "status": "settlement",
      "payment_type": "qris",
      "preferred_channel": "qris",
      "transaction_id": "TXN-20251115-ABC123",
      "settlement_time": "2025-11-15 14:35:00",
      "expires_at": "2025-11-16 14:30:00",
      "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/...",
      "qr_string": "00020101021226670016COM.NOBUBANK.WWW..."
    }
  }
}
```

#### **Response Success - Pending (200 OK)**

```json
{
  "success": true,
  "message": "Payment is pending",
  "data": {
    "invoice_id": 123,
    "invoice_status": "issued",
    "invoice_amount": 350000.00,
    "paid_at": null,
    "payment": {
      "payment_id": 456,
      "order_id": "INV-20251115-000123",
      "status": "pending",
      "expires_at": "2025-11-16 14:30:00",
      "redirect_url": "https://app.sandbox.midtrans.com/snap/v2/vtweb/..."
    }
  }
}
```

#### **Payment Status Values**

| Status | Description | Action |
|--------|-------------|--------|
| `pending` | Payment belum selesai | Show "Waiting for payment" |
| `settlement` | Payment berhasil | Show success screen |
| `capture` | Credit card captured | Show success screen |
| `deny` | Payment ditolak bank/fraud | Show "Payment denied, try again" |
| `cancel` | Payment dibatalkan user | Show "Payment cancelled" |
| `expire` | Payment kadaluarsa | Show "Payment expired, create new" |

#### **Example Usage (Java)**

```java
// Retrofit Interface
@GET("payments/status")
Call<PaymentStatusResponse> getPaymentStatus(
    @Header("Authorization") String token,
    @Query("invoice_id") Integer invoiceId,
    @Query("payment_id") Integer paymentId
);

// Usage
apiService.getPaymentStatus("Bearer " + accessToken, invoiceId, null)
    .enqueue(new Callback<PaymentStatusResponse>() {
        @Override
        public void onResponse(Call<PaymentStatusResponse> call, Response<PaymentStatusResponse> response) {
            if (response.isSuccessful()) {
                PaymentStatusData data = response.body().getData();
                String invoiceStatus = data.getInvoiceStatus();
                PaymentDetail payment = data.getPayment();

                if ("paid".equals(invoiceStatus)) {
                    // Show success screen
                    showPaymentSuccess(data);
                } else if (payment != null && "pending".equals(payment.getStatus())) {
                    // Show pending/waiting screen
                    showPaymentPending(data);
                } else if (payment != null && "expire".equals(payment.getStatus())) {
                    // Show expired, ask to create new payment
                    showPaymentExpired();
                }
            }
        }

        @Override
        public void onFailure(Call<PaymentStatusResponse> call, Throwable t) {
            // Handle error
        }
    });
```

---

## ❌ **ERROR HANDLING**

### **Standard Error Response Format**

```json
{
  "success": false,
  "message": "Human-readable error message",
  "error_code": "ERROR_CODE",
  "errors": {
    "field_name": "Field-specific error message"
  }
}
```

### **HTTP Status Codes**

| Status Code | Meaning | Action |
|-------------|---------|--------|
| 200 | Success | Process response |
| 201 | Created | Process response |
| 400 | Bad Request | Show error message to user |
| 401 | Unauthorized | Redirect to login / refresh token |
| 403 | Forbidden | Show "Access denied" |
| 404 | Not Found | Show "Data not found" |
| 422 | Validation Error | Show field errors to user |
| 500 | Server Error | Show "Something went wrong, try again" |
| 501 | Not Implemented | Show "Feature coming soon" |

### **Common Error Codes**

| Error Code | Description | Solution |
|------------|-------------|----------|
| `VALIDATION_ERROR` | Request validation failed | Check request body/params |
| `UNAUTHORIZED` | Invalid/expired token | Login again or refresh token |
| `FORBIDDEN` | Insufficient permissions | User tidak punya akses |
| `NOT_FOUND` | Resource not found | Invoice/payment tidak ditemukan |
| `CUSTOMER_NOT_FOUND` | Customer data missing | Contact support |
| `MIDTRANS_ERROR` | Midtrans API error | Retry or contact support |
| `DATABASE_ERROR` | Database operation failed | Retry or contact support |

### **Error Handling Example (Java)**

```java
apiService.createPayment("Bearer " + accessToken, request)
    .enqueue(new Callback<PaymentResponse>() {
        @Override
        public void onResponse(Call<PaymentResponse> call, Response<PaymentResponse> response) {
            if (response.isSuccessful()) {
                // Success case
                handleSuccess(response.body());
            } else {
                // Error case
                try {
                    String errorBody = response.errorBody().string();
                    ErrorResponse error = gson.fromJson(errorBody, ErrorResponse.class);

                    switch (response.code()) {
                        case 401:
                            // Unauthorized - redirect to login
                            redirectToLogin();
                            break;
                        case 422:
                            // Validation error - show field errors
                            showValidationErrors(error.getErrors());
                            break;
                        case 500:
                            // Server error - show generic message
                            showError("Something went wrong. Please try again.");
                            break;
                        default:
                            // Other errors
                            showError(error.getMessage());
                    }
                } catch (Exception e) {
                    showError("An error occurred");
                }
            }
        }

        @Override
        public void onFailure(Call<PaymentResponse> call, Throwable t) {
            // Network error
            if (t instanceof UnknownHostException) {
                showError("No internet connection");
            } else if (t instanceof SocketTimeoutException) {
                showError("Request timeout. Please try again.");
            } else {
                showError("Network error: " + t.getMessage());
            }
        }
    });
```

---

## 🔄 **PAYMENT FLOW DIAGRAM**

### **Complete Flow**

```
┌─────────────────────────────────────────────────────────────┐
│                    MOBILE APP PAYMENT FLOW                   │
└─────────────────────────────────────────────────────────────┘

1. USER ACTION: Click "Bayar Invoice"
   │
   ├─> APP: Show payment method selector (optional)
   │
   └─> APP: POST /api/v1/payments/checkout
       Request: {
         "invoice_id": 123,
         "preferred_channel": "qris",
         "return_url": "inet://payment-result"
       }
       │
       ├─ SUCCESS (201)
       │  │
       │  └─> Response: {
       │        "snap_token": "abc123...",
       │        "redirect_url": "https://midtrans.com/...",
       │        "payment_id": 456
       │      }
       │      │
       │      └─> APP: Open WebView with redirect_url
       │          │
       │          └─> USER: Complete payment in Midtrans
       │              │
       │              ├─ Payment Success
       │              │  │
       │              │  └─> Midtrans: Redirect to return_url
       │              │      │
       │              │      └─> APP: Detect deep link "inet://payment-result"
       │              │          │
       │              │          └─> APP: Close WebView
       │              │              │
       │              │              └─> APP: GET /api/v1/payments/status?payment_id=456
       │              │                  │
       │              │                  ├─ Response: { "invoice_status": "paid" }
       │              │                  │  │
       │              │                  │  └─> APP: Show success screen ✓
       │              │                  │
       │              │                  └─ Response: { "status": "pending" }
       │              │                     │
       │              │                     └─> APP: Show pending screen (poll again)
       │              │
       │              └─ Payment Cancelled/Failed
       │                 │
       │                 └─> APP: Detect deep link or timeout
       │                     │
       │                     └─> APP: Check status → show appropriate message
       │
       └─ ERROR (401/422/500)
          │
          └─> APP: Show error message, allow retry

2. BACKGROUND: Midtrans Webhook
   │
   └─> Midtrans: POST /api/midtrans/notification.php
       │
       └─> Backend: Update invoice & payment status
           │
           └─> Database: invoice.status = 'paid'
                         payment.status = 'settlement'

3. APP REFRESH: User manually refresh
   │
   └─> APP: GET /api/v1/payments/status?invoice_id=123
       │
       └─> Response: Updated status from webhook
```

### **Recommended Polling Strategy**

```java
// After opening Midtrans WebView, start polling
ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

Runnable pollStatusTask = new Runnable() {
    int retryCount = 0;
    final int MAX_RETRIES = 12; // 12 retries = 2 minutes

    @Override
    public void run() {
        if (retryCount >= MAX_RETRIES) {
            executor.shutdown();
            showTimeout();
            return;
        }

        apiService.getPaymentStatus("Bearer " + accessToken, null, paymentId)
            .enqueue(new Callback<PaymentStatusResponse>() {
                @Override
                public void onResponse(Call<PaymentStatusResponse> call, Response<PaymentStatusResponse> response) {
                    if (response.isSuccessful()) {
                        String status = response.body().getData().getInvoiceStatus();
                        if ("paid".equals(status)) {
                            executor.shutdown();
                            showSuccess();
                        }
                    }
                    retryCount++;
                }

                @Override
                public void onFailure(Call<PaymentStatusResponse> call, Throwable t) {
                    retryCount++;
                }
            });
    }
};

// Poll every 10 seconds
executor.scheduleAtFixedRate(pollStatusTask, 0, 10, TimeUnit.SECONDS);
```

---

## 🧪 **TESTING GUIDE**

### **Test Credentials (Sandbox)**

```
Midtrans Sandbox Environment
Server Key: SB-Mid-server-xxxxx (dari backend team)
Client Key: SB-Mid-client-xxxxx (tidak perlu di mobile)
```

### **Test Payment Methods**

**QRIS:**
- Scan QR → akan langsung success di sandbox
- Tidak perlu bayar real

**Bank Transfer:**
- VA number akan di-generate
- Bisa langsung "Pay" di sandbox tanpa transfer real

**Credit Card:**
- Card Number: 4811 1111 1111 1114
- CVV: 123
- Expiry: Any future date
- OTP: 112233

### **Test Invoice**

```sql
-- Get test invoice from database
SELECT id, customer_id, amount, status, description
FROM invoices
WHERE status IN ('issued', 'overdue')
  AND customer_id = YOUR_CUSTOMER_ID
LIMIT 1;
```

### **Test Flow**

```
1. Login → Get access_token
2. Get invoice_id (dari dashboard atau API)
3. POST /checkout dengan invoice_id
4. Open redirect_url
5. Complete payment (auto-success di sandbox)
6. Check status → should be "paid"
```

---

## ⚙️ **ENVIRONMENT SETUP**

### **Development (Ngrok)**

```java
public static final String BASE_URL =
    "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/";
```

**Notes:**
- Ngrok URL berubah setiap restart (free tier)
- Update base URL di app jika ngrok restart
- Untuk testing, monitor ngrok dashboard: `http://localhost:4040`

### **Production (TBD)**

```java
public static final String BASE_URL = "https://api.wifinet.com/"; // Example
```

### **Network Configuration**

**Network Security Config** (`network_security_config.xml`):

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <!-- Allow ngrok for development -->
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">unthoroughly-arachidic-aaden.ngrok-free.dev</domain>
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </domain-config>

    <!-- Allow localhost for testing -->
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

**AndroidManifest.xml:**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    android:usesCleartextTraffic="false"
    ...>
```

### **Retrofit Setup**

```java
// OkHttpClient with interceptors
OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(new AuthInterceptor(accessToken))
    .addInterceptor(new LoggingInterceptor())
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build();

// Retrofit instance
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(ApiConfig.BASE_URL)
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build();

// API Service
ApiService apiService = retrofit.create(ApiService.class);
```

---

## 📞 **SUPPORT & CONTACT**

### **Backend Team Contact**

- **API Issues:** Report di group chat
- **Server Down:** Check ngrok status `http://localhost:4040`
- **Endpoint Changes:** Will be notified via group

### **Important URLs**

```
Ngrok Dashboard:    http://localhost:4040
Backend Base:       https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/
Supabase Dashboard: [URL dari backend team]
Midtrans Dashboard: https://dashboard.sandbox.midtrans.com
```

### **Documentation Files**

```
Mobile API Docs:        Doc/MobileAPIDocumentation.md (THIS FILE)
Testing Guide:          Doc/MobilePaymentTesting.md
Ngrok Setup:            Doc/NgrokSetup.md
Payment Integration:    Doc/PaymentIntegration.md
API Requirements:       Doc/APIRequirements.md
```

### **Troubleshooting**

**Issue: 401 Unauthorized**
- Check access token masih valid (expire 1 hour)
- Refresh token atau login ulang

**Issue: 404 Not Found**
- Check base URL sudah benar
- Check endpoint path (perhatikan trailing slash)

**Issue: CORS Error**
- Sudah handled di backend
- If still error, contact backend team

**Issue: Webhook Not Working**
- This is backend issue
- Payment status might delay update
- Use polling untuk check status

---

## 🎯 **CHECKLIST UNTUK MOBILE TEAM**

### **Before Development**

- [ ] Verify base URL accessible via browser
- [ ] Get test credentials (access_token, invoice_id)
- [ ] Setup network security config (Android)
- [ ] Configure Retrofit/OkHttp client

### **During Development**

- [ ] Implement authentication flow
- [ ] Implement checkout API call
- [ ] Implement WebView for Midtrans
- [ ] Implement deep link handling
- [ ] Implement status checking
- [ ] Implement error handling
- [ ] Add loading states
- [ ] Add retry mechanism

### **Testing**

- [ ] Test with valid invoice
- [ ] Test with invalid invoice (404)
- [ ] Test with paid invoice (422)
- [ ] Test payment success flow
- [ ] Test payment cancellation
- [ ] Test network timeout
- [ ] Test token expiry (401)

### **Ready for Integration**

- [ ] All error cases handled
- [ ] Loading states implemented
- [ ] Success/failure UI ready
- [ ] Deep link working
- [ ] Status polling working
- [ ] Tested end-to-end

---

## 📝 **CHANGELOG**

**v1.0 - 2025-11-15**
- Initial release
- Payment checkout endpoint
- Payment status endpoint
- Midtrans integration
- Webhook support

---

**Happy Coding! 🚀**

For questions or issues, contact backend team via group chat.
