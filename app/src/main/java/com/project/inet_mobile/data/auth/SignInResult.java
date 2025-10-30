package com.project.inet_mobile.data.auth;

public class SignInResult {
    private final AuthSession session;
    private final UserProfile profile;

    public SignInResult(AuthSession session, UserProfile profile) {
        this.session = session;
        this.profile = profile;
    }

    public AuthSession getSession() {
        return session;
    }

    public UserProfile getProfile() {
        return profile;
    }
}
