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
import com.project.inet_mobile.data.auth.AuthException;
import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.auth.AuthRepository;
import com.project.inet_mobile.data.auth.SignInResult;
import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.data.auth.UserProfile;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.util.conn;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextLoginEmail, editTextLoginPassword;
    private Button buttonLogin;
    private TextView textViewRegister;
    private LottieAnimationView loadingAnimation;

    private SharedPreferences sharedPreferences;
    private TokenStorage tokenStorage;
    private AuthRepository authRepository;

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
        authRepository = new AuthRepository(new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey()));

        if (isUserLoggedIn() && hasValidSession()) {
            navigateToDashboard();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (authRepository != null) {
            authRepository.shutdown();
        }
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

    private boolean hasValidSession() {
        AuthSession session = tokenStorage.getSession();
        return session != null && !session.isExpired();
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
}
