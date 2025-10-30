package com.project.inet_mobile.data.auth;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository yang mengorkestrasi proses login Supabase (Auth + profil).
 */
public class AuthRepository {

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
            try {
                AuthSession session = authService.signIn(email, password);
                UserProfile profile = authService.fetchUserProfile(session);
                SignInResult result = new SignInResult(session, profile);
                mainHandler.post(() -> callback.onSuccess(result));
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
