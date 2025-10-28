# Laporan Uji Whitebox – LoginActivity

## 1. Info Umum
- **Modul**: `LoginActivity`
- **Tujuan**: Memastikan alur login, pemanggilan API, dan penyimpanan sesi berjalan aman.
- **Lingkungan**: Android (Java), koneksi Supabase (mock/offline untuk uji whitebox).

## 2. Ringkasan Modul
- Aktivitas menangani input email dan password, memanggil `conn.signInCustom`, lalu menarik data profil Supabase.
- Sesi disimpan ke `SharedPreferences` (flag login, id, nama, email, token akses).
- UI selalu menunggu minimal 2 detik sebelum menutup animasi loading.

## 3. Skenario Uji

### TC01 – Email wajib terisi
- **Tujuan**: Menghentikan proses login ketika email kosong.
- **Input**: Email kosong, password `dummy123`.
- **Ekspektasi**: Field email menampilkan pesan "Email is required" dan login tidak diproses.
- **Observasi**: Validasi menghentikan alur; dan `connention.signIn` tidak terpanggil.
- **Status**: PASS
- **Cuplikan Kode** (`LoginActivity.java:94-103`)
  ```java
  if (email.isEmpty()) {
      editTextLoginEmail.setError("Email is required");
      editTextLoginEmail.requestFocus();
      return;
  }
  if (password.isEmpty()) {
      editTextLoginPassword.setError("Password is required");
      editTextLoginPassword.requestFocus();
      return;
  }
  ```

### TC02 – Password wajib terisi
- **Tujuan**: Menghentikan proses login ketika password kosong.
- **Input**: Email `user@example.com`, password kosong.
- **Ekspektasi**: Field password menampilkan pesan "Password is required" dan login tidak diproses.
- **Observasi**: Validasi di `LoginActivity` bekerja sama seperti TC01.
- **Status**: PASS
- **Cuplikan Kode**: sama dengan TC01 karena blok validasi berbagi kode.

### TC03 – Jalur login sukses
- **Tujuan**: Memastikan token dan profil tersimpan, lalu pindah ke dashboard.
- **Input**: Email dan password valid; `conn.signInCustom` mengembalikan token `token123`; Supabase merespons profil dengan nama pelanggan.
- **Ekspektasi**: Setelah delay 2 detik, token disimpan, data profil disimpan, toast sukses tampil, dan aplikasi berpindah ke `DashboardActivity`.
- **Observasi**: `saveAccessToken` dan `saveLoginSession` terpanggil sebelum `navigateToDashboard()`; toast sukses tampil di tampilan Login.
- **Status**: PASS
- **Cuplikan Kode** (`LoginActivity.java:111-201`)
  ```java
  public void onSuccess(String accessToken) {
      saveAccessToken(accessToken);
      fetchUserData(email, start);
  }
  ...
  if (!result.success) {
      Toast.makeText(LoginActivity.this, mapFetchError(result.error), Toast.LENGTH_SHORT).show();
  } else {
      saveLoginSession(result.userId, result.userName, result.email);
      Toast.makeText(LoginActivity.this, "Login berhasil! Selamat datang " + result.userName, Toast.LENGTH_SHORT).show();
      navigateToDashboard();
  }
  ```

### TC04 – Pesan error kredensial salah
- **Tujuan**: Menampilkan pesan yang tepat ketika password salah.
- **Input**: Email valid, password salah; callback error `"Invalid login credentials"`.
- **Ekspektasi**: Setelah delay 2 detik, loading hilang dan toast menampilkan "Email atau password salah".
- **Observasi**: Handler di `LoginActivity.java:119-125` menjaga delay; `mapAuthError` di `LoginActivity.java:267-272` mengembalikan string yang sesuai.
- **Status**: PASS
- **Cuplikan Kode** (`LoginActivity.java:119-125`, `267-272`)
  ```java
  public void onError(String error) {
      long remaining = MIN_LOADING_MS - (System.currentTimeMillis() - start);
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
          showLoading(false);
          Toast.makeText(LoginActivity.this, mapAuthError(error), Toast.LENGTH_SHORT).show();
      }, Math.max(0, remaining));
  }
  ```
  ```java
  private String mapAuthError(String err) {
      if (err == null) return "Login gagal";
      String lower = err.toLowerCase();
      if (lower.contains("invalid")) return "Email atau password salah";
      if (lower.contains("network") || lower.contains("timeout")) return "Koneksi bermasalah";
      return "Login gagal: " + err;
  }
  ```




## 4. Catatan Tambahan
- Masalah pada TC05 juga terjadi jika `fetchUserData` gagal karena koneksi (misalnya error `CONNECTION_FAILED`); token tetap disimpan tanpa sesi.
- Rekomendasi: Hapus token saat `result.success` bernilai `false`, atau tunda penyimpanan token sampai profil berhasil diambil.

---

# Laporan Uji Whitebox – RegisterActivity

## 1. Info Umum
- **Modul**: `RegisterActivity`
- **Fokus**: Validasi input dan alur setelah tombol daftar ditekan.
- **Lingkungan**: Android (Java), belum ada koneksi backend aktif.

## 2. Ringkasan Modul
- Semua field wajib diisi dan divalidasi di sisi klien.
- Setelah lolos validasi, aplikasi hanya menampilkan toast sukses dan kembali ke login tanpa menyimpan data.

## 3. Skenario Uji

### RG-01 – NIK kosong
- **Tujuan**: Menangkap kondisi ketika NIK tidak diisi.
- **Input**: NIK kosong, field lain diisi dummy.
- **Ekspektasi**: Muncul pesan "NIK is required" dan proses berhenti.
- **Observasi**: `RegisterActivity.java:67-71` menampilkan error dan menghentikan alur.
- **Status**: PASS
- **Cuplikan Kode** (`RegisterActivity.java:67-83`)
  ```java
  if (nik.isEmpty()) {
      editTextNIK.setError("NIK is required");
      editTextNIK.requestFocus();
      return;
  }

  if (nik.length() != 16) {
      editTextNIK.setError("NIK must be 16 digits");
      editTextNIK.requestFocus();
      return;
  }

  if (!TextUtils.isDigitsOnly(nik)) {
      editTextNIK.setError("NIK must contain only numbers");
      editTextNIK.requestFocus();
      return;
  }
  ```

### RG-02 – NIK tidak 16 digit
- **Tujuan**: Memastikan panjang NIK dicek.
- **Input**: NIK `12345`, field lain valid.
- **Ekspektasi**: Pesan "NIK must be 16 digits".
- **Observasi**: Blok panjang NIK di `RegisterActivity.java:74-77` aktif dan menghentikan proses.
- **Status**: PASS
- **Cuplikan Kode**: lihat kode pada RG-01 (Code ke-2).

### RG-03 – NIK mengandung huruf
- **Tujuan**: Memastikan NIK hanya boleh angka.
- **Input**: NIK `1234ABCD56789012`.
- **Ekspektasi**: Pesan "NIK must contain only numbers".
- **Observasi**: Validasi `TextUtils.isDigitsOnly` di `RegisterActivity.java:80-83` memblokir input.
- **Status**: PASS
- **Cuplikan Kode**: lihat kode pada RG-01 (Code ke-3).

### RG-04 – Nama wajib diisi
- **Tujuan**: Menolak ketika nama kosong.
- **Input**: Nama kosong, field lain valid.
- **Ekspektasi**: Pesan "Name is required".
- **Observasi**: `RegisterActivity.java:86-90` memberikan error dan menghentikan alur.
- **Status**: PASS
- **Cuplikan Kode** (`RegisterActivity.java:86-90`)
  ```java
  if (name.isEmpty()) {
      editTextName.setError("Name is required");
      editTextName.requestFocus();
      return;
  }
  ```

### RG-05 – Nomor telepon tidak valid
- **Tujuan**: Memastikan nomor telepon wajib dan berpola benar.
- **Input**: Telepon kosong atau `abc123`, field lain valid.
- **Ekspektasi**: Pesan "Phone number is required" atau "Please enter a valid phone number".
- **Observasi**: Blok `RegisterActivity.java:93-103` melindungi dua kondisi tersebut.
- **Status**: PASS
- **Cuplikan Kode** (`RegisterActivity.java:93-103`)
  ```java
  if (phone.isEmpty()) {
      editTextPhone.setError("Phone number is required");
      editTextPhone.requestFocus();
      return;
  }

  if (!Patterns.PHONE.matcher(phone).matches()) {
      editTextPhone.setError("Please enter a valid phone number");
      editTextPhone.requestFocus();
      return;
  }
  ```

### RG-06 – Email tidak valid
- **Tujuan**: Menolak email kosong atau format salah.
- **Input**: Email kosong atau `user@invalid`, field lain valid.
- **Ekspektasi**: Pesan "Email is required" atau "Please enter a valid email address".
- **Observasi**: `RegisterActivity.java:106-116` memunculkan error sesuai kasus.
- **Status**: PASS
- **Cuplikan Kode** (`RegisterActivity.java:106-116`)
  ```java
  if (email.isEmpty()) {
      editTextEmail.setError("Email is required");
      editTextEmail.requestFocus();
      return;
  }

  if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      editTextEmail.setError("Please enter a valid email address");
      editTextEmail.requestFocus();
      return;
  }
  ```

### RG-07 – Password kurang dari 6 karakter
- **Tujuan**: Memastikan password minimum 6 karakter.
- **Input**: Password `12345`.
- **Ekspektasi**: Pesan "Password must be at least 6 characters".
- **Observasi**: Validasi di `RegisterActivity.java:119-123` memblokir input pendek.
- **Status**: PASS
- **Cuplikan Kode** (`RegisterActivity.java:119-123`)
  ```java
  if (password.isEmpty() || password.length() < 6) {
      editTextPassword.setError("Password must be at least 6 characters");
      editTextPassword.requestFocus();
      return;
  }
  ```

### RG-08 – Alamat wajib diisi
- **Tujuan**: Memastikan alamat tidak boleh kosong.
- **Input**: Alamat kosong.
- **Ekspektasi**: Pesan "Address is required".
- **Observasi**: `RegisterActivity.java:126-130` memunculkan error dan berhenti.
- **Status**: PASS
- **Cuplikan Kode** (`RegisterActivity.java:126-130`)
  ```java
  if (address.isEmpty()) {
      editTextAddress.setError("Address is required");
      editTextAddress.requestFocus();
      return;
  }
  ```

### RG-09 – Proses register tanpa backend
- **Tujuan**: Menilai alur akhir ketika semua validasi lolos.
- **Input**: Semua field valid.
- **Ekspektasi**: Data dikirim ke server dan tersimpan.
- **Observasi**: Kode hanya menampilkan toast sukses (`RegisterActivity.java:133-134`) dan kembali ke login (`RegisterActivity.java:138-140`); tidak ada pemanggilan API atau penyimpanan data.
- **Status**: FAIL
- **Cuplikan Kode** (`RegisterActivity.java:133-140`)
  ```java
  Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();

  // Here you would normally send the data to your server
  Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
  startActivity(intent);
  finish();
  ```
- **Catatan Bug**: Fitur belum tersambung ke database sehingga tidak ada data yang benar-benar tersimpan.

## 4. Catatan Tambahan
- Semua validasi sudah berjalan di sisi klien; integrasi ke backend perlu ditambahkan sebelum fitur dianggap selesai.
