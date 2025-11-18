package com.project.inet_mobile.data.remote;

/**
 * Central configuration for backend endpoints used by the mobile app.
 * Update BASE_URL when switching between ngrok/staging/production.
 */
public final class ApiConfig {

    private static final String BASE_URL = "https://unthoroughly-arachidic-aaden.ngrok-free.dev/Form-Handling/";
    public static final String API_V1 = BASE_URL + "api/v1/";

    private ApiConfig() {
        // no-op
    }
}
