package com.project.inet_mobile.data.auth;

public class AuthSession {
    private final String accessToken;
    private final String refreshToken;
    private final long expiresAtMillis;
    private final String tokenType;
    private final String authUserId;

    public AuthSession(String accessToken,
                       String refreshToken,
                       long expiresAtMillis,
                       String tokenType,
                       String authUserId) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAtMillis = expiresAtMillis;
        this.tokenType = tokenType;
        this.authUserId = authUserId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresAtMillis() {
        return expiresAtMillis;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtMillis;
    }
}
