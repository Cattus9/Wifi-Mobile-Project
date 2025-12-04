package com.project.inet_mobile.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.project.inet_mobile.data.auth.AuthSession;

import java.util.Map;

/**
 * Helper untuk menyimpan token Supabase secara lokal.
 * (Catatan: gunakan EncryptedSharedPreferences/DataStore pada iterasi berikutnya.)
 */
public class TokenStorage {

    private static final String TAG = "TokenStorage";
    private static final String PREFS_NAME_LEGACY = "SupabaseSession";
    private static final String PREFS_NAME_SECURE = "SupabaseSessionSecure";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_AUTH_USER_ID = "auth_user_id";

    private final SharedPreferences preferences;

    public TokenStorage(Context context) {
        SharedPreferences encryptedPrefs;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME_SECURE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception ex) {
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME_SECURE, Context.MODE_PRIVATE);
        }

        this.preferences = encryptedPrefs;
        migrateLegacyIfNeeded(context, encryptedPrefs);
    }

    public void saveSession(AuthSession session) {
        if (session == null) {
            Log.w(TAG, "saveSession: session is null, not saving");
            return;
        }

        Log.d(TAG, "=== SAVING SESSION ===");
        Log.d(TAG, "Access token length: " + (session.getAccessToken() != null ? session.getAccessToken().length() : 0));
        Log.d(TAG, "Auth user ID: " + session.getAuthUserId());
        Log.d(TAG, "Expires at: " + session.getExpiresAtMillis());
        Log.d(TAG, "=== END SAVING SESSION ===");

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
        Log.d(TAG, "=== GETTING SESSION ===");

        String accessToken = preferences.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null);
        long expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L);
        String tokenType = preferences.getString(KEY_TOKEN_TYPE, "bearer");
        String authUserId = preferences.getString(KEY_AUTH_USER_ID, null);

        Log.d(TAG, "Access token exists: " + (accessToken != null));
        Log.d(TAG, "Access token length: " + (accessToken != null ? accessToken.length() : 0));
        Log.d(TAG, "Auth user ID: " + authUserId);
        Log.d(TAG, "Expires at: " + expiresAt);
        Log.d(TAG, "Current time: " + System.currentTimeMillis());

        if (accessToken == null || accessToken.isEmpty() || authUserId == null || authUserId.isEmpty()) {
            Log.w(TAG, "getSession: returning null - missing accessToken or authUserId");
            Log.d(TAG, "=== END GETTING SESSION (NULL) ===");
            return null;
        }

        if (expiresAt == 0L) {
            // fallback: anggap token sudah kedaluwarsa
            Log.w(TAG, "getSession: returning null - expiresAt is 0");
            Log.d(TAG, "=== END GETTING SESSION (NULL) ===");
            return null;
        }

        Log.d(TAG, "getSession: returning valid session");
        Log.d(TAG, "=== END GETTING SESSION (SUCCESS) ===");
        return new AuthSession(accessToken, refreshToken, expiresAt, tokenType, authUserId);
    }

    public void clear() {
        preferences.edit().clear().apply();
    }

    /**
     * Check if user is logged in (has valid session)
     */
    public boolean isLoggedIn() {
        return getSession() != null;
    }

    private void migrateLegacyIfNeeded(Context context, SharedPreferences targetPrefs) {
        // Read from legacy plain prefs (older builds) and move into secure prefs.
        SharedPreferences legacyPrefs = context.getSharedPreferences(PREFS_NAME_LEGACY, Context.MODE_PRIVATE);
        if (legacyPrefs == null || legacyPrefs == targetPrefs) {
            return;
        }

        Map<String, ?> all = legacyPrefs.getAll();
        if (all == null || all.isEmpty()) {
            return;
        }

        SharedPreferences.Editor editor = targetPrefs.edit();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            // Skip reserved keys used by EncryptedSharedPreferences
            if (key != null && key.startsWith("__androidx_security_crypto")) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                editor.putString(key, (String) value);
            } else if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            }
        }
        editor.apply();
        legacyPrefs.edit().clear().apply();
    }
}
