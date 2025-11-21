package com.project.inet_mobile.data.remote;


import android.util.Base64;
import android.util.Log;

import androidx.annotation.Nullable;

import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.session.TokenStorage;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that injects Supabase access token into every API call.
 * Includes verbose logging so we can diagnose header issues quickly.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AUTH_DEBUG";
    private final TokenStorage tokenStorage;

    public AuthInterceptor(TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
    }

    @Nullable
    private AuthSession currentSession() {
        AuthSession session = tokenStorage.getSession();
        Log.d(TAG, "═══════════════════════════════");
        Log.d(TAG, "AuthInterceptor invoked");
        Log.d(TAG, "Session exists: " + (session != null));
        if (session == null) {
            Log.e(TAG, "❌ No session stored – user probably not logged in");
            Log.d(TAG, "═══════════════════════════════");
            return null;
        }

        if (session.isExpired()) {
            Log.e(TAG, "❌ Session expired at: " + new Date(session.getExpiresAtMillis()));
            Log.d(TAG, "Current time: " + new Date());
            Log.d(TAG, "═══════════════════════════════");
            return null;
        }

        Log.d(TAG, "✅ Session valid. Token type: " + session.getTokenType());
        Log.d(TAG, "Token preview: " + previewToken(session.getAccessToken()));
        Log.d(TAG, "═══════════════════════════════");
        return session;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header("Content-Type", "application/json");

        AuthSession session = currentSession();
        if (session != null) {
            String tokenType = session.getTokenType() != null ? session.getTokenType() : "Bearer";
            String authHeader = tokenType + " " + session.getAccessToken();
            builder.header("Authorization", authHeader.trim());
            Log.d(TAG, "Authorization header added: " + tokenType + " " + previewToken(session.getAccessToken()));
        } else {
            Log.w(TAG, "Proceeding WITHOUT Authorization header");
        }

        return chain.proceed(builder.build());
    }

    private String previewToken(@Nullable String token) {
        if (token == null) {
            return "null";
        }
        int previewLength = Math.min(token.length(), 40);
        return token.substring(0, previewLength) + "...";
    }

    @SuppressWarnings("unused")
    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return true;
            }
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE), StandardCharsets.UTF_8);
            JSONObject json = new JSONObject(payload);
            long exp = json.getLong("exp") * 1000L;
            boolean expired = System.currentTimeMillis() >= exp;
            if (expired) {
                Log.e(TAG, "Token expired at: " + new Date(exp));
            }
            return expired;
        } catch (Exception ex) {
            Log.e(TAG, "Failed to parse token expiry: " + ex.getMessage());
            return true;
        }
    }
}
