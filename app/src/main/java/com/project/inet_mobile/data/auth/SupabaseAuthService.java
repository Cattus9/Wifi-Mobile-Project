package com.project.inet_mobile.data.auth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Low level service that talks to Supabase Auth and REST endpoints.
 */
public class SupabaseAuthService {

    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;

    public SupabaseAuthService(String baseUrl, String apiKey) {
        this.httpClient = new OkHttpClient();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
    }

    /**
     * Sign up new user with email and password in Supabase Auth
     */
    public AuthSession signUp(String email, String password) throws AuthException {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/signup")
                    .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw buildAuthException(response.code(), raw);
            }

            JSONObject json = new JSONObject(raw);
            String accessToken = json.optString("access_token", "");
            String refreshToken = json.optString("refresh_token", "");
            long expiresIn = json.optLong("expires_in", 3600L);
            String tokenType = json.optString("token_type", "bearer");

            JSONObject userObj = json.optJSONObject("user");
            String authUserId = userObj != null ? userObj.optString("id", "") : "";

            if (authUserId == null || authUserId.isEmpty()) {
                throw new AuthException(response.code(), "Respons signup tidak lengkap dari server");
            }

            // Access token might be empty if email confirmation is required
            long expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L);
            return new AuthSession(accessToken, refreshToken, expiresAtMillis, tokenType, authUserId);
        } catch (JSONException e) {
            throw new AuthException("Gagal mem-parsing respons signup", e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    public AuthSession signIn(String email, String password) throws AuthException {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("password", password);

            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/token?grant_type=password")
                    .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw buildAuthException(response.code(), raw);
            }

            JSONObject json = new JSONObject(raw);
            String accessToken = json.optString("access_token", "");
            String refreshToken = json.optString("refresh_token", "");
            long expiresIn = json.optLong("expires_in", 3600L);
            String tokenType = json.optString("token_type", "bearer");

            JSONObject userObj = json.optJSONObject("user");
            String authUserId = userObj != null ? userObj.optString("id", "") : "";

            if (accessToken == null || accessToken.isEmpty() || authUserId == null || authUserId.isEmpty()) {
                throw new AuthException(response.code(), "Respons login tidak lengkap dari server");
            }

            long expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L);
            return new AuthSession(accessToken, refreshToken, expiresAtMillis, tokenType, authUserId);
        } catch (JSONException e) {
            throw new AuthException("Gagal mem-parsing respons login", e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh Supabase session menggunakan refresh_token.
     */
    public AuthSession refreshSession(String refreshToken) throws AuthException {
        try {
            JSONObject body = new JSONObject();
            body.put("refresh_token", refreshToken);

            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/token?grant_type=refresh_token")
                    .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw buildAuthException(response.code(), raw);
            }

            JSONObject json = new JSONObject(raw);
            String accessToken = json.optString("access_token", "");
            String newRefreshToken = json.optString("refresh_token", refreshToken);
            long expiresIn = json.optLong("expires_in", 3600L);
            String tokenType = json.optString("token_type", "bearer");

            JSONObject userObj = json.optJSONObject("user");
            String authUserId = userObj != null ? userObj.optString("id", "") : "";

            if (accessToken == null || accessToken.isEmpty() || authUserId == null || authUserId.isEmpty()) {
                throw new AuthException(response.code(), "Respons refresh tidak lengkap dari server");
            }

            long expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L);
            return new AuthSession(accessToken, newRefreshToken, expiresAtMillis, tokenType, authUserId);
        } catch (JSONException e) {
            throw new AuthException("Gagal mem-parsing respons refresh", e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    public UserProfile fetchUserProfile(AuthSession session) throws AuthException {
        try {
            HttpUrl url = HttpUrl.parse(baseUrl + "/rest/v1/users");
            if (url == null) {
                throw new AuthException("URL Supabase tidak valid: /rest/v1/users", new IllegalArgumentException("Invalid Supabase base URL"));
            }

            HttpUrl.Builder builder = url.newBuilder()
                    .addQueryParameter("select", "id,email,customer_id,customers(*)")
                    .addQueryParameter("auth_user_id", "eq." + session.getAuthUserId())
                    .addQueryParameter("limit", "1");

            Request request = new Request.Builder()
                    .url(builder.build())
                    .get()
                    .header("apikey", apiKey)
                    .header("Authorization", buildAuthHeader(session))
                    .header("Accept", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw buildAuthException(response.code(), raw);
            }

            JSONArray array = new JSONArray(raw);
            if (array.length() == 0) {
                throw new AuthException(response.code(), "Data profil pengguna tidak ditemukan");
            }

            JSONObject profile = array.getJSONObject(0);
            long userId = profile.optLong("id", 0L);
            String email = profile.optString("email", "");
            Long customerId = profile.isNull("customer_id") ? null : profile.optLong("customer_id");
            String displayName = email;

            if (!profile.isNull("customers")) {
                Object customers = profile.get("customers");
                JSONObject customerObject = null;
                if (customers instanceof JSONArray) {
                    JSONArray customerArray = (JSONArray) customers;
                    if (customerArray.length() > 0) {
                        customerObject = customerArray.getJSONObject(0);
                    }
                } else if (customers instanceof JSONObject) {
                    customerObject = (JSONObject) customers;
                }

                if (customerObject != null) {
                    String name = customerObject.optString("name", "");
                    if (name != null && !name.isEmpty()) {
                        displayName = name;
                    }
                }
            }

            if (displayName == null || displayName.isEmpty()) {
                displayName = email;
            }

            return new UserProfile(userId, email, customerId, displayName);
        } catch (JSONException e) {
            throw new AuthException("Gagal mem-parsing data profil: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetOtp(String email) throws AuthException {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);

            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/recover") // CORRECT ENDPOINT
                    .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                String raw = response.body() != null ? response.body().string() : "";
                throw buildAuthException(response.code(), raw);
            }
            // Success is indicated by a 2xx response code, body is empty.
        } catch (JSONException e) {
            throw new AuthException("Gagal membuat JSON request body", e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    public AuthSession verifyOtp(String email, String token) throws AuthException {
        try {
            JSONObject body = new JSONObject();
            body.put("type", "recovery");
            body.put("email", email);
            body.put("token", token);

            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/verify")
                    .post(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                    .header("apikey", apiKey)
                    .header("Content-Type", "application/json")
                    .build();

            Response response = httpClient.newCall(request).execute();
            String raw = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw buildAuthException(response.code(), raw);
            }

            // A successful OTP verification returns a full session object
            JSONObject json = new JSONObject(raw);
            String accessToken = json.optString("access_token", "");
            String refreshToken = json.optString("refresh_token", "");
            long expiresIn = json.optLong("expires_in", 3600L);
            String tokenType = json.optString("token_type", "bearer");

            JSONObject userObj = json.optJSONObject("user");
            String authUserId = userObj != null ? userObj.optString("id", "") : "";

            if (accessToken == null || accessToken.isEmpty() || authUserId == null || authUserId.isEmpty()) {
                throw new AuthException(response.code(), "Respons verifikasi OTP tidak lengkap");
            }

            long expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L);
            return new AuthSession(accessToken, refreshToken, expiresAtMillis, tokenType, authUserId);

        } catch (JSONException e) {
            throw new AuthException("Gagal mem-parsing respons verifikasi", e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    public void updateUserPassword(String accessToken, String newPassword) throws AuthException {
        try {
            JSONObject body = new JSONObject();
            body.put("password", newPassword);

            Request request = new Request.Builder()
                    .url(baseUrl + "/auth/v1/user")
                    .put(RequestBody.create(body.toString(), MEDIA_TYPE_JSON))
                    .header("apikey", apiKey)
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                String raw = response.body() != null ? response.body().string() : "";
                throw buildAuthException(response.code(), raw);
            }
            // A 200 OK response with the user object is returned on success.
        } catch (JSONException e) {
            throw new AuthException("Gagal membuat JSON request body", e);
        } catch (IOException e) {
            throw new AuthException("Koneksi ke server gagal: " + e.getMessage(), e);
        }
    }

    private String buildAuthHeader(AuthSession session) {
        String tokenType = session.getTokenType() != null ? session.getTokenType() : "bearer";
        return capitalizeTokenType(tokenType) + " " + session.getAccessToken();
    }

    private String capitalizeTokenType(String tokenType) {
        if (tokenType == null || tokenType.isEmpty()) {
            return "Bearer";
        }
        if (tokenType.equalsIgnoreCase("bearer")) {
            return "Bearer";
        }
        return tokenType;
    }

    private AuthException buildAuthException(int statusCode, String rawBody) {
        String errorType = null;
        String message = rawBody;

        if (rawBody != null && !rawBody.isEmpty() && (rawBody.trim().startsWith("{") || rawBody.trim().startsWith("["))) {
            try {
                JSONObject obj = new JSONObject(rawBody);
                errorType = obj.optString("error", null);
                if (obj.has("error_description")) {
                    message = obj.optString("error_description");
                } else if (obj.has("message")) {
                    message = obj.optString("message");
                }
            } catch (JSONException ignore) {
                // fallback to raw body
            }
        }

        if (message == null || message.isEmpty()) {
            message = "Permintaan ke server gagal (kode " + statusCode + ")";
        }

        return new AuthException(statusCode, errorType, message);
    }
}
