package com.project.inet_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;
import com.project.inet_mobile.util.conn;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextLoginEmail, editTextLoginPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private LottieAnimationView loadingAnimation;

    // ====== SharedPreferences ======
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ACCESS_TOKEN = "accessToken"; // simpan token untuk header Authorization

    // ====== Konfigurasi UX delay minimal ======
    private static final long MIN_LOADING_MS = 2000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // ✅ AKTIFKAN AUTO-LOGIN: Cek apakah user sudah login sebelumnya
        if (isUserLoggedIn() && !TextUtils.isEmpty(getSavedAccessToken())) {
            // Langsung ke Dashboard tanpa perlu login lagi
            navigateToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setListeners();
    }

    private void initViews() {
        editTextLoginEmail = findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        loadingAnimation = findViewById(R.id.loadingAnimation);
    }

    private void setListeners() {
        buttonLogin.setOnClickListener(v -> performLogin());
        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    private void performLogin() {
        CharSequence e = editTextLoginEmail.getText();
        CharSequence p = editTextLoginPassword.getText();
        String email = e == null ? "" : e.toString().trim();
        String password = p == null ? "" : p.toString().trim();

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

        showLoading(true);
        long start = System.currentTimeMillis();

        // Panggil custom auth karena password di-hash
        conn.signInCustom(LoginActivity.this, email, password, new conn.AuthCallback() {
            @Override
            public void onSuccess(String accessToken) {
                // Simpan token segera supaya bisa dipakai fetch
                saveAccessToken(accessToken);
                // Lanjut ambil profil user dari Supabase
                fetchUserData(email, start);
            }

            @Override
            public void onError(String error) {
                long remaining = MIN_LOADING_MS - (System.currentTimeMillis() - start);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, mapAuthError(error), Toast.LENGTH_SHORT).show();
                }, Math.max(0, remaining));
            }
        });
    }

    // ===================== Fetch User =====================

    private void fetchUserData(String email, long startMillis) {
        new Thread(() -> {
            LoginResult result = new LoginResult();
            result.email = email;

            try {
                String endpoint = "/rest/v1/users"
                        + "?select=id,email,customer_id,customers!inner(name)"
                        + "&email=eq." + URLEncoder.encode(email, "UTF-8");

                URL url = new URL(conn.getSupabaseUrl() + endpoint);
                HttpURLConnection http = (HttpURLConnection) url.openConnection();
                http.setRequestMethod("GET");
                http.setRequestProperty("apikey", conn.getSupabaseKey());

                http.setRequestProperty("Authorization", "Bearer " + conn.getSupabaseKey());
                http.setRequestProperty("Accept", "application/json");

                int code = http.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        (code >= 200 && code < 300) ? http.getInputStream() : http.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                if (code >= 200 && code < 300) {
                    JSONArray arr = new JSONArray(sb.toString());
                    if (arr.length() > 0) {
                        JSONObject user = arr.getJSONObject(0);
                        result.userId = user.optString("id", "");

                        // Ambil nama dari relasi customers (fallback ke email jika kosong)
                        String name = null;
                        if (!user.isNull("customers")) {
                            Object c = user.get("customers");
                            if (c instanceof JSONArray) {
                                JSONArray ca = (JSONArray) c;
                                if (ca.length() > 0) {
                                    name = ca.getJSONObject(0).optString("name", null);
                                }
                            } else if (c instanceof JSONObject) {
                                name = ((JSONObject) c).optString("name", null);
                            }
                        }
                        result.userName = !TextUtils.isEmpty(name) ? name : email;
                        result.success = true;
                    } else {
                        result.success = false;
                        result.error = "USER_NOT_FOUND";
                    }
                } else {
                    result.success = false;
                    result.error = "CONNECTION_FAILED";
                }
            } catch (Exception ex) {
                result.success = false;
                result.error = "ERROR: " + ex.getMessage();
            }

            long remaining = MIN_LOADING_MS - (System.currentTimeMillis() - startMillis);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showLoading(false);
                if (!result.success) {
                    Toast.makeText(LoginActivity.this, mapFetchError(result.error), Toast.LENGTH_SHORT).show();
                } else {
                    // Simpan sesi lengkap
                    saveLoginSession(result.userId, result.userName, result.email);
                    Toast.makeText(LoginActivity.this, "Login berhasil! Selamat datang " + result.userName, Toast.LENGTH_SHORT).show();
                    navigateToDashboard();
                }
            }, Math.max(0, remaining));
        }).start();
    }

    // ===================== SharedPreferences helpers =====================

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void saveLoginSession(String userId, String userName, String userEmail) {
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_USER_EMAIL, userEmail)
                .apply();
    }

    private void saveAccessToken(String token) {
        sharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, token)
                .apply();
    }

    private String getSavedAccessToken() {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, "");
    }

    // ✅ Method untuk logout (panggil dari DashboardActivity atau menu logout)
    public static void logout(AppCompatActivity activity) {
        SharedPreferences sharedPreferences = activity.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_ACCESS_TOKEN)
                .apply();

        // Clear Supabase session
        conn.signOut(activity);

        // Redirect ke LoginActivity
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    // ===================== UI helpers =====================

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingAnimation.setVisibility(android.view.View.VISIBLE);
            buttonLogin.setEnabled(false);
            editTextLoginEmail.setEnabled(false);
            editTextLoginPassword.setEnabled(false);
            textViewRegister.setEnabled(false);
        } else {
            loadingAnimation.setVisibility(android.view.View.GONE);
            buttonLogin.setEnabled(true);
            editTextLoginEmail.setEnabled(true);
            editTextLoginPassword.setEnabled(true);
            textViewRegister.setEnabled(true);
        }
    }

    private String mapAuthError(String err) {
        if (err == null) return "Login gagal";
        String lower = err.toLowerCase();
        if (lower.contains("invalid")) return "Email atau password salah";
        if (lower.contains("network") || lower.contains("timeout")) return "Koneksi bermasalah";
        return "Login gagal: " + err;
    }

    private String mapFetchError(String err) {
        if (err == null) return "Gagal memuat data user";
        switch (err) {
            case "NO_TOKEN": return "Sesi tidak valid. Silakan login ulang.";
            case "USER_NOT_FOUND": return "Data user tidak ditemukan";
            case "CONNECTION_FAILED": return "Gagal terhubung ke server";
            default: return "Gagal memuat data user: " + err;
        }
    }

    // ===================== DTO hasil login =====================
    private static class LoginResult {
        boolean success;
        String userId = "";
        String userName = "";
        String email = "";
        String error = "";
    }
}