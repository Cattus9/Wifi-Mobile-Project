package com.project.inet_mobile.data.remote;

import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketMessageRequest;
import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketRequest;
import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketResponse;
import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;
import com.project.inet_mobile.data.remote.dto.ticket.TicketMessageDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit service (Java version) for the Ticketing feature.
 * Targets Supabase PostgREST and Edge Functions.
 */
public interface SupabaseTicketService {

    @GET("rest/v1/tickets")
    Call<List<TicketDto>> getTickets(
            @Query("select") String select,
            @Query("order") String order,
            @Query("status") String statusFilter // e.g., "eq.open"
    );

    @GET("rest/v1/tickets")
    Call<List<TicketDto>> getTicketById(
            @Query("id") String ticketId, // e.g., "eq.123"
            @Query("select") String select
    );

    @GET("rest/v1/ticket_messages")
    Call<List<TicketMessageDto>> getTicketMessages(
            @Query("ticket_id") String ticketId, // e.g., "eq.123"
            @Query("select") String select,
            @Query("order") String order
    );

    @POST("functions/v1/create-ticket")
    Call<CreateTicketResponse> createTicket(@Body CreateTicketRequest body);

    @POST("functions/v1/create-ticket-message")
    Call<TicketMessageDto> createTicketMessage(@Body CreateTicketMessageRequest body);
}
