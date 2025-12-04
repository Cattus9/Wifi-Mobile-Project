package com.project.inet_mobile.data.packages;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.project.inet_mobile.Paket;
import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.remote.SupabasePackagesService;
import com.project.inet_mobile.data.remote.dto.ServicePackageDto;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.util.conn;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository untuk mengambil daftar paket layanan dari Supabase menggunakan Retrofit + AuthInterceptor.
 */
public class ServicePackagesRepository {

    private static final String TAG = "ServicePackagesRepo";
    private final SupabasePackagesService service;
    private final TokenStorage tokenStorage;

    public ServicePackagesRepository(Context context) {
        this.service = SupabaseApiClient.getSupabasePackagesService();
        this.tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    public interface PackagesCallback {
        void onSuccess(List<Paket> paketList);
        void onError(String message);
    }

    /**
     * Fetch packages with authentication check (for authenticated users)
     */
    public void fetchPackages(@NonNull PackagesCallback callback) {
        long start = System.currentTimeMillis();

        AuthSession session = tokenStorage.getSession();
        if (session == null || session.isExpired() || session.getAccessToken() == null || session.getAccessToken().isEmpty()) {
            Log.w(TAG, "fetchPackages: session not valid");
            callback.onError("Session tidak valid. Silakan login kembali.");
            return;
        }

        fetchPackagesInternal(callback, start);
    }

    /**
     * Fetch packages without authentication (for anonymous users during registration)
     */
    public void fetchPackagesAnonymous(@NonNull PackagesCallback callback) {
        long start = System.currentTimeMillis();
        Log.d(TAG, "fetchPackagesAnonymous: fetching packages for registration");
        fetchPackagesInternal(callback, start);
    }

    private void fetchPackagesInternal(@NonNull PackagesCallback callback, long startTime) {
        String apikey = conn.getSupabaseKey();
        // Only select columns that exist in service_packages table
        String select = "id,name,description,speed,price,is_active";
        // Filter only active packages
        Call<List<ServicePackageDto>> call = service.getPackages(apikey, select, "id.asc", "eq.true");
        call.enqueue(new Callback<List<ServicePackageDto>>() {
            @Override
            public void onResponse(Call<List<ServicePackageDto>> call, Response<List<ServicePackageDto>> response) {
                long elapsed = System.currentTimeMillis() - startTime;
                if (response.isSuccessful() && response.body() != null) {
                    List<Paket> pakets = mapToPaket(response.body());
                    Log.d(TAG, "fetchPackages success size=" + pakets.size() + " latencyMs=" + elapsed);
                    callback.onSuccess(pakets);
                } else {
                    String msg = "Failed to load packages: " + response.code();
                    Log.e(TAG, msg + " latencyMs=" + elapsed);
                    callback.onError(msg);
                }
            }

            @Override
            public void onFailure(Call<List<ServicePackageDto>> call, Throwable t) {
                long elapsed = System.currentTimeMillis() - startTime;
                String msg = "Network error: " + t.getMessage();
                Log.e(TAG, msg + " latencyMs=" + elapsed);
                callback.onError(msg);
            }
        });
    }

    private List<Paket> mapToPaket(List<ServicePackageDto> dtos) {
        List<Paket> out = new ArrayList<>();
        if (dtos == null) return out;

        for (ServicePackageDto dto : dtos) {
            String name = ns(dto.name);
            String desc = ns(dto.description);
            String speed = ns(dto.speed);
            double price = dto.price != null ? dto.price : 0.0;
            String duration = formatDuration(dto.duration);

            Paket paket = new Paket(dto.id, name, desc, speed, price, true, duration);
            paket.setPopuler(dto.isPopular != null && dto.isPopular);
            if (dto.quota != null && !dto.quota.isEmpty()) {
                paket.setQuota(dto.quota);
            }
            if (dto.phone != null && !dto.phone.isEmpty()) {
                paket.setPhone(dto.phone);
            }
            if (dto.originalPrice != null && !dto.originalPrice.isEmpty()) {
                paket.setHargaAsli(formatCurrency(dto.originalPrice));
            }
            out.add(paket);
        }
        return out;
    }

    private String ns(String s) {
        return s == null ? "" : s;
    }

    private String formatDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return "";
        }
        try {
            int days = Integer.parseInt(duration);
            if (days == 1) return "1 Day";
            return days + " Days";
        } catch (NumberFormatException e) {
            return duration;
        }
    }

    private String formatCurrency(String raw) {
        try {
            double v = Double.parseDouble(raw);
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            return nf.format(v).replace(",00", "");
        } catch (Exception ex) {
            return raw == null ? "" : raw;
        }
    }
}
