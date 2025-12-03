package com.project.inet_mobile.data.remote;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Minimal Supabase Storage REST API for direct object upload.
 */
public interface SupabaseStorageService {

    @Headers({
            "Accept: application/json"
    })
    @PUT("storage/v1/object/{bucket}/{path}")
    Call<ResponseBody> uploadObject(
            @Header("apikey") String apiKey,
            @Header("Authorization") String authHeader,
            @Header("Content-Type") String contentType,
            @Path("bucket") String bucket,
            @Path(value = "path", encoded = true) String objectPath,
            @Body RequestBody body
    );
}
