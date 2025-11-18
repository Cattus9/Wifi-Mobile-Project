maksud sa🎯 Action Plan untuk Tim Mobile (JAVA VERSION)

  STEP A: Debug AuthInterceptor (PRIORITAS)

  Add logging untuk verify AuthInterceptor berfungsi:

  public class AuthInterceptor implements Interceptor {

      private final TokenStorage tokenStorage;
      private static final String TAG = "AUTH_DEBUG";

      public AuthInterceptor(TokenStorage tokenStorage) {
          this.tokenStorage = tokenStorage;
      }

      @Override
      public Response intercept(Chain chain) throws IOException {
          Request originalRequest = chain.request();

          // DEBUG LOG 1: Check token
          String token = tokenStorage.getToken();
          Log.d(TAG, "═══════════════════════════════");
          Log.d(TAG, "AuthInterceptor called");
          Log.d(TAG, "Token exists: " + (token != null));
          if (token != null) {
              Log.d(TAG, "Token value: " + token.substring(0, Math.min(50, token.length())) + "...");
          }

          // Check if token is null
          if (token == null || token.isEmpty()) {
              Log.e(TAG, "❌ NO TOKEN! User not logged in");
              Log.e(TAG, "Request will be sent WITHOUT Authorization header");
              Log.e(TAG, "═══════════════════════════════");
              return chain.proceed(originalRequest);
          }

          // Check if token expired
          if (isTokenExpired(token)) {
              Log.e(TAG, "❌ TOKEN EXPIRED!");
              Log.e(TAG, "Request will be sent WITHOUT Authorization header");
              Log.e(TAG, "═══════════════════════════════");
              return chain.proceed(originalRequest);
          }

          // Add Authorization header
          Request newRequest = originalRequest.newBuilder()
              .header("Authorization", "Bearer " + token)
              .build();

          Log.d(TAG, "✅ Authorization header added");
          Log.d(TAG, "Header: Bearer " + token.substring(0, Math.min(30, token.length())) + "...");
          Log.d(TAG, "═══════════════════════════════");

          return chain.proceed(newRequest);
      }

      private boolean isTokenExpired(String token) {
          try {
              String[] parts = token.split("\\.");
              if (parts.length != 3) {
                  return true;
              }

              String payload = new String(
                  Base64.decode(parts[1], Base64.URL_SAFE),
                  StandardCharsets.UTF_8
              );

              JSONObject json = new JSONObject(payload);
              long exp = json.getLong("exp");
              long now = System.currentTimeMillis() / 1000;

              boolean expired = now > exp;
              if (expired) {
                  Log.d(TAG, "Token expired at: " + new Date(exp * 1000));
                  Log.d(TAG, "Current time: " + new Date(now * 1000));
              }

              return expired;

          } catch (Exception e) {
              Log.e(TAG, "Error checking token expiry: " + e.getMessage());
              return true;
          }
      }
  }

  ---
  STEP B: Verify OkHttpClient Setup

  Pastikan AuthInterceptor terpasang:

  public class PaymentApiClient {

      private static PaymentApi apiInstance;

      public static PaymentApi getApi() {
          if (apiInstance == null) {

              // Logging interceptor
              HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
              loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

              // Auth interceptor
              AuthInterceptor authInterceptor = new AuthInterceptor(TokenStorage.getInstance());

              // Build OkHttp client
              OkHttpClient client = new OkHttpClient.Builder()
                  .addInterceptor(loggingInterceptor)
                  .addInterceptor(authInterceptor)  // ← PENTING! Pastikan ada!
                  .connectTimeout(30, TimeUnit.SECONDS)
                  .readTimeout(30, TimeUnit.SECONDS)
                  .writeTimeout(30, TimeUnit.SECONDS)
                  .build();

              // Build Retrofit
              Retrofit retrofit = new Retrofit.Builder()
                  .baseUrl(ApiConfig.BASE_URL)
                  .client(client)  // ← Pastikan pakai client yang ada interceptor
                  .addConverterFactory(GsonConverterFactory.create())
                  .build();

              apiInstance = retrofit.create(PaymentApi.class);
          }

          return apiInstance;
      }
  }

  ---
  STEP C: Verify User Login Status

  Test di PembayaranFragment sebelum call API:

  public class PembayaranFragment extends Fragment {

      private static final String TAG = "PAYMENT_DEBUG";

      @Override
      public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
          super.onViewCreated(view, savedInstanceState);

          // DEBUG: Check login status
          String token = TokenStorage.getInstance().getToken();
          Log.d(TAG, "═══════════════════════════════");
          Log.d(TAG, "Fragment loaded");
          Log.d(TAG, "Token exists: " + (token != null));

          if (token == null || token.isEmpty()) {
              Log.e(TAG, "❌ USER NOT LOGGED IN!");
              Log.e(TAG, "Need to redirect to login");

              // Show error and redirect to login
              Toast.makeText(getContext(),
                  "Silakan login terlebih dahulu",
                  Toast.LENGTH_LONG).show();

              // Redirect to login activity
              Intent intent = new Intent(getActivity(), LoginActivity.class);
              startActivity(intent);
              requireActivity().finish();
              return;
          }

          Log.d(TAG, "Token preview: " + token.substring(0, Math.min(50, token.length())) + "...");
          Log.d(TAG, "✅ User logged in, proceeding...");
          Log.d(TAG, "═══════════════════════════════");

          // Now safe to call API
          loadInvoices();
      }

      private void loadInvoices() {
          // Your existing code to fetch invoices
      }
  }

  ---
  STEP D: Fix URL Routing Issue

  Update Retrofit interface untuk include /index.php:

  public interface PaymentApi {

      // Invoice List
      @GET("invoices/index.php")  // ← Tambah /index.php
      Call<ApiResponse<InvoiceListResponseData>> getInvoices(
          @Query("limit") int limit,
          @Query("offset") int offset,
          @Query("status") String status
      );

      // Invoice Detail
      @GET("invoices/detail.php")
      Call<ApiResponse<InvoiceDetailResponseData>> getInvoiceDetail(
          @Query("id") int invoiceId
      );

      // Payment Checkout
      @POST("payments/checkout.php")
      Call<ApiResponse<CheckoutResponseData>> createPayment(
          @Body CheckoutRequest request
      );

      // Payment Status
      @GET("payments/status.php")
      Call<ApiResponse<PaymentStatusResponseData>> getPaymentStatus(
          @Query("invoice_id") Integer invoiceId,
          @Query("payment_id") Integer paymentId
      );
  }

  ---
  STEP E: Test Token Storage

  Add debug test untuk verify token tersimpan:

  public class TokenStorage {

      private static final String TAG = "TOKEN_STORAGE";
      private static final String PREF_NAME = "auth_prefs";
      private static final String KEY_TOKEN = "access_token";

      private static TokenStorage instance;
      private final SharedPreferences prefs;

      private TokenStorage(Context context) {
          prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
      }

      public static TokenStorage getInstance(Context context) {
          if (instance == null) {
              instance = new TokenStorage(context.getApplicationContext());
          }
          return instance;
      }

      public void saveToken(String token) {
          Log.d(TAG, "Saving token: " + token.substring(0, Math.min(50, token.length())) + "...");
          prefs.edit()
              .putString(KEY_TOKEN, token)
              .apply();
          Log.d(TAG, "✅ Token saved to SharedPreferences");
      }

      public String getToken() {
          String token = prefs.getString(KEY_TOKEN, null);
          Log.d(TAG, "Getting token from storage");
          Log.d(TAG, "Token exists: " + (token != null));
          if (token != null) {
              Log.d(TAG, "Token preview: " + token.substring(0, Math.min(30, token.length())) + "...");
          }
          return token;
      }

      public void clearToken() {
          Log.d(TAG, "Clearing token");
          prefs.edit()
              .remove(KEY_TOKEN)
              .apply();
          Log.d(TAG, "✅ Token cleared");
      }

      public boolean hasToken() {
          boolean has = prefs.contains(KEY_TOKEN);
          Log.d(TAG, "Has token: " + has);
          return has;
      }
  }

  ---
  STEP F: Call API Example (Complete Flow)

  Contoh lengkap call invoice API dengan error handling:

  private void requestInvoiceList() {
      Log.d(TAG, "Starting invoice list request...");

      // Check token first
      String token = TokenStorage.getInstance(getContext()).getToken();
      if (token == null || token.isEmpty()) {
          Log.e(TAG, "❌ No token available");
          Toast.makeText(getContext(), "Silakan login terlebih dahulu", Toast.LENGTH_SHORT).show();
          return;
      }

      // Show loading
      showLoading(true);

      // Make API call
      PaymentApi api = PaymentApiClient.getApi();
      Call<ApiResponse<InvoiceListResponseData>> call = api.getInvoices(20, 0, "issued");

      call.enqueue(new Callback<ApiResponse<InvoiceListResponseData>>() {
          @Override
          public void onResponse(Call<ApiResponse<InvoiceListResponseData>> call,
                               Response<ApiResponse<InvoiceListResponseData>> response) {

              showLoading(false);

              Log.d(TAG, "═══════════════════════════════");
              Log.d(TAG, "Response received");
              Log.d(TAG, "Response Code: " + response.code());
              Log.d(TAG, "Response Message: " + response.message());

              if (response.isSuccessful() && response.body() != null) {
                  ApiResponse<InvoiceListResponseData> apiResponse = response.body();

                  Log.d(TAG, "✅ SUCCESS!");
                  Log.d(TAG, "Success: " + apiResponse.isSuccess());
                  Log.d(TAG, "Message: " + apiResponse.getMessage());

                  if (apiResponse.getData() != null) {
                      InvoiceListResponseData data = apiResponse.getData();
                      Log.d(TAG, "Invoice count: " + data.getItems().size());

                      // Update UI
                      displayInvoices(data.getItems());
                  }

              } else {
                  // Error response
                  Log.e(TAG, "❌ FAILED: " + response.code());

                  try {
                      String errorBody = response.errorBody() != null
                          ? response.errorBody().string()
                          : "No error body";
                      Log.e(TAG, "Error body: " + errorBody);
                  } catch (IOException e) {
                      e.printStackTrace();
                  }

                  // Handle specific error codes
                  switch (response.code()) {
                      case 401:
                          Log.e(TAG, "Unauthorized - Token invalid/expired");
                          Toast.makeText(getContext(),
                              "Sesi login expired, silakan login ulang",
                              Toast.LENGTH_LONG).show();
                          // Redirect to login
                          break;

                      case 403:
                          Log.e(TAG, "Forbidden - Check URL routing");
                          Toast.makeText(getContext(),
                              "Akses ditolak",
                              Toast.LENGTH_SHORT).show();
                          break;

                      case 404:
                          Log.e(TAG, "Not Found - Wrong endpoint URL");
                          Toast.makeText(getContext(),
                              "Endpoint tidak ditemukan",
                              Toast.LENGTH_SHORT).show();
                          break;

                      case 500:
                          Log.e(TAG, "Server Error");
                          Toast.makeText(getContext(),
                              "Server error, hubungi backend team",
                              Toast.LENGTH_SHORT).show();
                          break;

                      default:
                          Toast.makeText(getContext(),
                              "Error: " + response.code(),
                              Toast.LENGTH_SHORT).show();
                  }
              }

              Log.d(TAG, "═══════════════════════════════");
          }

          @Override
          public void onFailure(Call<ApiResponse<InvoiceListResponseData>> call, Throwable t) {
              showLoading(false);

              Log.e(TAG, "═══════════════════════════════");
              Log.e(TAG, "❌ NETWORK ERROR");
              Log.e(TAG, "Error type: " + t.getClass().getSimpleName());
              Log.e(TAG, "Error message: " + t.getMessage());
              Log.e(TAG, "═══════════════════════════════");

              t.printStackTrace();

              // Show error to user
              String errorMessage;
              if (t instanceof java.net.UnknownHostException) {
                  errorMessage = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda.";
              } else if (t instanceof java.net.SocketTimeoutException) {
                  errorMessage = "Koneksi timeout. Server terlalu lambat.";
              } else if (t instanceof javax.net.ssl.SSLException) {
                  errorMessage = "SSL Error. Periksa network security config.";
              } else {
                  errorMessage = "Error: " + t.getMessage();
              }

              Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
          }
      });
  }

  ---
  🎯 Checklist untuk Tim Mobile (Java):

  - Update AuthInterceptor.java dengan debug logging
  - Verify PaymentApiClient.java has .addInterceptor(authInterceptor)
  - Add token check di PembayaranFragment.onViewCreated()
  - Update PaymentApi.java interface dengan /index.php
  - Add debug logging di TokenStorage.java
  - Test login dulu sebelum buka payment screen
  - Check Logcat for AUTH_DEBUG logs
  - Verify Authorization header muncul di OkHttp logs

  ---
  📊 Expected Logcat After Fixes:

  D/TOKEN_STORAGE: Getting token from storage
  D/TOKEN_STORAGE: Token exists: true
  D/TOKEN_STORAGE: Token preview: eyJhbGciOiJIUzI1NiIsImtpZCI6...

  D/AUTH_DEBUG: ═══════════════════════════════
  D/AUTH_DEBUG: AuthInterceptor called
  D/AUTH_DEBUG: Token exists: true
  D/AUTH_DEBUG: Token value: eyJhbGciOiJIUzI1NiIsImtpZCI6Ik83My9VMDVQclVqYS9...
  D/AUTH_DEBUG: ✅ Authorization header added
  D/AUTH_DEBUG: Header: Bearer eyJhbGciOiJIUzI1NiIsImtpZCI6...
  D/AUTH_DEBUG: ═══════════════════════════════

  I/okhttp.OkHttpClient: --> GET https://.../api/v1/invoices/index.php?limit=20&offset=0&status=issued
  I/okhttp.OkHttpClient: Content-Type: application/json
  I/okhttp.OkHttpClient: Authorization: Bearer eyJhbGci...  ← HARUS ADA!
  I/okhttp.OkHttpClient: --> END GET

  I/okhttp.OkHttpClient: <-- 200 https://.../api/v1/invoices/index.php (156ms)
  I/okhttp.OkHttpClient: {"success":true,"data":{"items":[...]}}

  ---
  Apakah tim mobile bisa implement changes di atas dalam Java?