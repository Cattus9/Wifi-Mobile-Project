package com.project.inet_mobile.data.payment;

import android.content.Context;

import androidx.annotation.Nullable;

import com.project.inet_mobile.data.remote.PaymentApiClient;
import com.project.inet_mobile.data.remote.PaymentApiService;
import com.project.inet_mobile.data.remote.dto.ApiResponse;
import com.project.inet_mobile.data.remote.dto.CheckoutRequest;
import com.project.inet_mobile.data.remote.dto.CheckoutResponseData;
import com.project.inet_mobile.data.remote.dto.InvoiceDetailResponseData;
import com.project.inet_mobile.data.remote.dto.InvoiceListResponseData;
import com.project.inet_mobile.data.remote.dto.PaymentStatusResponseData;

import retrofit2.Call;

/**
 * Thin wrapper around the Retrofit service so fragments/viewmodels
 * can request payments without dealing with Retrofit setup details.
 */
public class PaymentRemoteDataSource {

    private final PaymentApiService apiService;

    public PaymentRemoteDataSource(Context context) {
        this.apiService = new PaymentApiClient(context).getApiService();
    }

    public Call<ApiResponse<CheckoutResponseData>> checkout(long invoiceId,
                                                            String preferredChannel,
                                                            @Nullable String returnUrl) {
        CheckoutRequest body = new CheckoutRequest(invoiceId, preferredChannel, returnUrl);
        return apiService.checkout(body);
    }

    public Call<ApiResponse<PaymentStatusResponseData>> getPaymentStatus(@Nullable Long invoiceId,
                                                                         @Nullable Long paymentId) {
        return apiService.getPaymentStatus(invoiceId, paymentId);
    }

    public Call<ApiResponse<InvoiceListResponseData>> getInvoices(@Nullable Integer limit,
                                                                  @Nullable Integer offset,
                                                                  @Nullable String status) {
        return apiService.getInvoices(limit, offset, status);
    }

    public Call<ApiResponse<InvoiceDetailResponseData>> getInvoiceDetail(long invoiceId) {
        return apiService.getInvoiceDetail(invoiceId);
    }
}
