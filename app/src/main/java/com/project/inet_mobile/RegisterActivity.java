package com.project.inet_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.textfield.TextInputEditText;
import com.project.inet_mobile.data.auth.AuthException;
import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.data.auth.UserRepository;
import com.project.inet_mobile.data.remote.SupabaseUserService;
import com.project.inet_mobile.util.conn;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText editTextName, editTextPhone, editTextEmail, editTextPassword, editTextAddress;
    private Button buttonRegister;
    private TextView textViewLogin;
    private LottieAnimationView loadingAnimation;
    private CardView cardLoadingAnimation;
    private View overlayBackground;

    private SupabaseAuthService supabaseAuthService;
    private UserRepository userRepository;

    private Long selectedPackageId; // Package selected from PackageSelectionActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Get selected package ID from intent
        if (getIntent().hasExtra("SELECTED_PACKAGE_ID")) {
            int packageIdInt = getIntent().getIntExtra("SELECTED_PACKAGE_ID", -1);
            if (packageIdInt > 0) {
                selectedPackageId = Long.valueOf(packageIdInt);
                Log.d(TAG, "Received selected package ID: " + selectedPackageId);
            } else {
                selectedPackageId = null;
                Log.w(TAG, "Invalid package ID received: " + packageIdInt);
            }
        } else {
            // Optional: Allow registration without package (backward compatibility)
            selectedPackageId = null;
            Log.w(TAG, "No package ID received - registering without package");
        }

        initServices();
        initViews();
        setListeners();
    }

    private void initServices() {
        String baseUrl = conn.getSupabaseUrl();
        String apiKey = conn.getSupabaseKey();
        supabaseAuthService = new SupabaseAuthService(baseUrl, apiKey);

        // Create simple Retrofit instance without authentication interceptor
        // (for anonymous registration call)
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl + "/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SupabaseUserService supabaseUserService = retrofit.create(SupabaseUserService.class);
        userRepository = new UserRepository(supabaseUserService, new com.project.inet_mobile.data.session.TokenStorage(this));
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextAddress = findViewById(R.id.editTextAddress);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        cardLoadingAnimation = findViewById(R.id.cardLoadingAnimation);
        overlayBackground = findViewById(R.id.overlayBackground);
    }

    private void setListeners() {
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    performRegister();
                } catch (Throwable t) {
                    // Catch everything, including Errors, and show them in a dialog
                    Log.e(TAG, "Crash caught in onClickListener", t);
                    showErrorDialog(t);
                }
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to login activity
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showErrorDialog(Throwable t) {
        // Ensure this runs on the UI thread
        runOnUiThread(() -> {
            new androidx.appcompat.app.AlertDialog.Builder(RegisterActivity.this)
                .setTitle("Terjadi Error")
                .setMessage(t.getClass().getName() + ":\n\n" + t.getMessage() + "\n\n" + android.util.Log.getStackTraceString(t))
                .setPositiveButton("OK", null)
                .show();
            
            // Also hide loading animation if it was showing
            showLoading(false);
        });
    }

    private void performRegister() {
        // Defensive UI element check
        if (editTextName == null || editTextPhone == null || editTextEmail == null || 
            editTextPassword == null || editTextAddress == null || buttonRegister == null) {
            Toast.makeText(this, "Kesalahan inisialisasi UI. Elemen tidak ditemukan.", Toast.LENGTH_LONG).show();
            return;
        }

        final String name = editTextName.getText().toString().trim();
        final String phone = editTextPhone.getText().toString().trim();
        final String email = editTextEmail.getText().toString().trim();
        final String password = editTextPassword.getText().toString().trim();
        final String address = editTextAddress.getText().toString().trim();

        // Validate Name field
        if (name.isEmpty()) {
            editTextName.setError("Nama wajib diisi");
            editTextName.requestFocus();
            return;
        }

        // Validate Phone field
        if (phone.isEmpty()) {
            editTextPhone.setError("Nomor telepon wajib diisi");
            editTextPhone.requestFocus();
            return;
        }

        if (!Patterns.PHONE.matcher(phone).matches()) {
            editTextPhone.setError("Nomor telepon tidak valid");
            editTextPhone.requestFocus();
            return;
        }

        // Validate Email field
        if (email.isEmpty()) {
            editTextEmail.setError("Email wajib diisi");
            editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Email tidak valid");
            editTextEmail.requestFocus();
            return;
        }

        // Validate Password field
        if (password.isEmpty() || password.length() < 6) {
            editTextPassword.setError("Password minimal 6 karakter");
            editTextPassword.requestFocus();
            return;
        }

        // Validate Address field
        if (address.isEmpty()) {
            editTextAddress.setError("Alamat wajib diisi");
            editTextAddress.requestFocus();
            return;
        }

        // Show loading
        showLoading(true);

        // Perform registration in background thread
        new Thread(() -> {
            try {
                // STEP 1: Sign up with Supabase Auth
                Log.d(TAG, "Starting Supabase Auth signup...");
                AuthSession authSession = supabaseAuthService.signUp(email, password);
                final String authUserId = authSession.getAuthUserId();

                if (authUserId == null || authUserId.isEmpty()) {
                    throw new AuthException(500, "Auth user ID tidak ditemukan setelah signup");
                }

                Log.d(TAG, "Supabase Auth signup successful. auth_user_id: " + authUserId);

                // STEP 2: Create customer and user records in database
                runOnUiThread(() -> {
                    // Pass selectedPackageId to RPC function
                    Log.d(TAG, "Calling registerUser with package_id: " + selectedPackageId);
                    userRepository.registerUser(authUserId, email, phone, name, address, selectedPackageId, new UserRepository.RegisterCallback() {
                        @Override
                        public void onSuccess(String message) {
                            showLoading(false);
                            Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                            // Navigate to login
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            intent.putExtra("registered_email", email);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            showLoading(false);
                            Log.e(TAG, "Register user error: " + errorMessage);

                            // Parse error message
                            String userMessage = parseErrorMessage(errorMessage);
                            Toast.makeText(RegisterActivity.this, userMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                });

            } catch (AuthException e) {
                Log.e(TAG, "Signup error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showLoading(false);
                    String userMessage = parseAuthError(e);
                    Toast.makeText(RegisterActivity.this, userMessage, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                // Catch any unexpected errors
                Log.e(TAG, "Unexpected registration error: " + e.getMessage(), e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this,
                        "Terjadi kesalahan tidak terduga: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private String parseAuthError(AuthException e) {
        String message = e.getMessage();
        if (message == null) {
            return "Terjadi kesalahan saat registrasi";
        }

        // Parse common Supabase Auth errors
        if (message.contains("already registered") || message.contains("already exists")) {
            return "Email sudah terdaftar";
        } else if (message.contains("invalid email")) {
            return "Email tidak valid";
        } else if (message.contains("password")) {
            return "Password tidak memenuhi syarat";
        } else {
            return "Registrasi gagal: " + message;
        }
    }

    private String parseErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return "Terjadi kesalahan saat registrasi";
        }

        // Parse RPC function errors
        if (errorMessage.contains("EMAIL_ALREADY_EXISTS")) {
            return "Email sudah terdaftar dalam sistem";
        } else if (errorMessage.contains("AUTH_USER_ALREADY_LINKED")) {
            return "User sudah terhubung dengan akun lain";
        } else if (errorMessage.contains("INVALID_PACKAGE")) {
            return "Paket layanan tidak valid atau tidak aktif";
        } else {
            return errorMessage;
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            cardLoadingAnimation.setVisibility(View.VISIBLE);
            overlayBackground.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
            buttonRegister.setEnabled(false);
        } else {
            cardLoadingAnimation.setVisibility(View.GONE);
            overlayBackground.setVisibility(View.GONE);
            loadingAnimation.cancelAnimation();
            buttonRegister.setEnabled(true);
        }
    }
}
