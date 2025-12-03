package com.project.inet_mobile.data.auth;

import android.content.Context;

import androidx.annotation.Nullable;

import com.project.inet_mobile.data.session.TokenStorage;

/**
 * Handles session validation and refresh logic in one place.
 */
public class SessionManager {

    private final TokenStorage tokenStorage;
    private final SupabaseAuthService authService;
    private final long refreshThresholdMs;

    public SessionManager(Context context, SupabaseAuthService authService, long refreshThresholdMs) {
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
        this.authService = authService;
        this.refreshThresholdMs = refreshThresholdMs;
    }

    /**
     * Returns a valid session. Will refresh if expired or near expiry.
     */
    @Nullable
    public synchronized AuthSession getValidSession() throws AuthException {
        AuthSession session = tokenStorage.getSession();
        if (session == null) {
            return null;
        }

        if (!session.isExpired() && !isExpiringSoon(session)) {
            return session;
        }

        if (session.getRefreshToken() == null || session.getRefreshToken().isEmpty()) {
            return null;
        }

        AuthSession refreshed = authService.refreshSession(session.getRefreshToken());
        tokenStorage.saveSession(refreshed);
        return refreshed;
    }

    private boolean isExpiringSoon(AuthSession session) {
        long remaining = session.getExpiresAtMillis() - System.currentTimeMillis();
        return remaining <= refreshThresholdMs;
    }

    public void clearSession() {
        tokenStorage.clear();
    }
}
