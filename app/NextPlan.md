# 📊 LAPORAN ANALISIS FUNDAMENTAL - ISP Mobile Application

**Tanggal Analisis**: 8 November 2025
**Project Location**: `C:\ProjekWebMobile\Wifi-Mobile-Project`
**Status Project**: Development Phase (60% Complete)
**Platform**: Android SDK 26-36 | Java 11
**Backend**: Supabase (PostgreSQL + Auth)

---

## RINGKASAN EKSEKUTIF

Aplikasi mobile ISP ini adalah aplikasi Android native berbasis Java yang dikembangkan untuk memberikan layanan **self-service** kepada pelanggan ISP (Internet Service Provider). Aplikasi ini mengintegrasikan **Supabase** sebagai backend dengan fitur autentikasi dan REST API untuk manajemen data pelanggan.

**Tech Stack**: Java 11, AndroidX, Material Design, OkHttp3, Lottie
**Production Readiness**: 60% Complete
**Estimated Time to Production**: 3-4 months (dengan 2 developers)

---

## 1. ARSITEKTUR APLIKASI

### 1.1 Struktur Layer

```
┌─────────────────────────────────────────────────────┐
│           UI Layer (Activities/Fragments)           │
│  ┌──────────┬──────────┬──────────┬──────────┬───┐ │
│  │ Beranda  │  Paket   │ Riwayat  │Pembayaran│Akun│ │
│  └──────────┴──────────┴──────────┴──────────┴───┘ │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│         Business Logic (Repository Pattern)         │
│  ┌─────────────────┐      ┌────────────────────┐   │
│  │ AuthRepository  │      │  Other Repositories │   │
│  └─────────────────┘      └────────────────────┘   │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│        Data Layer (Services & Storage)              │
│  ┌──────────────────┐     ┌──────────────────────┐ │
│  │SupabaseAuthSvc   │     │   TokenStorage       │ │
│  └──────────────────┘     └──────────────────────┘ │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│          External Services (Supabase)               │
│       Auth API  │  REST API  │  PostgreSQL          │
└─────────────────────────────────────────────────────┘
```

### 1.2 Package Structure

```
com/project/inet_mobile/
├── Activities (Root Level)
│   ├── SplashActivity.java      - Entry point dengan loading animation
│   ├── LoginActivity.java       - Authentication & session management
│   ├── RegisterActivity.java    - User registration form
│   └── DashboardActivity.java   - Main app container dengan bottom navigation
│
├── data/
│   ├── auth/
│   │   ├── AuthSession.java           - Token model (access, refresh, expiry)
│   │   ├── UserProfile.java           - User data model
│   │   ├── SignInResult.java          - Login result wrapper
│   │   ├── AuthException.java         - Custom exception handling
│   │   ├── AuthRepository.java        - Business logic orchestrator
│   │   └── SupabaseAuthService.java   - Low-level Supabase API calls
│   └── session/
│       └── TokenStorage.java          - Token persistence (SharedPreferences)
│
├── ui/
│   ├── home/
│   │   └── BerandaFragment.java       - Home/Dashboard fragment
│   ├── packages/
│   │   └── PaketFragment.java         - Service packages list
│   ├── payment/
│   │   └── PembayaranFragment.java    - Payment fragment (basic)
│   ├── history/
│   │   ├── RiwayatFragment.java       - Payment history & invoices
│   │   ├── RiwayatAdapter.java        - RecyclerView adapter
│   │   └── PaymentHistoryItem.java    - Payment history data model
│   ├── account/
│   │   └── AkunFragment.java          - User account profile & logout
│   └── cs/
│       ├── CsFragment.java            - Customer support main fragment
│       └── FormLaporanActivity.java   - Complaint/report form activity
│
├── util/
│   ├── MyApplication.java             - Application class
│   ├── conn.java                      - HTTP utilities & Supabase configs
│   └── DetailPaketFragment.java       - Package detail view
│
└── Models (Root Level)
    ├── Paket.java                      - Service package data model
    └── PaketAdapter.java               - RecyclerView adapter
```

### 1.3 Design Patterns Implementasi

| Pattern | Lokasi | Status | Catatan |
|---------|--------|--------|---------|
| **Repository** | `data/auth/AuthRepository.java` | ✅ Implemented | Untuk auth flow orchestration |
| **Service Layer** | `data/auth/SupabaseAuthService.java`, `util/conn.java` | ✅ Implemented | OkHttp3 HTTP client |
| **Adapter** | `PaketAdapter.java`, `RiwayatAdapter.java` | ✅ Implemented | RecyclerView adapters |
| **Fragment Navigation** | `DashboardActivity.java` | ✅ Implemented | Show/Hide pattern dengan back-stack |
| **Callback** | `AuthRepository.SignInCallback` | ✅ Implemented | Async operations |
| **ViewModel** | - | ❌ Not Implemented | **REKOMENDASI**: Perlu ditambahkan |
| **Dependency Injection** | - | ❌ Not Implemented | Manual instantiation |

---

## 2. KOMPONEN AUTENTIKASI & SESSION MANAGEMENT

### 2.1 Flow Autentikasi Login

```
[LoginActivity]
      │
      ├─> Input: email + password
      │
      ▼
[AuthRepository.signIn()] ← Orchestrator
      │
      ├─> [SupabaseAuthService.signIn()]
      │   POST /auth/v1/token?grant_type=password
      │   Response: {access_token, refresh_token, expires_in, user.id}
      │
      ├─> [SupabaseAuthService.fetchUserProfile(token)]
      │   GET /rest/v1/users?auth_user_id=eq.{id}
      │   Response: {id, email, customer_id, customers(*)}
      │
      ▼
[SignInResult] = {AuthSession + UserProfile}
      │
      ├─> [TokenStorage.saveSession()] → SharedPreferences
      │
      ├─> [LoginActivity.saveLoginSession()] → LoginPrefs
      │
      ▼
[Navigate to DashboardActivity]
```

### 2.2 Komponen Session Management

**File Locations:**
- `LoginActivity.java:28-215` - Login UI & session validation
- `data/auth/AuthRepository.java:1-47` - Auth orchestration
- `data/auth/SupabaseAuthService.java` - API calls
- `data/session/TokenStorage.java:1-66` - Token persistence

**Storage Mechanism:**

1. **TokenStorage** (SharedPreferences: `"SupabaseSession"`)
   ```
   - access_token (JWT)
   - refresh_token
   - expires_at (milliseconds)
   - token_type ("bearer")
   - auth_user_id (UUID)
   ```

2. **LoginPrefs** (SharedPreferences: `"LoginPrefs"`)
   ```
   - isLoggedIn (boolean)
   - userId (String)
   - userName (String)
   - userEmail (String)
   ```

### 2.3 Session Validation

**LoginActivity.java:57-60**
```java
if (isUserLoggedIn() && hasValidSession()) {
    navigateToDashboard(); // Auto-login
    return;
}
```

**Kriteria Valid Session:**
- `isLoggedIn` flag = true
- Token exists dalam TokenStorage
- Token belum expired (`session.isExpired()` = false)

### 2.4 ⚠️ SECURITY CONCERNS

| Issue | Severity | Impact | Recommendation |
|-------|----------|--------|----------------|
| **Plain-text Token Storage** | 🔴 CRITICAL | Token dapat dicuri dengan root access | Gunakan `EncryptedSharedPreferences` |
| **Hardcoded API Key** | 🔴 CRITICAL | Supabase anon key exposed di `conn.java` | Pindah ke `BuildConfig` atau server proxy |
| **No Token Refresh** | 🟠 HIGH | Session timeout tanpa auto-renewal | Implement refresh token mechanism |
| **No Token Revocation** | 🟡 MEDIUM | Logout hanya local, token masih valid di server | Consider server-side revocation |

**Action Items:**
```java
// IMMEDIATE FIX NEEDED:
1. TokenStorage.java:26 → Ganti dengan EncryptedSharedPreferences
2. conn.java → Move API key ke gradle.properties + BuildConfig
3. AuthRepository → Tambahkan refreshToken() method
```

---

## 3. DATABASE SCHEMA & MODEL

### 3.1 Supabase PostgreSQL Tables

**Core Tables:**

| Table | Purpose | Key Fields | Relationships |
|-------|---------|------------|---------------|
| `users` | User accounts | id, auth_user_id, customer_id, email, role | FK → auth.users, customers |
| `customers` | Customer profiles | id, name, phone, address, service_package_id, status | FK → service_packages |
| `service_packages` | Internet packages | id, name, description, speed, price, is_active | - |
| `invoices` | Billing records | id, customer_id, amount, due_date, status, paid_at | FK → customers |
| `tickets` | Support tickets | id, customer_id, subject, status, prioritas, kategori | FK → customers, users |

**Enums:**
- `user_role`: admin, customer
- `customer_status`: new, active, suspended, cancelled
- `invoice_status`: draft, issued, overdue, paid, cancelled
- `ticket_status`: open, in_progress, closed
- `prioritas_tiket`: rendah, normal, tinggi, mendesak
- `kategori_tiket`: koneksi, tagihan, instalasi, lainnya, perubahan_paket

**Reference**: Lihat `SchemaSupabase.md` untuk complete schema

### 3.2 Java Model Classes

**Implemented Models:**

```java
// Paket.java - Service Package
Properties: id, name, description, speed, price, isActive, duration,
            isPopuler, quota, phone, hargaAsli
Methods: getHarga() → "Rp XXX.XXX"

// UserProfile.java - User Data
Properties: userId, email, customerId, displayName

// AuthSession.java - Token Model
Properties: accessToken, refreshToken, expiresAtMillis, tokenType, authUserId
Methods: isExpired() → boolean

// PaymentHistoryItem.java - Invoice Display Model
Properties: monthLabel, paymentDate, method, invoiceNumber,
            amountFormatted, amountValue, status
Enum: InvoiceStatus (PAID, OVERDUE, DRAFT, UNKNOWN)
```

**⚠️ Missing Models:**
- Customer.java (untuk customer profile)
- Invoice.java (untuk billing data)
- Ticket.java (untuk support tickets)
- TicketMessage.java (untuk ticket messages)

---

## 4. FITUR-FITUR PELANGGAN

### 4.1 Implementasi Status

| Fragment | File Location | Status | Fitur | API Integration |
|----------|--------------|--------|-------|----------------|
| **Beranda** | `ui/home/BerandaFragment.java` | 🟡 Partial | Active package info, quota, expiry | ❌ Dummy data |
| **Paket** | `ui/packages/PaketFragment.java` | ✅ Complete | Package list, detail, pull-to-refresh | ✅ Supabase REST |
| **Riwayat** | `ui/history/RiwayatFragment.java` | 🟡 Partial | Payment history, invoice list | ❌ Dummy data |
| **Pembayaran** | `ui/payment/PembayaranFragment.java` | ❌ Not Started | Basic structure only | ❌ Not connected |
| **Akun** | `ui/account/AkunFragment.java` | ✅ Complete | Profile display, logout | ✅ SharedPreferences |

### 4.2 Detail Fitur per Fragment

#### 📱 **BerandaFragment** (Home)
```
✅ Implemented:
  - CardView dengan active package display
  - Quota progress bar (used/limit)
  - Expiry date & remaining days
  - Package details (speed + description)

❌ Missing:
  - API integration untuk fetch active package
  - Real-time quota updates
  - Package renewal CTA
```

#### 📦 **PaketFragment** (Packages)
```
✅ Implemented:
  - RecyclerView dengan gradient cards
  - Popular badge highlighting
  - Pull-to-refresh (SwipeRefreshLayout)
  - Loading animation (Lottie)
  - 10-second timeout dengan retry button
  - Error handling & display
  - Navigation ke DetailPaketFragment

✅ DetailPaketFragment:
  - Expandable Terms & Conditions
  - Expandable FAQ section
  - Benefits display (speed, duration, quota, phone)
  - Original price strikethrough (discount)
  - Bottom bar dengan "Beli Sekarang" button

❌ Missing:
  - Purchase flow (belum connect ke payment)
```

**Code Reference**: `ui/packages/PaketFragment.java`, `util/DetailPaketFragment.java`

#### 📜 **RiwayatFragment** (Payment History)
```
✅ Implemented:
  - Payment summary card (total paid, last month, count)
  - Outstanding amount (tunggakan)
  - Invoice list RecyclerView
  - Status badges (PAID/OVERDUE/DRAFT) dengan color coding
  - Empty state placeholder

❌ Missing:
  - API integration (masih dummy data)
  - Filter by date range
  - Download invoice PDF
```

**Code Reference**: `ui/history/RiwayatFragment.java`, `ui/history/RiwayatAdapter.java`

#### 💳 **PembayaranFragment** (Payment)
```
❌ NOT STARTED - basic structure only

Planned Features (from Plan.md):
  - Invoice list dengan due dates
  - Payment gateway integration (Midtrans/Xendit)
  - Payment proof upload
  - Payment status tracking
```

#### 👤 **AkunFragment** (Account)
```
✅ Implemented:
  - Profile display (name & email from SharedPreferences)
  - Logout button dengan confirmation dialog
  - Session clearing

❌ Missing:
  - Edit profile feature
  - Change password
  - Package management
  - Notification settings
```

**Code Reference**: `ui/account/AkunFragment.java`

### 4.3 DashboardActivity - Navigation Management

**File**: `DashboardActivity.java:1-208`

**Features:**
- Bottom Navigation dengan 5 fragments
- Fragment show/hide pattern (tidak recreate fragment)
- Navigation history management (LinkedList, max 5)
- Back button handling (popBackStack → navigation history → exit)
- State persistence (onSaveInstanceState)
- Edge-to-edge display dengan WindowInsets

**Navigation Items:**
```
R.id.navigation_beranda    → BerandaFragment
R.id.navigation_paket      → PaketFragment
R.id.navigation_riwayat    → RiwayatFragment
R.id.navigation_pembayaran → PembayaranFragment
R.id.navigation_akun       → AkunFragment
```

---

## 5. API INTEGRATION & NETWORKING

### 5.1 Supabase Configuration

**Base URL**: `https://rqmzvonjytyjdfhpqwvc.supabase.co`
**Anon Key**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (hardcoded di `util/conn.java`)

⚠️ **SECURITY RISK**: API key terbuka di source code!

### 5.2 API Endpoints Usage

| Endpoint | Method | Purpose | File Location | Status |
|----------|--------|---------|---------------|--------|
| `/auth/v1/token` | POST | Login dengan password grant | `SupabaseAuthService.java` | ✅ |
| `/rest/v1/users` | GET | Fetch user profile | `SupabaseAuthService.java` | ✅ |
| `/rest/v1/service_packages` | GET | Fetch all packages | `conn.java` | ✅ |
| `/rest/v1/customers` | GET | Fetch customer data | - | ❌ Not Implemented |
| `/rest/v1/invoices` | GET | Fetch invoices | - | ❌ Not Implemented |
| `/rest/v1/tickets` | GET/POST | Manage support tickets | - | ❌ Not Implemented |

### 5.3 HTTP Client Implementation

**Primary**: OkHttp3 4.12.0
```java
// SupabaseAuthService.java pattern:
Request request = new Request.Builder()
    .url(baseUrl + endpoint)
    .post(RequestBody.create(jsonBody, MediaType.JSON))
    .header("apikey", API_KEY)
    .header("Authorization", "Bearer " + token)
    .build();

Response response = httpClient.newCall(request).execute();
```

**Legacy**: AsyncTask (deprecated)
```java
// conn.java pattern:
new AsyncTask<Void, Void, JSONArray>() {
    @Override
    protected JSONArray doInBackground(Void... voids) {
        // HttpURLConnection + BufferedReader
    }
    @Override
    protected void onPostExecute(JSONArray result) {
        callback.onSuccess(result);
    }
}.execute();
```

**⚠️ Recommendations:**
- Migrate dari AsyncTask ke Kotlin Coroutines atau RxJava
- Implement Retrofit untuk type-safe API calls
- Add centralized error handling
- Implement retry mechanism dengan exponential backoff

### 5.4 Error Handling

**AuthException.java** - Custom exception dengan user-friendly messages:
```java
Mapping errors:
  "invalid_grant" / "invalid credentials" → "Email atau password salah"
  "email not confirmed"                  → "Email belum terverifikasi"
  Network errors                         → "Tidak dapat terhubung ke server"
```

**Network Resilience:**
- 10-second timeout di PaketFragment
- SwipeRefresh untuk manual retry
- Fallback ke anon key jika auth token invalid (conn.java)

---

## 6. TEKNOLOGI & DEPENDENCIES

### 6.1 Build Configuration

```gradle
Namespace: com.project.inet_mobile
Compile SDK: 36
Min SDK: 26 (Android 8.0 Oreo)
Target SDK: 36
Version: 1.0 (versionCode: 1)

Java: VERSION_11
View Binding: Enabled
Minify: Disabled (debug & release)
```

### 6.2 Core Dependencies

**AndroidX & UI:**
```
- androidx.appcompat:appcompat
- com.google.android.material:material:1.10.0
- androidx.constraintlayout:constraintlayout
- androidx.cardview:cardview:1.0.0
- androidx.recyclerview:recyclerview
- androidx.swiperefreshlayout:swiperefreshlayout
- androidx.viewpager2:viewpager2
```

**Navigation & Lifecycle:**
```
- androidx.navigation:navigation-fragment
- androidx.navigation:navigation-ui
- androidx.fragment:fragment
- androidx.fragment:fragment-ktx
- androidx.lifecycle:lifecycle-livedata-ktx
- androidx.lifecycle:lifecycle-viewmodel-ktx
```

**Networking & JSON:**
```
- com.squareup.okhttp3:okhttp:4.12.0
- com.google.code.gson:gson:2.10.1
```

**Animation:**
```
- com.airbnb.android:lottie:6.1.0
```

### 6.3 ⚠️ Missing Important Libraries

**Recommended Additions:**
- **Retrofit** (untuk type-safe API)
- **Dagger/Hilt** (untuk DI)
- **Room** (untuk local database/cache)
- **Timber** (untuk logging)
- **Coil/Glide** (untuk image loading)
- **WorkManager** (untuk background tasks)
- **EncryptedSharedPreferences** (untuk secure storage)

---

## 7. UI/UX DESIGN SYSTEM

### 7.1 Color Palette

```kotlin
Primary Colors:
  - register_primary: #30B6F0 (Cyan Blue)
  - register_primary_dark: #4CC4FF

Gradient:
  - start: #6A5BFF (Purple)
  - end: #86E3FF (Light Cyan)

Status Colors:
  - Paid: #1ABC9C (Teal)
  - Overdue: #E53935 (Red)
  - Draft: #FFB300 (Orange)

Text Colors:
  - Dark Primary: #0D0F25, #1B2559
  - White: #FFFFFF

Background:
  - Fragment: #f0f7fc (Light Cyan)
  - Card: #FFFFFF

Destructive:
  - Logout Red: #D32F2F
  - Logout Background: #FFE5E5
```

### 7.2 Typography & Styling

- **App Name**: "NSI"
- **Language**: Indonesian
- **Font**: Default Material Design (Roboto)
- **Design System**: Material Design 3

### 7.3 Animation

- **Splash Screen**: Lottie animation (3 seconds)
- **Loading States**: Lottie loading animation
- **Fragment Transitions**: TRANSIT_FRAGMENT_FADE
- **Pull-to-Refresh**: Material SwipeRefreshLayout

---

## 8. GIT HISTORY & DEVELOPMENT FLOW

**Current Branch**: `main`
**Recent Commits:**
```
a3d5ede - Merge branch 'Abi'
b75bab9 - Perbaiki Load Gagal Paket
158f468 - rework dashboard dan cs
efb81ac - perbaikan login dan detail paket
bb4f2d7 - Perbaiki Load Gagal Paket
```

**Development Branches**:
- `main` (primary branch)
- `Abi` (merged)

**Development Focus Areas** (dari commit history):
1. Authentication implementation
2. Package loading fixes (multiple iterations)
3. Dashboard restructuring
4. Customer support (CS) development
5. Login & detail paket improvements

---

## 9. CRITICAL ISSUES & RECOMMENDATIONS

### 9.1 🔴 CRITICAL SECURITY ISSUES

| # | Issue | Location | Impact | Fix Priority |
|---|-------|----------|--------|--------------|
| 1 | **Hardcoded Supabase Anon Key** | `util/conn.java` | Public key exposure | **IMMEDIATE** |
| 2 | **Plain-text Token Storage** | `data/session/TokenStorage.java:26` | Token theft vulnerability | **IMMEDIATE** |
| 3 | **No Token Refresh Mechanism** | AuthRepository | Session timeout, poor UX | **HIGH** |

**Recommended Fixes:**

```java
// FIX #1: Move API key ke BuildConfig
// gradle.properties:
SUPABASE_URL=https://rqmzvonjytyjdfhpqwvc.supabase.co
SUPABASE_ANON_KEY=eyJh...

// build.gradle.kts:
android {
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${properties["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${properties["SUPABASE_ANON_KEY"]}\"")
    }
}

// Usage in conn.java:
public static String getSupabaseUrl() {
    return BuildConfig.SUPABASE_URL;
}

// FIX #2: Encrypted Storage
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class TokenStorage {
    private final SharedPreferences preferences;

    public TokenStorage(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            this.preferences = EncryptedSharedPreferences.create(
                "SupabaseSession",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to regular SharedPreferences
            this.preferences = context.getSharedPreferences("SupabaseSession", Context.MODE_PRIVATE);
        }
    }
}

// FIX #3: Token Refresh
public void refreshToken(AuthSession currentSession, TokenRefreshCallback callback) {
    executorService.execute(() -> {
        try {
            // POST /auth/v1/token?grant_type=refresh_token
            String jsonBody = String.format("{\"refresh_token\":\"%s\"}",
                currentSession.getRefreshToken());

            Request request = new Request.Builder()
                .url(baseUrl + "/auth/v1/token?grant_type=refresh_token")
                .post(RequestBody.create(jsonBody, MediaType.JSON))
                .header("apikey", API_KEY)
                .build();

            Response response = httpClient.newCall(request).execute();
            // Parse response and create new AuthSession
            // ...
        } catch (Exception ex) {
            // Handle error
        }
    });
}
```

### 9.2 🟠 HIGH PRIORITY ISSUES

| # | Issue | Type | Recommendation |
|---|-------|------|----------------|
| 1 | AsyncTask Deprecated | Technical Debt | Migrate ke Coroutines/RxJava |
| 2 | No ViewModel Architecture | Architecture | Implement MVVM pattern |
| 3 | Manual JSON Parsing | Maintainability | Use Gson/Moshi dengan Retrofit |
| 4 | FormLaporanActivity tidak di Manifest | Bug | Register activity di AndroidManifest.xml |
| 5 | Dummy Data di Multiple Fragments | Incomplete Feature | Connect ke Supabase REST API |

### 9.3 🟡 MEDIUM PRIORITY IMPROVEMENTS

- **Logging**: Tambahkan Timber untuk structured logging
- **Error Tracking**: Integrate Firebase Crashlytics
- **Analytics**: Add Firebase Analytics atau Mixpanel
- **Offline Support**: Implement Room database untuk caching
- **Image Caching**: Add Coil atau Glide
- **Dependency Injection**: Implement Dagger/Hilt
- **Testing**: Write unit tests & UI tests

---

## 10. ROADMAP PENGEMBANGAN

### 10.1 Phase 1: Security Hardening (IMMEDIATE - 1 Week)

**Priority**: 🔴 CRITICAL
**Duration**: 1 week
**Team**: 1 developer

```
✅ Tasks:
1. Move Supabase API key ke BuildConfig
   - Update gradle.properties
   - Add buildConfigField di build.gradle.kts
   - Update conn.java untuk menggunakan BuildConfig
   - Add gradle.properties ke .gitignore

2. Implement EncryptedSharedPreferences
   - Add dependency androidx.security:security-crypto
   - Update TokenStorage.java untuk menggunakan EncryptedSharedPreferences
   - Add fallback mechanism jika encryption gagal
   - Test pada berbagai Android versions

3. Add ProGuard rules
   - Enable minifyEnabled untuk release build
   - Configure ProGuard rules untuk keep models
   - Test obfuscation tidak break app

4. Fix FormLaporanActivity manifest registration
   - Add <activity> entry di AndroidManifest.xml
   - Test navigation ke FormLaporanActivity

5. (Optional) Add SSL certificate pinning
   - Implement CertificatePinner untuk OkHttp
   - Add Supabase certificate pins

Deliverables:
  ✅ Secure token storage dengan encryption
  ✅ No exposed secrets in source code
  ✅ APK with proper obfuscation
  ✅ All activities registered
```

### 10.2 Phase 2: Architecture Improvement (2-3 Weeks)

**Priority**: 🔴 CRITICAL
**Duration**: 2-3 weeks
**Team**: 1-2 developers

```
✅ Tasks:
1. Implement ViewModel untuk semua fragments
   - Create ViewModels: BerandaViewModel, PaketViewModel, RiwayatViewModel, etc.
   - Move business logic dari Fragment ke ViewModel
   - Implement LiveData untuk reactive UI updates
   - Add ViewModelFactory jika perlu dependency injection

2. Migrate AsyncTask → Kotlin Coroutines (atau RxJava)
   - Add Kotlin support ke project jika belum ada
   - Add Coroutines dependency
   - Migrate conn.java AsyncTask ke Coroutines
   - Update semua callback-based code

3. Implement Retrofit + Gson untuk API calls
   - Add Retrofit + Converter Gson dependencies
   - Create SupabaseApiService interface
   - Create RetrofitClient singleton
   - Create data models untuk API responses
   - Migrate semua OkHttp manual calls ke Retrofit

4. Add Repository layer untuk semua entities
   - Create PackageRepository untuk service_packages
   - Create CustomerRepository untuk customers
   - Create InvoiceRepository untuk invoices
   - Create TicketRepository untuk tickets
   - Implement caching strategy

5. Add Room database untuk offline caching
   - Add Room dependencies
   - Create database entities
   - Create DAOs (Data Access Objects)
   - Implement database migrations
   - Add offline-first pattern

Deliverables:
  ✅ Clean MVVM architecture
  ✅ Type-safe API calls dengan Retrofit
  ✅ Offline-first approach dengan Room
  ✅ Better state management dengan ViewModel + LiveData
  ✅ No deprecated code (AsyncTask removed)
```

### 10.3 Phase 3: Feature Completion (3-4 Weeks)

**Priority**: 🟠 HIGH
**Duration**: 3-4 weeks
**Team**: 2 developers

```
✅ Week 1-2: BerandaFragment & Data Integration

1. BerandaFragment API Integration
   - Create endpoint untuk fetch active customer package
   - Implement CustomerRepository.getActivePackage()
   - Create BerandaViewModel untuk state management
   - Connect UI ke ViewModel dengan LiveData
   - Add real-time quota updates (jika tersedia)
   - Implement package renewal CTA

2. RiwayatFragment API Integration
   - Implement InvoiceRepository.getInvoices(customerId)
   - Create payment summary calculation logic
   - Replace dummy data dengan API calls
   - Add date range filter
   - Implement invoice PDF download (jika tersedia)
   - Add pull-to-refresh untuk refresh data


✅ Week 2-3: PembayaranFragment Implementation

1. Payment Gateway Integration
   - Research & pilih payment gateway (Midtrans/Xendit)
   - Add payment gateway SDK dependencies
   - Implement payment gateway initialization
   - Create payment request flow
   - Handle payment callbacks
   - Update invoice status after payment

2. Payment UI Development
   - Create invoice list layout dengan due dates
   - Implement "Bayar Sekarang" button
   - Add payment method selection
   - Create payment proof upload UI
   - Add payment status tracking
   - Implement payment confirmation dialog


✅ Week 3-4: Customer Support (CS) Implementation

1. Ticket List & Creation
   - Implement TicketRepository.getTickets(customerId)
   - Create ticket list UI dengan RecyclerView
   - Implement filter by status/category
   - Create "Buat Tiket Baru" form
   - Add ticket category & priority selection
   - Implement ticket submission

2. Ticket Detail & Messaging
   - Create ticket detail view
   - Implement ticket messages list
   - Add message composer
   - Implement file attachment upload
   - Add real-time status updates (jika menggunakan realtime)
   - Implement ticket closing

Deliverables:
  ✅ Fully functional home dashboard dengan real data
  ✅ Complete payment system dengan gateway integration
  ✅ Working customer support dengan ticket management
  ✅ All fragments connected ke API
  ✅ No dummy data
```

### 10.4 Phase 4: Advanced Features (4-6 Weeks)

**Priority**: 🟡 MEDIUM
**Duration**: 4-6 weeks
**Team**: 2 developers

```
✅ Week 1-2: Push Notifications

1. Firebase Cloud Messaging Setup
   - Add Firebase project & google-services.json
   - Add FCM dependencies
   - Implement FirebaseMessagingService
   - Store FCM token di Supabase users table
   - Create notification handler

2. Notification Types Implementation
   - Invoice due date reminders (3 days, 1 day before)
   - Invoice overdue notifications
   - Ticket status updates (replied, closed)
   - New package announcements
   - Service disruption alerts

3. Backend Integration
   - Create Supabase Edge Functions untuk send notifications
   - Setup database triggers untuk auto-notification
   - Implement notification scheduling


✅ Week 3-4: User Experience Enhancements

1. Dark Mode Support
   - Create dark theme colors.xml
   - Update all layouts dengan theme-aware colors
   - Add theme switcher di Settings
   - Save theme preference
   - Test semua screens dalam dark mode

2. UI Improvements
   - Add app shortcuts untuk quick actions
   - Improve splash screen dengan SplashScreen API
   - Add skeleton loading screens
   - Implement shimmer effect saat loading
   - Add pull-to-refresh ke semua applicable screens
   - Add empty states dengan illustrations
   - Improve error states dengan retry buttons

3. Onboarding & First-time Experience
   - Create onboarding screens untuk new users
   - Add feature highlights
   - Implement skip/next navigation
   - Add "Don't show again" preference


✅ Week 5-6: Analytics & Monitoring

1. Firebase Analytics Integration
   - Add Firebase Analytics dependency
   - Implement screen tracking
   - Add event tracking untuk key actions:
     - Login/Logout
     - Package view/purchase
     - Payment initiated/completed
     - Ticket created/closed
   - Create custom user properties

2. Crashlytics Integration
   - Add Firebase Crashlytics dependency
   - Setup crash reporting
   - Add custom crash keys
   - Test crash reporting

3. Performance Monitoring
   - Add Firebase Performance Monitoring
   - Add custom traces untuk API calls
   - Monitor screen rendering time
   - Track app startup time

4. User Behavior Analytics
   - Track user journeys
   - Measure feature adoption
   - Track conversion funnels
   - Create analytics dashboard

Deliverables:
  ✅ Push notifications working
  ✅ Enhanced UX dengan dark mode & loading states
  ✅ Complete analytics & monitoring
  ✅ Crashlytics for error tracking
```

### 10.5 Phase 5: Testing & Release Preparation (2 Weeks)

**Priority**: 🟠 HIGH
**Duration**: 2 weeks
**Team**: 1 developer + 1 QA

```
✅ Week 1: Testing

1. Unit Testing (Target 70% coverage)
   - Write unit tests untuk ViewModels
   - Write unit tests untuk Repositories
   - Write unit tests untuk utility classes
   - Write unit tests untuk data models
   - Run code coverage report

2. Integration Testing
   - Test API integration dengan MockWebServer
   - Test database operations
   - Test navigation flows
   - Test authentication flows

3. UI Testing dengan Espresso
   - Test login flow
   - Test navigation
   - Test fragment transitions
   - Test form validations
   - Test error states

4. Manual QA Testing
   - Create test cases document
   - Test semua user flows
   - Test edge cases
   - Test on multiple devices/Android versions
   - Document bugs & issues


✅ Week 2: Release Preparation

1. App Signing Configuration
   - Generate release keystore
   - Configure signing in build.gradle
   - Store keystore securely
   - Document signing process

2. Build Optimization
   - Enable ProGuard/R8 untuk release
   - Optimize APK size
   - Generate App Bundle (.aab)
   - Test release build thoroughly

3. Play Store Preparation
   - Create Play Console account
   - Prepare app screenshots (phone & tablet)
   - Write app description (Indonesian & English)
   - Create feature graphic
   - Prepare promotional video (optional)
   - Fill content rating questionnaire
   - Set pricing & distribution

4. Legal & Compliance
   - Create Privacy Policy
   - Create Terms of Service
   - Add in-app policy links
   - Ensure GDPR compliance (jika applicable)

5. Release Notes & Documentation
   - Write release notes
   - Update README.md
   - Create user guide/FAQ
   - Document known issues

6. Beta Release
   - Create beta testing group
   - Upload beta build ke Play Console
   - Distribute to beta testers
   - Collect feedback
   - Fix critical issues

Deliverables:
  ✅ Comprehensive test suite dengan 70%+ coverage
  ✅ Production-ready signed APK/AAB
  ✅ Complete Play Store listing
  ✅ Privacy policy & terms
  ✅ Beta testing completed
  ✅ Ready for production release
```

---

## 11. ESTIMASI DEVELOPMENT EFFORT

### 11.1 Timeline & Resources

| Phase | Duration | Developer Count | Priority | Cost Estimate* |
|-------|----------|-----------------|----------|----------------|
| **Phase 1: Security** | 1 week | 1 developer | 🔴 CRITICAL | Rp 5-8 juta |
| **Phase 2: Architecture** | 2-3 weeks | 1-2 developers | 🔴 CRITICAL | Rp 15-25 juta |
| **Phase 3: Features** | 3-4 weeks | 2 developers | 🟠 HIGH | Rp 30-50 juta |
| **Phase 4: Advanced** | 4-6 weeks | 2 developers | 🟡 MEDIUM | Rp 40-60 juta |
| **Phase 5: Testing** | 2 weeks | 1 dev + 1 QA | 🟠 HIGH | Rp 10-15 juta |
| **TOTAL** | **12-16 weeks** | **2 developers** | - | **Rp 100-158 juta** |

*Estimasi biaya berdasarkan rate developer Android: Rp 5-10 juta/minggu

### 11.2 Team Composition

**Recommended Team:**
```
1x Senior Android Developer (Lead)
  - Architecture decisions
  - Security implementation
  - Code review
  - Technical leadership

1x Mid-level Android Developer
  - Feature implementation
  - UI development
  - API integration
  - Bug fixes

1x QA Engineer (Part-time, Phase 5)
  - Test case creation
  - Manual testing
  - Bug reporting
  - Release validation

1x UI/UX Designer (Consultant, as needed)
  - UI improvements
  - Asset creation
  - User flow optimization
```

### 11.3 Dependency & Risk Matrix

| Dependency | Impact | Risk | Mitigation |
|------------|--------|------|------------|
| Supabase API availability | HIGH | LOW | Add retry logic & offline mode |
| Payment Gateway approval | HIGH | MEDIUM | Start integration early, have backup gateway |
| FCM token delivery | MEDIUM | LOW | Implement retry & queue mechanism |
| Play Store approval | HIGH | MEDIUM | Follow guidelines strictly, prepare alternatives |
| Third-party SDK updates | MEDIUM | MEDIUM | Lock dependency versions, test updates |

---

## 12. PRODUCTION READINESS CHECKLIST

### 12.1 Current Status: 60% Ready

```
Legend:
✅ Ready    🟡 Partial    ❌ Not Ready

CORE FEATURES:
  ✅ Authentication & Login
  ✅ Session Management
  ✅ Package Listing & Detail
  🟡 Home Dashboard (needs API)
  🟡 Payment History (needs API)
  ❌ Payment Processing
  ❌ Customer Support
  ✅ User Profile & Logout

TECHNICAL REQUIREMENTS:
  ❌ Security (CRITICAL issues present)
  🟡 Architecture (needs ViewModel)
  ✅ UI/UX Design
  ❌ Testing (no tests written)
  ❌ Error Monitoring
  ❌ Analytics
  ❌ Offline Support
  ✅ Localization (Indonesian)

CODE QUALITY:
  🟡 Code organization (good package structure)
  ❌ Code documentation (minimal comments)
  ❌ Unit tests (0% coverage)
  ❌ Integration tests (none)
  ❌ UI tests (none)
  🟡 Error handling (partial)

PERFORMANCE:
  ✅ App startup time (acceptable)
  🟡 API response caching (none)
  ✅ Image loading (minimal images used)
  🟡 Memory usage (not optimized)
  ❌ Battery usage (not measured)

SECURITY:
  ❌ API key protection (hardcoded)
  ❌ Token encryption (plain text)
  ❌ SSL pinning (not implemented)
  ✅ HTTPS only (Supabase uses HTTPS)
  ❌ Code obfuscation (disabled)

COMPLIANCE:
  ❌ Privacy Policy (not created)
  ❌ Terms of Service (not created)
  ❌ GDPR compliance (not assessed)
  ❌ Data retention policy (not defined)
```

### 12.2 Pre-Production Checklist

**Before submitting to Play Store:**

```
SECURITY:
  □ API keys moved to BuildConfig
  □ Tokens encrypted dengan EncryptedSharedPreferences
  □ ProGuard/R8 enabled dan tested
  □ SSL certificate pinning implemented (optional)
  □ No sensitive data logged
  □ All API calls use HTTPS

CODE QUALITY:
  □ All TODOs resolved or documented
  □ No debug/test code in release
  □ Code review completed
  □ All compiler warnings addressed
  □ Lint checks passed

FUNCTIONALITY:
  □ All features working as expected
  □ No dummy/mock data in production
  □ All user flows tested
  □ Error handling implemented everywhere
  □ Offline mode working (if applicable)

TESTING:
  □ Unit tests written (70%+ coverage)
  □ Integration tests passed
  □ UI tests passed
  □ Manual testing completed
  □ Beta testing feedback addressed
  □ Performance testing done
  □ Security testing done

ASSETS & RESOURCES:
  □ App icon finalized (all densities)
  □ Splash screen optimized
  □ All images optimized
  □ All strings translated
  □ All layouts tested on multiple screen sizes

BUILD & RELEASE:
  □ Release build signed with production keystore
  □ Version code & version name updated
  □ App bundle (.aab) generated
  □ APK size optimized (<50MB recommended)
  □ Build variants configured correctly

PLAY STORE:
  □ App screenshots prepared (8 screens minimum)
  □ Feature graphic created (1024x500)
  □ App description written (ID & EN)
  □ Privacy policy published & linked
  □ Terms of service published & linked
  □ Content rating completed
  □ Target audience defined
  □ Pricing & distribution set

BACKEND:
  □ Supabase production setup completed
  □ Database backups configured
  □ API rate limits configured
  □ Monitoring & alerts set up
  □ Error logging configured

LEGAL & COMPLIANCE:
  □ Privacy policy reviewed by legal
  □ Terms of service reviewed by legal
  □ Data handling documented
  □ User consent flows implemented
  □ Data deletion mechanism implemented
```

---

## 13. MONITORING & MAINTENANCE PLAN

### 13.1 Post-Launch Monitoring

**Week 1-2 after launch (Critical Period):**
```
Daily Monitoring:
  - Crashlytics dashboard (crash-free rate target: >99%)
  - Firebase Analytics (DAU, MAU, retention)
  - API error rates
  - App store reviews & ratings
  - User support tickets

Weekly Actions:
  - Review top crashes
  - Analyze user behavior patterns
  - Monitor payment gateway success rates
  - Check notification delivery rates
  - Review API performance
```

**Month 1-3 (Stabilization):**
```
Weekly Monitoring:
  - Performance metrics
  - User engagement metrics
  - Feature adoption rates
  - Churn rate

Bi-weekly Actions:
  - Release bug fix updates
  - Respond to user reviews
  - Optimize based on analytics
  - Plan new features based on feedback
```

### 13.2 Maintenance Schedule

**Regular Updates:**
```
Monthly (Security & Bug Fixes):
  - Security patches
  - Critical bug fixes
  - Dependency updates
  - Performance optimizations

Quarterly (Feature Updates):
  - New features based on roadmap
  - UI/UX improvements
  - Android version updates
  - Library major version updates

Yearly (Major Updates):
  - Major redesign (if needed)
  - Architecture improvements
  - Technology stack updates
  - Compliance updates
```

### 13.3 Key Metrics to Track

**Business Metrics:**
```
- Monthly Active Users (MAU)
- Daily Active Users (DAU)
- User retention rate (Day 1, Day 7, Day 30)
- Churn rate
- Payment conversion rate
- Customer support ticket volume
- Average ticket resolution time
- Customer satisfaction score (CSAT)
```

**Technical Metrics:**
```
- Crash-free rate (target: >99%)
- ANR (Application Not Responding) rate (target: <0.5%)
- App startup time (target: <2s)
- API response time (target: <1s)
- Payment success rate (target: >95%)
- Push notification delivery rate (target: >90%)
- App size (target: <50MB)
```

**Engagement Metrics:**
```
- Session length
- Screen views per session
- Feature usage rate
- Login frequency
- Payment frequency
- Support ticket creation rate
```

---

## 14. KESIMPULAN & NEXT STEPS

### 14.1 Kondisi Saat Ini

**Strengths (Kekuatan):**
- ✅ Core authentication sudah solid dengan Supabase
- ✅ UI/UX design modern dengan Material Design
- ✅ Package listing sudah fully functional dengan pull-to-refresh
- ✅ Repository pattern sudah diimplementasikan (partial)
- ✅ Navigation flow sudah baik dengan back-stack management
- ✅ Database schema sudah comprehensive dan well-designed
- ✅ Error handling dengan user-friendly messages
- ✅ Loading states dengan Lottie animations

**Weaknesses (Kelemahan):**
- ❌ **CRITICAL**: Security vulnerabilities (exposed API key, plain-text tokens)
- ❌ **CRITICAL**: No token refresh mechanism
- ❌ Incomplete features (Pembayaran, CS belum jalan)
- ❌ No ViewModel architecture (state management kurang optimal)
- ❌ AsyncTask deprecated (technical debt)
- ❌ Banyak dummy data (belum connect ke API)
- ❌ No offline support
- ❌ No testing (0% coverage)
- ❌ FormLaporanActivity tidak registered di manifest

**Opportunities (Peluang):**
- 🎯 Payment gateway integration dapat menjadi revenue stream
- 🎯 Customer support automation dapat reduce operational cost
- 🎯 Push notifications dapat improve engagement
- 🎯 Analytics dapat provide insights untuk business decisions
- 🎯 Offline mode dapat improve user experience di area dengan koneksi buruk

**Threats (Ancaman):**
- ⚠️ Security issues dapat lead to data breach
- ⚠️ Deprecated code dapat break di Android versions mendatang
- ⚠️ Competitor apps dengan better UX
- ⚠️ Play Store rejection karena policy violations
- ⚠️ User churn karena bugs atau missing features

### 14.2 Production Readiness Assessment

**Current**: 60% Ready for Production
**Target**: 100% Ready for Production
**Gap**: 40%

**Breakdown:**
```
Security:        20% → Target: 100% (Gap: 80%)
Architecture:    50% → Target: 100% (Gap: 50%)
Features:        70% → Target: 100% (Gap: 30%)
Testing:          0% → Target: 100% (Gap: 100%)
Documentation:   40% → Target: 100% (Gap: 60%)
```

**Estimated Time to Production-Ready**: 12-16 weeks

### 14.3 Immediate Next Steps (Week 1)

**Day 1-2: Security Audit & Planning**
```
1. Review semua security vulnerabilities
2. Create detailed security fix plan
3. Setup development environment untuk secure development
4. Prepare gradle.properties template
```

**Day 3-4: Security Implementation**
```
1. Move API keys to BuildConfig
2. Implement EncryptedSharedPreferences
3. Add ProGuard configuration
4. Test security fixes thoroughly
```

**Day 5: Code Review & Documentation**
```
1. Code review untuk security changes
2. Update documentation
3. Create migration guide untuk team
4. Prepare for Phase 2 (Architecture)
```

### 14.4 Success Criteria

**Phase 1 (Security) Success:**
- ✅ No hardcoded secrets in source code
- ✅ All tokens encrypted at rest
- ✅ ProGuard enabled dan tested
- ✅ Security audit passed

**Phase 2 (Architecture) Success:**
- ✅ All fragments using ViewModel
- ✅ All API calls using Retrofit
- ✅ Room database implemented
- ✅ No deprecated code
- ✅ LiveData untuk reactive UI

**Phase 3 (Features) Success:**
- ✅ All fragments connect to real API
- ✅ Payment gateway working
- ✅ Customer support fully functional
- ✅ No dummy data

**Phase 4 (Advanced) Success:**
- ✅ Push notifications working
- ✅ Dark mode implemented
- ✅ Analytics tracking all key events
- ✅ Crashlytics monitoring all crashes

**Phase 5 (Release) Success:**
- ✅ 70%+ test coverage
- ✅ Play Store listing approved
- ✅ Beta testing completed with positive feedback
- ✅ Production release successful

### 14.5 Long-term Vision

**6 Months Post-Launch:**
```
- 10,000+ active users
- 4.5+ star rating on Play Store
- <1% crash rate
- 50%+ monthly retention rate
- Payment gateway processing 1000+ transactions/month
- Customer support handling 500+ tickets/month
```

**1 Year Post-Launch:**
```
- 50,000+ active users
- Feature parity with competitor apps
- Premium features implementation
- Multi-language support
- iOS version development started
- Web dashboard integration
```

---

## 15. REFERENSI FILE PENTING

### 15.1 Core Files untuk Development

```
Authentication & Session:
  📄 app/src/main/java/com/project/inet_mobile/LoginActivity.java:28-215
  📄 app/src/main/java/com/project/inet_mobile/data/auth/AuthRepository.java:1-47
  📄 app/src/main/java/com/project/inet_mobile/data/auth/SupabaseAuthService.java
  📄 app/src/main/java/com/project/inet_mobile/data/session/TokenStorage.java:1-66

Navigation & UI:
  📄 app/src/main/java/com/project/inet_mobile/DashboardActivity.java:1-208
  📄 app/src/main/java/com/project/inet_mobile/SplashActivity.java

Fragments:
  📄 app/src/main/java/com/project/inet_mobile/ui/home/BerandaFragment.java
  📄 app/src/main/java/com/project/inet_mobile/ui/packages/PaketFragment.java
  📄 app/src/main/java/com/project/inet_mobile/ui/history/RiwayatFragment.java
  📄 app/src/main/java/com/project/inet_mobile/ui/payment/PembayaranFragment.java
  📄 app/src/main/java/com/project/inet_mobile/ui/account/AkunFragment.java
  📄 app/src/main/java/com/project/inet_mobile/ui/cs/CsFragment.java

Models & Adapters:
  📄 app/src/main/java/com/project/inet_mobile/Paket.java
  📄 app/src/main/java/com/project/inet_mobile/PaketAdapter.java
  📄 app/src/main/java/com/project/inet_mobile/ui/history/RiwayatAdapter.java
  📄 app/src/main/java/com/project/inet_mobile/ui/history/PaymentHistoryItem.java

Utilities:
  📄 app/src/main/java/com/project/inet_mobile/util/conn.java
  📄 app/src/main/java/com/project/inet_mobile/util/DetailPaketFragment.java
  📄 app/src/main/java/com/project/inet_mobile/util/MyApplication.java

Configuration:
  📄 app/build.gradle.kts:1-76
  📄 app/src/main/AndroidManifest.xml
  📄 app/SchemaSupabase.md:1-192
  📄 app/Plan.md:1-36
  📄 app/ContinuePlan.md
```

### 15.2 Documentation Files

```
Project Documentation:
  📄 app/SchemaSupabase.md - Database schema complete
  📄 app/Plan.md - Original development plan
  📄 app/ContinuePlan.md - Continuation notes
  📄 app/NextPlan.md - This comprehensive analysis (NEW)

Git:
  📄 .gitignore
  📄 README.md (if exists)
```

### 15.3 Configuration Files

```
Build Configuration:
  📄 build.gradle.kts (root)
  📄 app/build.gradle.kts
  📄 gradle.properties
  📄 settings.gradle.kts

Android Resources:
  📄 app/src/main/res/values/colors.xml
  📄 app/src/main/res/values/strings.xml
  📄 app/src/main/res/values/styles.xml
  📄 app/src/main/res/values/themes.xml
```

---

## 16. GLOSSARY & TERMINOLOGY

**Technical Terms:**
```
- MVVM: Model-View-ViewModel architecture pattern
- Repository Pattern: Design pattern untuk abstract data sources
- LiveData: Android lifecycle-aware observable data holder
- ViewModel: UI-related data holder that survives configuration changes
- Retrofit: Type-safe HTTP client untuk Android
- Room: SQLite abstraction library
- Coroutines: Kotlin feature untuk asynchronous programming
- ProGuard/R8: Code shrinker dan obfuscator
- FCM: Firebase Cloud Messaging
```

**Business Terms:**
```
- ISP: Internet Service Provider
- MAU: Monthly Active Users
- DAU: Daily Active Users
- CSAT: Customer Satisfaction Score
- Churn Rate: Rate of customer attrition
- Retention Rate: Percentage of users who return
- Conversion Rate: Percentage of users who complete desired action
```

**Supabase Terms:**
```
- Auth: Supabase authentication service
- REST API: Supabase auto-generated REST API dari PostgreSQL
- Anon Key: Public API key untuk anonymous access
- Service Role Key: Admin API key dengan full access
- RLS: Row Level Security policies
```

---

## APPENDIX A: CODE EXAMPLES

### A.1 Secure Token Storage Implementation

```java
// SecureTokenStorage.java
package com.project.inet_mobile.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.project.inet_mobile.data.auth.AuthSession;

public class SecureTokenStorage {
    private static final String TAG = "SecureTokenStorage";
    private static final String PREFS_NAME = "SupabaseSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_AUTH_USER_ID = "auth_user_id";

    private final SharedPreferences preferences;

    public SecureTokenStorage(Context context) {
        SharedPreferences prefs;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            Log.d(TAG, "Using encrypted SharedPreferences");
        } catch (Exception e) {
            Log.e(TAG, "Failed to create encrypted preferences, falling back to regular", e);
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }
        this.preferences = prefs;
    }

    // ... rest of TokenStorage methods
}
```

### A.2 ViewModel Example

```java
// BerandaViewModel.java
package com.project.inet_mobile.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.project.inet_mobile.data.model.ActivePackage;
import com.project.inet_mobile.data.repository.CustomerRepository;

public class BerandaViewModel extends ViewModel {

    private final CustomerRepository customerRepository;
    private final MutableLiveData<ActivePackage> activePackage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public BerandaViewModel(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public LiveData<ActivePackage> getActivePackage() {
        return activePackage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadActivePackage(long customerId) {
        isLoading.setValue(true);
        customerRepository.getActivePackage(customerId, new CustomerRepository.Callback<ActivePackage>() {
            @Override
            public void onSuccess(ActivePackage result) {
                isLoading.postValue(false);
                activePackage.postValue(result);
            }

            @Override
            public void onError(Exception exception) {
                isLoading.postValue(false);
                error.postValue(exception.getMessage());
            }
        });
    }

    public void refresh(long customerId) {
        loadActivePackage(customerId);
    }
}
```

### A.3 Retrofit API Service

```java
// SupabaseApiService.java
package com.project.inet_mobile.data.api;

import com.project.inet_mobile.data.model.Customer;
import com.project.inet_mobile.data.model.Invoice;
import com.project.inet_mobile.data.model.Paket;
import com.project.inet_mobile.data.model.Ticket;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SupabaseApiService {

    @GET("/rest/v1/service_packages")
    Call<List<Paket>> getServicePackages(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authorization,
        @Query("select") String select,
        @Query("order") String order
    );

    @GET("/rest/v1/customers")
    Call<List<Customer>> getCustomer(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authorization,
        @Query("id") String customerId,
        @Query("select") String select
    );

    @GET("/rest/v1/invoices")
    Call<List<Invoice>> getInvoices(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authorization,
        @Query("customer_id") String customerId,
        @Query("order") String order,
        @Query("select") String select
    );

    @GET("/rest/v1/tickets")
    Call<List<Ticket>> getTickets(
        @Header("apikey") String apiKey,
        @Header("Authorization") String authorization,
        @Query("customer_id") String customerId,
        @Query("order") String order,
        @Query("select") String select
    );
}
```

---

## APPENDIX B: MIGRATION GUIDES

### B.1 AsyncTask to Coroutines Migration

**Before (AsyncTask):**
```java
new AsyncTask<Void, Void, JSONArray>() {
    @Override
    protected JSONArray doInBackground(Void... voids) {
        // Network call
        return result;
    }

    @Override
    protected void onPostExecute(JSONArray result) {
        callback.onSuccess(result);
    }
}.execute();
```

**After (Coroutines - if migrating to Kotlin):**
```kotlin
viewModelScope.launch {
    try {
        val result = withContext(Dispatchers.IO) {
            // Network call
        }
        callback.onSuccess(result)
    } catch (e: Exception) {
        callback.onError(e)
    }
}
```

**After (Java with ExecutorService):**
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
Handler mainHandler = new Handler(Looper.getMainLooper());

executor.execute(() -> {
    try {
        // Network call
        JSONArray result = performNetworkCall();
        mainHandler.post(() -> callback.onSuccess(result));
    } catch (Exception e) {
        mainHandler.post(() -> callback.onError(e));
    }
});
```

---

## CHANGELOG

**Version 1.0 - 8 November 2025**
- Initial comprehensive analysis
- Complete architecture documentation
- Security audit findings
- 5-phase development roadmap
- Production readiness assessment
- Code examples and migration guides

---

**Document Maintenance:**
- Review and update quarterly
- Update after major feature additions
- Revise estimates based on actual progress
- Document lessons learned
- Keep track of technical debt

---

**END OF DOCUMENT**

*This document serves as the fundamental guide for the development and deployment of the ISP Mobile Application. All developers, stakeholders, and team members should refer to this document for project direction and technical decisions.*

**Next Review Date**: 8 February 2026
**Document Owner**: Development Team Lead
**Last Updated**: 8 November 2025
