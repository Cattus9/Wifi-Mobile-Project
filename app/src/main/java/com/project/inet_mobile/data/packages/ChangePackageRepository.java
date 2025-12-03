package com.project.inet_mobile.data.packages;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.project.inet_mobile.data.remote.ChangePackageService;
import com.project.inet_mobile.data.remote.PaymentApiClient;
import com.project.inet_mobile.data.remote.dto.ChangePackageRequest;
import com.project.inet_mobile.data.remote.dto.ChangePackageStatusResponse;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.data.auth.AuthSession;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePackageRepository {

    private static final String TAG = "ChangePackageRepo";
    private final ChangePackageService service;
    private final TokenStorage tokenStorage;

    public interface SubmitCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface StatusCallback {
        void onSuccess(ChangePackageStatusResponse status);
        void onError(String message);
    }

    public ChangePackageRepository(@NonNull Context context) {
        PaymentApiClient client = new PaymentApiClient(context);
        this.service = client.getChangePackageService();
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    private String bearer() {
        AuthSession session = tokenStorage.getSession();
        if (session == null || session.isExpired() || session.getAccessToken() == null || session.getAccessToken().isEmpty()) {
            return null;
        }
        return "Bearer " + session.getAccessToken();
    }

    public void submitChangePackage(long targetPackageId, String note, SubmitCallback callback) {
        String auth = bearer();
        if (auth == null) {
            callback.onError("User not logged in.");
            return;
        }
        ChangePackageRequest body = new ChangePackageRequest(targetPackageId, note);
        long start = System.currentTimeMillis();
        service.submitChangePackage(auth, body).enqueue(new Callback<ChangePackageStatusResponse>() {
            @Override
            public void onResponse(Call<ChangePackageStatusResponse> call, Response<ChangePackageStatusResponse> response) {
                long elapsed = System.currentTimeMillis() - start;
                if (response.isSuccessful()) {
                    Log.d(TAG, "submit change-package success latencyMs=" + elapsed);
                    callback.onSuccess();
                } else {
                    String msg = "Gagal mengajukan ubah paket: " + response.code();
                    callback.onError(msg);
                    Log.e(TAG, msg + " latencyMs=" + elapsed);
                }
            }

            @Override
            public void onFailure(Call<ChangePackageStatusResponse> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                String msg = "Jaringan bermasalah: " + t.getMessage();
                callback.onError(msg);
                Log.e(TAG, msg + " latencyMs=" + elapsed);
            }
        });
    }

    public void getActiveChangeStatus(StatusCallback callback) {
        String auth = bearer();
        if (auth == null) {
            callback.onError("User not logged in.");
            return;
        }
        long start = System.currentTimeMillis();
        service.getActiveChangePackage(auth).enqueue(new Callback<ChangePackageStatusResponse>() {
            @Override
            public void onResponse(Call<ChangePackageStatusResponse> call, Response<ChangePackageStatusResponse> response) {
                long elapsed = System.currentTimeMillis() - start;
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "get change-package status success latencyMs=" + elapsed);
                    callback.onSuccess(response.body());
                } else if (response.code() == 404) {
                    // No active ticket
                    callback.onSuccess(null);
                } else {
                    String msg = "Gagal memuat status ubah paket: " + response.code();
                    callback.onError(msg);
                    Log.e(TAG, msg + " latencyMs=" + elapsed);
                }
            }

            @Override
            public void onFailure(Call<ChangePackageStatusResponse> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - start;
                String msg = "Jaringan bermasalah: " + t.getMessage();
                callback.onError(msg);
                Log.e(TAG, msg + " latencyMs=" + elapsed);
            }
        });
    }
}
