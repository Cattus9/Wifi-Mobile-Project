package com.project.inet_mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
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
import com.project.inet_mobile.data.auth.AuthRepository;
import com.project.inet_mobile.data.auth.SignInResult;
import com.project.inet_mobile.data.auth.SessionManager;
import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.data.auth.UserProfile;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.util.conn;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextLoginEmail, editTextLoginPassword;
    private Button buttonLogin;
    private TextView textViewRegister, textViewForgotPassword;
    private LottieAnimationView loadingAnimation;
    private CardView cardLoadingAnimation;
    private View overlayBackground;

    private SharedPreferences sharedPreferences;
    private TokenStorage tokenStorage;
    private AuthRepository authRepository;
    private SessionManager sessionManager;
    private SupabaseAuthService authService;
    private final ExecutorService sessionExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ID = "userId";

    private static final long MIN_LOADING_MS = 2000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        tokenStorage = new TokenStorage(getApplicationContext());
        authService = new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey());
        authRepository = new AuthRepository(authService);
        sessionManager = new SessionManager(getApplicationContext(), authService, 2 * 60 * 1000L); // refresh when <2 minutes remaining

        setContentView(R.layout.activity_login);
        initViews();
        setListeners();

        maybeAutoLoginWithRefresh();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authRepository != null) {
            authRepository.shutdown();
        }
        sessionExecutor.shutdownNow();
    }

    private void initViews() {
        editTextLoginEmail = findViewById(R.id.editTextLoginEmail);
        editTextLoginPassword = findViewById(R.id.editTextLoginPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewRegister = findViewById(R.id.textViewRegister);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        cardLoadingAnimation = findViewById(R.id.cardLoadingAnimation);
        overlayBackground = findViewById(R.id.overlayBackground);
    }

    private void setListeners() {
        buttonLogin.setOnClickListener(v -> performLogin());
        textViewRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
        textViewForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, AuthFlowActivity.class));
        });
    }

    /**
     * Try to reuse existing session; refresh if needed. Falls back to manual login when invalid.
     */
    private void maybeAutoLoginWithRefresh() {
        AuthSession cached = tokenStorage.getSession();
        if (cached == null) {
            clearLoginSession();
            return;
        }

        showLoading(true);
        sessionExecutor.execute(() -> {
            try {
                AuthSession valid = sessionManager.getValidSession();
                if (valid == null) {
                    sessionManager.clearSession();
                    clearLoginSession();
                    mainHandler.post(() -> showLoading(false));
                    return;
                }

                // Fetch profile to repopulate name/email before navigating
                UserProfile profile = authService.fetchUserProfile(valid);
                tokenStorage.saveSession(valid);

                mainHandler.post(() -> {
                    saveLoginSession(
                            String.valueOf(profile.getUserId()),
                            TextUtils.isEmpty(profile.getDisplayName()) ? profile.getEmail() : profile.getDisplayName(),
                            profile.getEmail()
                    );
                    navigateToDashboard();
                });
            } catch (Exception ex) {
                sessionManager.clearSession();
                clearLoginSession();
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Sesi kadaluarsa, silakan login kembali", Toast.LENGTH_SHORT).show();
                });
            }
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

        authRepository.signIn(email, password, new AuthRepository.SignInCallback() {
            @Override
            public void onSuccess(SignInResult result) {
                long remaining = MIN_LOADING_MS - (System.currentTimeMillis() - start);
                new Handler(Looper.getMainLooper()).postDelayed(() -> handleLoginSuccess(result), Math.max(0, remaining));
            }

            @Override
            public void onError(AuthException exception) {
                long remaining = MIN_LOADING_MS - (System.currentTimeMillis() - start);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, exception.getUserMessage(), Toast.LENGTH_SHORT).show();
                }, Math.max(0, remaining));
            }
        });
    }

    private void handleLoginSuccess(SignInResult result) {
        tokenStorage.saveSession(result.getSession());

        UserProfile profile = result.getProfile();
        String userName = profile.getDisplayName();
        if (TextUtils.isEmpty(userName)) {
            userName = profile.getEmail();
        }

        saveLoginSession(
                String.valueOf(profile.getUserId()),
                userName,
                profile.getEmail()
        );

        showLoading(false);
        Toast.makeText(this, "Login berhasil! Selamat datang " + userName, Toast.LENGTH_SHORT).show();
        navigateToDashboard();
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void clearLoginSession() {
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .apply();
        tokenStorage.clear();
    }

    private void saveLoginSession(String userId, String userName, String userEmail) {
        sharedPreferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_USER_EMAIL, userEmail)
                .apply();
    }

    public static void logout(AppCompatActivity activity) {
        SharedPreferences preferences = activity.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        preferences.edit()
                .putBoolean(KEY_IS_LOGGED_IN, false)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .apply();

        new TokenStorage(activity.getApplicationContext()).clear();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        if (show) {
            // Tampilkan CardView loading dan overlay
            cardLoadingAnimation.setVisibility(View.VISIBLE);
            overlayBackground.setVisibility(View.VISIBLE);

            // Disable semua input
            buttonLogin.setEnabled(false);
            editTextLoginEmail.setEnabled(false);
            editTextLoginPassword.setEnabled(false);
            textViewRegister.setEnabled(false);
        } else {
            // Sembunyikan CardView loading dan overlay
            cardLoadingAnimation.setVisibility(View.GONE);
            overlayBackground.setVisibility(View.GONE);

            // Enable kembali semua input
            buttonLogin.setEnabled(true);
            editTextLoginEmail.setEnabled(true);
            editTextLoginPassword.setEnabled(true);
            textViewRegister.setEnabled(true);
        }
    }
}
