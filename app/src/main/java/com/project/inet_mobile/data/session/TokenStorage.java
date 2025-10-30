package com.project.inet_mobile.data.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.project.inet_mobile.data.auth.AuthSession;

/**
 * Helper untuk menyimpan token Supabase secara lokal.
 * (Catatan: gunakan EncryptedSharedPreferences/DataStore pada iterasi berikutnya.)
 */
public class TokenStorage {

    private static final String PREFS_NAME = "SupabaseSession";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_AUTH_USER_ID = "auth_user_id";

    private final SharedPreferences preferences;

    public TokenStorage(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(AuthSession session) {
        if (session == null) {
            return;
        }

        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, session.getAccessToken())
                .putString(KEY_REFRESH_TOKEN, session.getRefreshToken())
                .putLong(KEY_EXPIRES_AT, session.getExpiresAtMillis())
                .putString(KEY_TOKEN_TYPE, session.getTokenType())
                .putString(KEY_AUTH_USER_ID, session.getAuthUserId())
                .apply();
    }

    @Nullable
    public AuthSession getSession() {
        String accessToken = preferences.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null);
        long expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L);
        String tokenType = preferences.getString(KEY_TOKEN_TYPE, "bearer");
        String authUserId = preferences.getString(KEY_AUTH_USER_ID, null);

        if (accessToken == null || accessToken.isEmpty() || authUserId == null || authUserId.isEmpty()) {
            return null;
        }

        if (expiresAt == 0L) {
            // fallback: anggap token sudah kedaluwarsa
            return null;
        }

        return new AuthSession(accessToken, refreshToken, expiresAt, tokenType, authUserId);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
