package com.project.inet_mobile.data.remote;

import android.content.Context;

import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.util.conn;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds Retrofit client for payment endpoints.
 */
public class PaymentApiClient {

    private final PaymentApiService apiService;
    private final ChangePackageService changePackageService;

    public PaymentApiClient(Context context) {
        TokenStorage storage = new TokenStorage(context.getApplicationContext());
        SupabaseAuthService authService = new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey());

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // Note: PHP backend doesn't need apikey header, so pass null
                .addInterceptor(new AuthInterceptor(storage, authService, null))
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.API_V1)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(PaymentApiService.class);
        changePackageService = retrofit.create(ChangePackageService.class);
    }

    public PaymentApiService getApiService() {
        return apiService;
    }

    public ChangePackageService getChangePackageService() {
        return changePackageService;
    }
}
