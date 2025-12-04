package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.SupabaseDashboardInvoiceDto;
import com.project.inet_mobile.data.remote.dto.SupabaseDashboardUserDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit service interface for fetching dashboard data from Supabase PostgREST
 *
 * Uses AuthInterceptor to automatically inject Authorization and apikey headers
 */
public interface SupabaseDashboardService {

    /**
     * Get user data with customer and service package information
     *
     * Query example:
     * GET /rest/v1/users?auth_user_id=eq.{uuid}&select=customer_id,customers(id,name,status,service_package_id,service_packages(id,name,speed,quota))
     *
     * @param authUserId Filter by auth_user_id with eq. prefix (e.g., "eq.uuid-here")
     * @param select     Fields to select with nested joins
     * @return List of users (should contain only 1 item)
     */
    @GET("rest/v1/users")
    Call<List<SupabaseDashboardUserDto>> getUserDashboardData(
            @Query("auth_user_id") String authUserId,
            @Query("select") String select
    );

    /**
     * Get latest unpaid invoice for a customer
     *
     * Query example:
     * GET /rest/v1/invoices?customer_id=eq.{id}&status=in.(issued,overdue)&order=due_date.desc&limit=1
     *
     * Valid invoice_status enum values: draft, issued, overdue, paid, cancelled
     *
     * @param customerId Filter by customer_id with eq. prefix (e.g., "eq.123")
     * @param status     Filter by status with in. prefix (e.g., "in.(issued,overdue)")
     * @param order      Order by field (e.g., "due_date.desc")
     * @param limit      Limit results
     * @return List of invoices (should contain 0 or 1 item)
     */
    @GET("rest/v1/invoices")
    Call<List<SupabaseDashboardInvoiceDto>> getLatestInvoice(
            @Query("customer_id") String customerId,
            @Query("status") String status,
            @Query("order") String order,
            @Query("limit") int limit
    );
}
