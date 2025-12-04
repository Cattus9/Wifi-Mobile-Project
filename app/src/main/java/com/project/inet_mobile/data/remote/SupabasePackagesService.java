package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.ServicePackageDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface SupabasePackagesService {

    @Headers({
            "Accept: application/json",
            "Content-Type: application/json"
    })
    @GET("rest/v1/service_packages")
    Call<List<ServicePackageDto>> getPackages(
            @Header("apikey") String apiKey,
            @Query("select") String select,
            @Query("order") String order,
            @Query("is_active") String isActive
    );
}
