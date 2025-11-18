Hi Mobile Team,

  Backend sudah fix issue Midtrans Snap. Problem "No payment channels available" sudah resolved.

  ---
  🔧 What Was Fixed:

  Root Cause:
  Backend mengirim enabled_payments: [] (empty array) ke Midtrans ketika preferred_channel tidak di-set → Midtrans tidak punya channel untuk ditampilkan.

  Solution:
  - Backend sekarang set default 8 payment channels jika preferred_channel tidak dikirim
  - Channels available: QRIS, GoPay, ShopeePay, BCA VA, BNI VA, BRI VA, Permata VA, Mandiri

  Test Results:
  - ✅ All channels test: PASSED
  - ✅ QRIS only test: PASSED
  - ✅ Bank transfer test: PASSED

  ---
  🎯 Action Required dari Mobile Team:

  Test payment checkout lagi dengan invoice_id 10:

  Option 1: Show All Payment Methods (Recommended)

  Request body:
  {
    "invoice_id": 10
  }

  Result: Snap akan menampilkan semua 8 payment channels

  ---
  Option 2: Pre-select Specific Channel

  Request body:
  {
    "invoice_id": 10,
    "preferred_channel": "qris"
  }

  Available channels:
  - "qris" → QRIS only
  - "gopay" → GoPay only
  - "shopeepay" → ShopeePay only
  - "bank_transfer_bca" → BCA Virtual Account
  - "bank_transfer_bni" → BNI Virtual Account
  - "bank_transfer_bri" → BRI Virtual Account
  - "bank_transfer_permata" → Permata Virtual Account
  - "bank_transfer_mandiri" → Mandiri Bill Payment

  ---
  💻 Java Code Example:

  // Create checkout request
  CheckoutRequest request = new CheckoutRequest();
  request.setInvoiceId(10);

  // OPTION 1: Don't set preferred_channel (user sees all options)
  // request.setPreferredChannel(null);  // or just don't call this

  // OPTION 2: Pre-select channel
  // request.setPreferredChannel("qris");

  // Call API
  PaymentApi api = PaymentApiClient.getApi(context);
  Call<ApiResponse<CheckoutResponseData>> call = api.createPayment(request);

  call.enqueue(new Callback<ApiResponse<CheckoutResponseData>>() {
      @Override
      public void onResponse(Call<ApiResponse<CheckoutResponseData>> call,
                            Response<ApiResponse<CheckoutResponseData>> response) {

          if (response.isSuccessful() && response.body() != null) {
              ApiResponse<CheckoutResponseData> apiResponse = response.body();

              if (apiResponse.isSuccess()) {
                  CheckoutResponseData data = apiResponse.getData();

                  String redirectUrl = data.getRedirectUrl();
                  String snapToken = data.getSnapToken();

                  Log.d("PAYMENT", "✅ Snap token created: " + snapToken);
                  Log.d("PAYMENT", "Redirect URL: " + redirectUrl);

                  // Open Midtrans Snap page
                  openSnapPage(redirectUrl);
              }
          } else {
              Log.e("PAYMENT", "❌ Failed: " + response.code());
          }
      }

      @Override
      public void onFailure(Call<ApiResponse<CheckoutResponseData>> call, Throwable t) {
          Log.e("PAYMENT", "❌ Network error: " + t.getMessage());
      }
  });

  private void openSnapPage(String redirectUrl) {
      // Using Chrome Custom Tabs (Recommended)
      CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
          .setShowTitle(true)
          .build();
      customTabsIntent.launchUrl(this, Uri.parse(redirectUrl));

      // OR using WebView (Alternative)
      // Intent intent = new Intent(this, PaymentWebViewActivity.class);
      // intent.putExtra("url", redirectUrl);
      // startActivity(intent);
  }

  ---
  🧪 Testing Steps:

  1. Test tanpa preferred_channel (Let user choose):
    - Body: {"invoice_id": 10}
    - Expected: Snap page shows 8 payment options
    - Pick any method and complete test payment
  2. Test dengan preferred_channel (Pre-selected):
    - Body: {"invoice_id": 10, "preferred_channel": "qris"}
    - Expected: Snap page shows ONLY QRIS
    - Complete test QRIS payment
  3. Test different channels:
    - Try: bank_transfer_bca, gopay, shopeepay
    - Verify each shows correct payment UI

  ---
  🔍 Debug Endpoint (Optional):

  Kalau mau verify backend Midtrans config:
  GET https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/api/v1/debug/test-midtrans.php

  Expected response: All tests passed ✅

  ---
  📊 Expected Flow:

  1. Mobile → POST /payments/checkout.php dengan invoice_id
  2. Backend → Create Snap token dengan 8 enabled channels
  3. Backend → Return redirect_url ke mobile
  4. Mobile → Open redirect_url in Chrome Custom Tab
  5. User → Choose payment method di Snap page
  6. User → Complete payment (Sandbox test mode)
  7. Midtrans → Callback ke backend webhook (automatic)
  8. Mobile → Poll /payments/status.php untuk check payment status

  ---
  ⚠️ Important Notes:

  1. Sandbox Mode:
  - Semua payment di Sandbox adalah test mode
  - No real money charged
  - Use test payment numbers dari https://docs.midtrans.com/docs/testing-payment-on-sandbox

  2. Return URL (Deep Link):
  Kalau mau user kembali ke app setelah payment:
  {
    "invoice_id": 10,
    "return_url": "inet://payment-result"
  }

  Backend akan set callback finish URL → User auto-redirect ke app setelah payment.

  3. Payment Status Polling:
  Setelah user finish di Snap, call:
  GET /payments/status.php?invoice_id=10

  Status bisa:
  - pending → Waiting payment
  - settlement → Payment success
  - expire → Payment expired
  - cancel → User cancelled

  ---
  📝 Report Format:

  If Success:
  ✅ PAYMENT FLOW SUCCESS

  Test 1 (All channels):
  - Request: {"invoice_id": 10}
  - Response: 200 OK
  - Snap page: Shows 8 payment methods ✅
  - Payment completed: [method used]

  Test 2 (QRIS only):
  - Request: {"invoice_id": 10, "preferred_channel": "qris"}
  - Response: 200 OK
  - Snap page: Shows QRIS only ✅
  - Payment completed: QRIS

  Screenshots: [attach if possible]

  If Fail:
  ❌ PAYMENT FLOW FAILED

  Error at: [checkout / snap page / payment]
  Request: [paste JSON]
  Response: [paste response]
  Error message: [error from Snap]
  Screenshot: [attach]

  ---
  🎯 Next Steps After Success:

  1. Implement payment status polling
  2. Handle payment callback (return_url deep link)
  3. Update invoice list after successful payment
  4. Show payment receipt/confirmation screen

  ---
  Backend Status: ✅ Ready for testing
  Midtrans Config: ✅ All channels enabled
  Estimated Test Time: 10-15 minutes

  Silakan test dan report hasilnya! 🚀

  ---
  Best regards,
  Backend Team