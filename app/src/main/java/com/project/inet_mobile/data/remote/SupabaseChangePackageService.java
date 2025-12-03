package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.ChangePackageRequest;
import com.project.inet_mobile.data.remote.dto.SupabaseChangePackageResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * Retrofit service untuk Supabase Edge Function: change-package
 * Endpoint: POST /functions/v1/change-package
 */
public interface SupabaseChangePackageService {

    /**
     * Submit change package request via Supabase Edge Function
     * Authorization header will be added automatically by AuthInterceptor
     *
     * @param request Request body containing package_id and notes
     * @return Response from Edge Function wrapping RPC result
     */
    @Headers({
        "Accept: application/json",
        "Content-Type: application/json"
    })
    @POST("functions/v1/change-package")
    Call<SupabaseChangePackageResponse> submitChangePackage(
        @Body ChangePackageRequest request
    );
}
