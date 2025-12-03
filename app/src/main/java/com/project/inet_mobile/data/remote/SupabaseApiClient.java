package com.project.inet_mobile.data.remote;

import android.content.Context;

import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.util.conn; // To get Supabase URL and Key

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseApiClient {

    private static Retrofit retrofit;
    private static OkHttpClient okHttpClient;
    private static HttpLoggingInterceptor loggingInterceptor;

    // Instance SupabaseUserService
    private static SupabaseUserService supabaseUserService;
    private static SupabaseStorageService supabaseStorageService;
    private static SupabasePackagesService supabasePackagesService;
    private static SupabaseChangePackageService supabaseChangePackageService;
    private static com.project.inet_mobile.data.packages.CurrentPackageRepository.CurrentPackageService currentPackageService;

    public static void init(Context context) {
        if (retrofit == null) {
            loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Instantiate TokenStorage and SupabaseAuthService
            TokenStorage tokenStorage = new TokenStorage(context.getApplicationContext());
            // SupabaseAuthService requires Supabase URL and Key
            SupabaseAuthService authService = new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey());

            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(tokenStorage, authService)) // Pass both dependencies
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(conn.getSupabaseUrl() + "/") // Supabase URL from conn.java
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
    }

    public static SupabaseUserService getSupabaseUserService() {
        if (supabaseUserService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            supabaseUserService = retrofit.create(SupabaseUserService.class);
        }
        return supabaseUserService;
    }

    public static SupabasePackagesService getSupabasePackagesService() {
        if (supabasePackagesService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            supabasePackagesService = retrofit.create(SupabasePackagesService.class);
        }
        return supabasePackagesService;
    }

    public static SupabaseStorageService getSupabaseStorageService() {
        if (supabaseStorageService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            supabaseStorageService = retrofit.create(SupabaseStorageService.class);
        }
        return supabaseStorageService;
    }

    public static SupabaseChangePackageService getSupabaseChangePackageService() {
        if (supabaseChangePackageService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            supabaseChangePackageService = retrofit.create(SupabaseChangePackageService.class);
        }
        return supabaseChangePackageService;
    }

    public static com.project.inet_mobile.data.packages.CurrentPackageRepository.CurrentPackageService getCurrentPackageService() {
        if (currentPackageService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            currentPackageService = retrofit.create(com.project.inet_mobile.data.packages.CurrentPackageRepository.CurrentPackageService.class);
        }
        return currentPackageService;
    }

    // Anda bisa menambahkan service Supabase lainnya di sini jika diperlukan
    // contoh: SupabaseStorageService, dll.
}
