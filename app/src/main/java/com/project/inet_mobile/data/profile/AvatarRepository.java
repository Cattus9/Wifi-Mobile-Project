package com.project.inet_mobile.data.profile;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.auth.User;
import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.remote.SupabaseStorageService;
import com.project.inet_mobile.data.remote.SupabaseUserService;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.util.conn;
import com.google.gson.JsonNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Handles avatar upload to Supabase Storage and updates users.avatar_url.
 */
public class AvatarRepository {

    private static final String TAG = "AvatarRepository";
    private static final long MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB
    private static final String BUCKET = "avatars";

    private final Context appContext;
    private final SupabaseStorageService storageService;
    private final SupabaseUserService userService;
    private final TokenStorage tokenStorage;

    public interface CallbackResult {
        void onSuccess(String publicUrl);
        void onError(String message);
    }

    public AvatarRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.storageService = SupabaseApiClient.getSupabaseStorageService();
        this.userService = SupabaseApiClient.getSupabaseUserService();
        this.tokenStorage = new TokenStorage(appContext);
    }

    public void deleteAvatar(long userId, @NonNull CallbackResult callback) {
        AuthSession session = tokenStorage.getSession();
        if (session == null || session.isExpired() || session.getAccessToken() == null || session.getAccessToken().isEmpty()) {
            callback.onError("User not logged in.");
            return;
        }
        String apiKey = conn.getSupabaseKey();
        String authHeader = "Bearer " + session.getAccessToken();
        long start = System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        // Use empty string to clear avatar if null is ignored by PostgREST policies.
        payload.put("avatar_url", "");
        Call<List<User>> call = userService.updateAvatarUrl(
                apiKey,
                authHeader,
                "return=representation",
                "eq." + userId,
                payload
        );
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                long elapsed = System.currentTimeMillis() - start;
                if (response.isSuccessful()) {
                    Log.d(TAG, "delete avatar_url success latencyMs=" + elapsed);
                    callback.onSuccess("");
                } else {
                    String msg = "Gagal menghapus avatar: " + response.code();
                    String body = "";
                    try { body = response.errorBody() != null ? response.errorBody().string() : ""; } catch (Exception ignore) {}
                    Log.e(TAG, msg + " latencyMs=" + elapsed + " body=" + body);
                    callback.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                String msg = "Error hapus avatar: " + t.getMessage();
                Log.e(TAG, msg + " latencyMs=" + elapsed);
                callback.onError(msg);
            }
        });
    }

    public void uploadAvatar(long customerId, long userId, @NonNull Uri imageUri, @NonNull CallbackResult callback) {
        AuthSession session = tokenStorage.getSession();
        if (session == null || session.isExpired() || session.getAccessToken() == null || session.getAccessToken().isEmpty()) {
            callback.onError("User not logged in.");
            return;
        }

        try {
            byte[] bytes = readBytes(imageUri);
            if (bytes.length == 0) {
                callback.onError("File kosong.");
                return;
            }
            if (bytes.length > MAX_SIZE_BYTES) {
                callback.onError("Ukuran file melebihi 2MB.");
                return;
            }

            String mime = detectMime(imageUri);
            if (!isAllowedMime(mime)) {
                callback.onError("Format gambar tidak didukung. Gunakan JPG/PNG/WebP.");
                return;
            }

            String ext = mapExtension(mime);
            String objectPath = "customer_" + customerId + "." + ext;
            String authHeader = "Bearer " + session.getAccessToken();
            String apiKey = conn.getSupabaseKey();

            RequestBody body = RequestBody.create(bytes, MediaType.parse(mime));
            long start = System.currentTimeMillis();
            storageService.uploadObject(apiKey, authHeader, mime, BUCKET, objectPath, body)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            long elapsed = System.currentTimeMillis() - start;
                            if (!response.isSuccessful()) {
                                String msg = "Upload gagal: " + response.code();
                                Log.e(TAG, msg + " latencyMs=" + elapsed);
                                callback.onError(msg);
                                return;
                            }
                            String publicUrl = buildPublicUrl(objectPath);
                            updateAvatarUrl(userId, publicUrl, authHeader, apiKey, elapsed, callback);
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            long elapsed = System.currentTimeMillis() - start;
                            String msg = "Upload error: " + t.getMessage();
                            Log.e(TAG, msg + " latencyMs=" + elapsed);
                            callback.onError(msg);
                        }
                    });
        } catch (Exception ex) {
            callback.onError("Gagal membaca file: " + ex.getMessage());
        }
    }

    private void updateAvatarUrl(long userId, String publicUrl, String authHeader, String apiKey, long uploadLatencyMs, CallbackResult callback) {
        long start = System.currentTimeMillis();
        Map<String, Object> payload = new HashMap<>();
        payload.put("avatar_url", publicUrl);
        Call<List<User>> call = userService.updateAvatarUrl(
                apiKey,
                authHeader,
                "return=representation",
                "eq." + userId,
                payload
        );
        call.enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                long elapsed = System.currentTimeMillis() - start;
                if (response.isSuccessful()) {
                    Log.d(TAG, "update avatar_url success uploadMs=" + uploadLatencyMs + " updateMs=" + elapsed);
                    callback.onSuccess(publicUrl);
                } else {
                    String msg = "Gagal update profil: " + response.code();
                    String body = "";
                    try { body = response.errorBody() != null ? response.errorBody().string() : ""; } catch (Exception ignore) {}
                    Log.e(TAG, msg + " uploadMs=" + uploadLatencyMs + " updateMs=" + elapsed + " body=" + body);
                    callback.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                String msg = "Error update profil: " + t.getMessage();
                Log.e(TAG, msg + " uploadMs=" + uploadLatencyMs + " updateMs=" + elapsed);
                callback.onError(msg);
            }
        });
    }

    private String buildPublicUrl(String objectPath) {
        return conn.getSupabaseUrl() + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;
    }

    private byte[] readBytes(Uri uri) throws Exception {
        try (InputStream is = appContext.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (is == null) {
                return new byte[0];
            }
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    private String detectMime(Uri uri) {
        String mime = appContext.getContentResolver().getType(uri);
        if (mime == null) {
            String path = uri.getPath();
            if (path != null) {
                mime = URLConnection.guessContentTypeFromName(path);
            }
        }
        return mime != null ? mime : "";
    }

    private boolean isAllowedMime(@Nullable String mime) {
        if (mime == null) return false;
        String lower = mime.toLowerCase();
        return lower.equals("image/jpeg") || lower.equals("image/jpg") || lower.equals("image/png") || lower.equals("image/webp");
    }

    private String mapExtension(String mime) {
        String lower = mime.toLowerCase();
        if (lower.contains("png")) return "png";
        if (lower.contains("webp")) return "webp";
        return "jpg";
    }

}
