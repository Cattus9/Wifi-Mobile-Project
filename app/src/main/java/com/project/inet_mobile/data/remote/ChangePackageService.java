package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.ChangePackageRequest;
import com.project.inet_mobile.data.remote.dto.ChangePackageStatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ChangePackageService {

    /**
     * Submit a change package request.
     * Endpoint: POST /api/v1/customer/change-package.php
     */
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @POST("customer/change-package.php")
    Call<ChangePackageStatusResponse> submitChangePackage(
            @Header("Authorization") String authHeader,
            @Body ChangePackageRequest body
    );

    /**
     * Get active change package status.
     * Endpoint: GET /api/v1/customer/change-package.php
     */
    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @GET("customer/change-package.php")
    Call<ChangePackageStatusResponse> getActiveChangePackage(
            @Header("Authorization") String authHeader
    );
}
