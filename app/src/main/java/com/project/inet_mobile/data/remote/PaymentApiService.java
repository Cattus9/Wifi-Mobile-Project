package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.ApiResponse;
import com.project.inet_mobile.data.remote.dto.CheckoutRequest;
import com.project.inet_mobile.data.remote.dto.CheckoutResponseData;
import com.project.inet_mobile.data.remote.dto.InvoiceDetailResponseData;
import com.project.inet_mobile.data.remote.dto.InvoiceListResponseData;
import com.project.inet_mobile.data.remote.dto.PaymentStatusResponseData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface PaymentApiService {

    @GET("invoices/index.php")
    Call<ApiResponse<InvoiceListResponseData>> getInvoices(
            @Query("limit") Integer limit,
            @Query("offset") Integer offset,
            @Query("status") String status
    );

    @GET("invoices/detail.php")
    Call<ApiResponse<InvoiceDetailResponseData>> getInvoiceDetail(
            @Query("id") long invoiceId
    );

    @POST("payments/checkout.php")
    Call<ApiResponse<CheckoutResponseData>> checkout(@Body CheckoutRequest body);

    @GET("payments/status.php")
    Call<ApiResponse<PaymentStatusResponseData>> getPaymentStatus(
            @Query("invoice_id") Long invoiceId,
            @Query("payment_id") Long paymentId
    );
}
