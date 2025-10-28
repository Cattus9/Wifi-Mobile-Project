package com.project.inet_mobile.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class conn {

    private static final String SUPABASE_URL = "https://rqmzvonjytyjdfhpqwvc.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJxbXp2b25qeXR5amRmaHBxd3ZjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1NzMwNDYsImV4cCI6MjA3NTE0OTA0Nn0.AKKfD3x1opGRzWa2Y6IYcdgLGMmizDeCGBi2WPBZr_Q";
    private static final String PREFS_NAME = "SupabaseAuth";
    private static final String KEY_ACCESS_TOKEN = "access_token";

    // Public accessors for constants
    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getSupabaseKey() {
        return SUPABASE_KEY;
    }

    public interface AuthCallback {
        void onSuccess(String accessToken);
        void onError(String error);
    }

    // Sign up a new user
    public static void signUp(Context context, String email, String password, AuthCallback callback) {
        new AuthTask(context, "signup", email, password, callback).execute();
    }

    // Sign in existing user
    public static void signIn(Context context, String email, String password, AuthCallback callback) {
        new AuthTask(context, "signin", email, password, callback).execute();
    }

    // Get stored access token
    public static String getAccessToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ACCESS_TOKEN, "");
    }

    // Save access token
    private static void saveAccessToken(Context context, String token) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply();
    }

    // Sign in with direct authentication for plain text passwords (debug mode)
    public static void signInCustom(Context context, String email, String password, AuthCallback callback) {
        new PlainTextAuthTask(context, email, password, callback).execute();
    }

    // Clear session (logout)
    public static void signOut(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private static class AuthTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private String type; // "signup" or "signin"
        private String email;
        private String password;
        private AuthCallback callback;
        private Exception exception;

        AuthTask(Context context, String type, String email, String password, AuthCallback callback) {
            this.context = context;
            this.type = type;
            this.email = email;
            this.password = password;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                String endpoint = type.equals("signup") ? "/auth/v1/signup" : "/auth/v1/token?grant_type=password";
                URL url = new URL(SUPABASE_URL + endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("apikey", SUPABASE_KEY);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                JSONObject credentials = new JSONObject();
                credentials.put("email", email);
                credentials.put("password", password);

                OutputStream os = connection.getOutputStream();
                os.write(credentials.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = connection.getResponseCode();
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                }

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    return result.toString();
                } else {
                    return null;
                }

            } catch (Exception e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception != null) {
                callback.onError("Network error: " + exception.getMessage());
                return;
            }

            if (result == null) {
                callback.onError("Authentication failed - invalid credentials or server error");
                return;
            }

            try {
                JSONObject response = new JSONObject(result);
                String accessToken = response.getString("access_token");

                // Save token
                saveAccessToken(context, accessToken);

                callback.onSuccess(accessToken);

            } catch (Exception e) {
                callback.onError("Failed to parse response: " + e.getMessage());
            }
        }
    }
    
    // Plain text authentication task for direct database comparison (debug mode)
    private static class PlainTextAuthTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private String email;
        private String password;
        private AuthCallback callback;
        private Exception exception;

        PlainTextAuthTask(Context context, String email, String password, AuthCallback callback) {
            this.context = context;
            this.email = email;
            this.password = password;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Direct query to users table with plain text password comparison
                String encodedEmail = java.net.URLEncoder.encode(email, "UTF-8");
                String encodedPassword = java.net.URLEncoder.encode(password, "UTF-8");
                String endpoint = SUPABASE_URL + "/rest/v1/users" +
                        "?select=id,email,customer_id" +
                        "&email=eq." + encodedEmail +
                        "&password=eq." + encodedPassword;

                URL url = new URL(endpoint);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey", SUPABASE_KEY);
                connection.setRequestProperty("Authorization", "Bearer " + SUPABASE_KEY);
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                java.io.BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream()));
                } else {
                    reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getErrorStream()));
                }

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                if (responseCode >= 200 && responseCode < 300) {
                    return result.toString();
                } else {
                    return null;
                }

            } catch (Exception e) {
                exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception != null) {
                callback.onError("Network error: " + exception.getMessage());
                return;
            }

            if (result == null) {
                callback.onError("Email atau password salah");
                return;
            }

            try {
                org.json.JSONArray array = new org.json.JSONArray(result);
                if (array.length() > 0) {
                    org.json.JSONObject userData = array.getJSONObject(0);
                    String userId = userData.optString("id");
                    
                    if (userId != null && !userId.isEmpty()) {
                        // Save token
                        saveAccessToken(context, userId);
                        callback.onSuccess(userId);
                    } else {
                        callback.onError("Login response does not contain user ID");
                    }
                } else {
                    callback.onError("Email atau password salah");
                }
            } catch (Exception e) {
                callback.onError("Failed to parse response: " + e.getMessage());
            }
        }
    }
    public interface PackagesCallback {
        void onSuccess(org.json.JSONArray data);
        void onError(String error);
    }

    /**
     * Ambil 3 baris pertama service_package dari Supabase.
     * - Menggunakan access_token yang tersimpan (jika ada), jika tidak, fallback ke anon key.
     */
    public static void fetchServicePackages(Context context, PackagesCallback callback) {
        new AsyncTask<Void, Void, org.json.JSONArray>() {
            Exception ex;

            @Override
            protected org.json.JSONArray doInBackground(Void... voids) {
                try {
                    String endpoint = SUPABASE_URL + "/rest/v1/service_packages"
                            + "?select=id,name,description,speed,price,duration"
                            + "&order=id.asc";

                    java.net.URL url = new java.net.URL(endpoint);
                    java.net.HttpURLConnection connHttp = (java.net.HttpURLConnection) url.openConnection();
                    connHttp.setRequestMethod("GET");
                    connHttp.setRequestProperty("apikey", SUPABASE_KEY);

                    // Use access token if valid JWT format, otherwise use anon key
                    String token = getAccessToken(context);
                    // Check if token looks like a JWT (has dots separating header.payload.signature)
                    if (token == null || token.isEmpty() || !token.contains(".") || token.split("\\.").length != 3) {
                        token = SUPABASE_KEY; // Use anon key if not a valid JWT
                    }
                    connHttp.setRequestProperty("Authorization", "Bearer " + token);
                    connHttp.setRequestProperty("Accept", "application/json");

                    int code = connHttp.getResponseCode();
                    java.io.BufferedReader reader;
                    if (code >= 200 && code < 300) {
                        reader = new java.io.BufferedReader(new java.io.InputStreamReader(connHttp.getInputStream()));
                    } else {
                        reader = new java.io.BufferedReader(new java.io.InputStreamReader(connHttp.getErrorStream()));
                    }

                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    if (code >= 200 && code < 300) {
                        return new org.json.JSONArray(sb.toString());
                    } else {
                        throw new RuntimeException("HTTP " + code + ": " + sb);
                    }
                } catch (Exception e) {
                    ex = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(org.json.JSONArray result) {
                if (ex != null) {
                    callback.onError(ex.getMessage());
                } else {
                    callback.onSuccess(result != null ? result : new org.json.JSONArray());
                }
            }
        }.execute();
    }
}