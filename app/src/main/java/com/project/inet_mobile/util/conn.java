package com.project.inet_mobile.util;

import android.content.Context;
import android.os.AsyncTask;

import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.session.TokenStorage;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Legacy helper untuk request sederhana ke Supabase REST.
 * Fungsionalitas login telah dipindahkan ke repository baru.
 */
public class conn {

    private static final String SUPABASE_URL = "https://rqmzvonjytyjdfhpqwvc.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJxbXp2b25qeXR5amRmaHBxd3ZjIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk1NzMwNDYsImV4cCI6MjA3NTE0OTA0Nn0.AKKfD3x1opGRzWa2Y6IYcdgLGMmizDeCGBi2WPBZr_Q";

    public static String getSupabaseUrl() {
        return SUPABASE_URL;
    }

    public static String getSupabaseKey() {
        return SUPABASE_KEY;
    }

    public interface PackagesCallback {
        void onSuccess(JSONArray data);
        void onError(String error);
    }

    /**
     * Ambil daftar paket layanan dari Supabase.
     * Auth bearer akan menggunakan token session bila tersedia, fallback ke anon key.
     */
    public static void fetchServicePackages(Context context, PackagesCallback callback) {
        new AsyncTask<Void, Void, JSONArray>() {
            Exception ex;

            @Override
            protected JSONArray doInBackground(Void... voids) {
                try {
                    return fetchPackagesInternal(context, true);
                } catch (Exception primary) {
                    ex = primary;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(JSONArray result) {
                if (ex != null) {
                    callback.onError(ex.getMessage());
                } else {
                    callback.onSuccess(result != null ? result : new JSONArray());
                }
            }
        }.execute();
    }

    private static JSONArray fetchPackagesInternal(Context context, boolean preferSession) throws Exception {
        String endpoint = SUPABASE_URL + "/rest/v1/service_packages"
                + "?select=*"
                + "&order=id.asc";

        String token = preferSession ? resolveBearerToken(context) : SUPABASE_KEY;
        boolean usingAnon = token == null || token.isEmpty() || token.equals(SUPABASE_KEY);

        HttpURLConnection connHttp = null;
        try {
            URL url = new URL(endpoint);
            connHttp = (HttpURLConnection) url.openConnection();
            connHttp.setRequestMethod("GET");
            connHttp.setRequestProperty("apikey", SUPABASE_KEY);
            connHttp.setRequestProperty("Authorization", "Bearer " + (token == null ? SUPABASE_KEY : token));
            connHttp.setRequestProperty("Accept", "application/json");

            int code = connHttp.getResponseCode();
            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(connHttp.getInputStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connHttp.getErrorStream()));
            }

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            if (code >= 200 && code < 300) {
                return new JSONArray(sb.toString());
            }

            // Jika token session tidak valid (401/403), fallback ke anon key satu kali
            if (!usingAnon && (code == 401 || code == 403 || code == 400)) {
                return fetchPackagesInternal(context, false);
            }

            throw new RuntimeException("HTTP " + code + ": " + sb);
        } finally {
            if (connHttp != null) {
                connHttp.disconnect();
            }
        }
    }

    private static String resolveBearerToken(Context context) {
        TokenStorage storage = new TokenStorage(context.getApplicationContext());
        AuthSession session = storage.getSession();
        if (session != null && !session.isExpired() && session.getAccessToken() != null && !session.getAccessToken().isEmpty()) {
            return session.getAccessToken();
        }
        return SUPABASE_KEY;
    }

    public static void signOut(Context context) {
        new TokenStorage(context.getApplicationContext()).clear();
    }
}
