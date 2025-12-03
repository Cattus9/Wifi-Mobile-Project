package com.project.inet_mobile.data.auth;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository yang mengorkestrasi proses login Supabase (Auth + profil).
 */
public class AuthRepository {

    private static final String TAG = "AuthRepository";
    private final SupabaseAuthService authService;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public AuthRepository(SupabaseAuthService authService) {
        this.authService = authService;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public interface SignInCallback {
        void onSuccess(SignInResult result);
        void onError(AuthException exception);
    }

    public void signIn(final String email, final String password, final SignInCallback callback) {
        executorService.execute(() -> {
            long start = System.currentTimeMillis();
            try {
                Log.d(TAG, "signIn start email=" + email);
                AuthSession session = authService.signIn(email, password);
                UserProfile profile = authService.fetchUserProfile(session);
                SignInResult result = new SignInResult(session, profile);
                long elapsed = System.currentTimeMillis() - start;
                Log.d(TAG, "signIn success latencyMs=" + elapsed);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (AuthException ex) {
                long elapsed = System.currentTimeMillis() - start;
                Log.e(TAG, "signIn failed latencyMs=" + elapsed + " msg=" + ex.getMessage());
                mainHandler.post(() -> callback.onError(ex));
            } catch (Exception ex) {
                AuthException wrapped = new AuthException("Terjadi kesalahan tak terduga: " + ex.getMessage(), ex);
                long elapsed = System.currentTimeMillis() - start;
                Log.e(TAG, "signIn unexpected error latencyMs=" + elapsed + " msg=" + ex.getMessage());
                mainHandler.post(() -> callback.onError(wrapped));
            }
        });
    }

    public interface AuthCallback {
        void onSuccess();
        void onError(AuthException exception);
    }

    public void sendPasswordResetOtp(final String email, final AuthCallback callback) {
        executorService.execute(() -> {
            try {
                authService.sendPasswordResetOtp(email);
                mainHandler.post(callback::onSuccess);
            } catch (AuthException ex) {
                mainHandler.post(() -> callback.onError(ex));
            } catch (Exception ex) {
                AuthException wrapped = new AuthException("Terjadi kesalahan tak terduga: " + ex.getMessage(), ex);
                mainHandler.post(() -> callback.onError(wrapped));
            }
        });
    }

    public interface VerifyOtpCallback {
        void onSuccess(AuthSession session);
        void onError(AuthException exception);
    }

    public void verifyOtp(final String email, final String token, final VerifyOtpCallback callback) {
        executorService.execute(() -> {
            try {
                AuthSession session = authService.verifyOtp(email, token);
                mainHandler.post(() -> callback.onSuccess(session));
            } catch (AuthException ex) {
                mainHandler.post(() -> callback.onError(ex));
            } catch (Exception ex) {
                AuthException wrapped = new AuthException("Terjadi kesalahan tak terduga: " + ex.getMessage(), ex);
                mainHandler.post(() -> callback.onError(wrapped));
            }
        });
    }

    public void updateUserPassword(final String accessToken, final String newPassword, final AuthCallback callback) {
        executorService.execute(() -> {
            try {
                authService.updateUserPassword(accessToken, newPassword);
                mainHandler.post(callback::onSuccess);
            } catch (AuthException ex) {
                mainHandler.post(() -> callback.onError(ex));
            } catch (Exception ex) {
                AuthException wrapped = new AuthException("Terjadi kesalahan tak terduga: " + ex.getMessage(), ex);
                mainHandler.post(() -> callback.onError(wrapped));
            }
        });
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
