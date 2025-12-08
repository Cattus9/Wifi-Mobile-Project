package com.project.inet_mobile.ui.ticket;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;
import com.project.inet_mobile.data.ticket.TicketRepository;

import java.util.List;

public class TicketListViewModel extends AndroidViewModel {

    private final TicketRepository ticketRepository;
    private String currentStatusFilter = null; // To hold the current filter, e.g., "eq.open"

    public enum ListStatus {
        LOADING,
        SUCCESS,
        ERROR,
        EMPTY
    }

    public static class TicketsState {
        public final ListStatus status;
        public final List<TicketDto> tickets;
        public final String errorMessage;

        private TicketsState(ListStatus status, List<TicketDto> tickets, String errorMessage) {
            this.status = status;
            this.tickets = tickets;
            this.errorMessage = errorMessage;
        }

        public static TicketsState loading() {
            return new TicketsState(ListStatus.LOADING, null, null);
        }

        public static TicketsState success(List<TicketDto> tickets) {
            return new TicketsState(ListStatus.SUCCESS, tickets, null);
        }

        public static TicketsState error(String message) {
            return new TicketsState(ListStatus.ERROR, null, message);
        }
        
        public static TicketsState empty() {
            return new TicketsState(ListStatus.EMPTY, null, null);
        }
    }

    private final MutableLiveData<TicketsState> _ticketsState = new MutableLiveData<>(TicketsState.loading());
    public final LiveData<TicketsState> ticketsState = _ticketsState;

    public TicketListViewModel(@NonNull Application application) {
        super(application);
        this.ticketRepository = new TicketRepository(application.getApplicationContext());
        fetchTickets(); // Fetch on init
    }

    public void fetchTickets() {
        _ticketsState.setValue(TicketsState.loading());

        ticketRepository.getTickets(getApplication().getApplicationContext(), currentStatusFilter, new TicketRepository.GetTicketsCallback() {
            @Override
            public void onSuccess(List<TicketDto> tickets) {
                if (tickets == null || tickets.isEmpty()) {
                    _ticketsState.setValue(TicketsState.empty());
                } else {
                    _ticketsState.setValue(TicketsState.success(tickets));
                }
            }

            @Override
            public void onError(String message) {
                _ticketsState.setValue(TicketsState.error(message));
            }
        });
    }

    /**
     * Sets a new status filter and re-fetches the tickets.
     * @param status The new status to filter by (e.g., "open", "closed"), or null to clear the filter.
     */
    public void setFilterAndFetch(@Nullable String status) {
        if (status == null) {
            this.currentStatusFilter = null;
        } else {
            this.currentStatusFilter = "eq." + status;
        }
        fetchTickets();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ticketRepository.shutdown();
    }
}
