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

    // Service instances
    private static SupabaseUserService supabaseUserService;
    private static SupabaseStorageService supabaseStorageService;
    private static SupabasePackagesService supabasePackagesService;
    private static SupabaseChangePackageService supabaseChangePackageService;
    private static SupabaseDashboardService supabaseDashboardService;
    private static com.project.inet_mobile.data.packages.CurrentPackageRepository.CurrentPackageService currentPackageService;
    private static SupabaseTicketService supabaseTicketService; // The new service

    public static void init(Context context) {
        if (retrofit == null) {
            loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            TokenStorage tokenStorage = new TokenStorage(context.getApplicationContext());
            String supabaseAnonKey = conn.getSupabaseKey();
            SupabaseAuthService authService = new SupabaseAuthService(conn.getSupabaseUrl(), supabaseAnonKey);

            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(tokenStorage, authService, supabaseAnonKey))
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(conn.getSupabaseUrl() + "/")
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

    public static SupabaseDashboardService getSupabaseDashboardService() {
        if (supabaseDashboardService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            supabaseDashboardService = retrofit.create(SupabaseDashboardService.class);
        }
        return supabaseDashboardService;
    }
    
    // Getter for the new Ticket Service
    public static SupabaseTicketService getSupabaseTicketService() {
        if (supabaseTicketService == null) {
            if (retrofit == null) {
                throw new IllegalStateException("SupabaseApiClient belum diinisialisasi. Panggil init() terlebih dahulu.");
            }
            supabaseTicketService = retrofit.create(SupabaseTicketService.class);
        }
        return supabaseTicketService;
    }
}
