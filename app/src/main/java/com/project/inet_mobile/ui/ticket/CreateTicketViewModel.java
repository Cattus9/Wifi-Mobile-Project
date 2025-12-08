package com.project.inet_mobile.ui.ticket;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.project.inet_mobile.data.remote.dto.ticket.CreateTicketResponse;
import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;
import com.project.inet_mobile.data.ticket.TicketRepository;

public class CreateTicketViewModel extends AndroidViewModel {

    private final TicketRepository ticketRepository;

    public enum CreationStatus {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    public static class CreationState {
        public final CreationStatus status;
        public final CreateTicketResponse response; // Changed from TicketDto
        public final String errorMessage;

        private CreationState(CreationStatus status, CreateTicketResponse response, String errorMessage) {
            this.status = status;
            this.response = response;
            this.errorMessage = errorMessage;
        }

        public static CreationState idle() {
            return new CreationState(CreationStatus.IDLE, null, null);
        }

        public static CreationState loading() {
            return new CreationState(CreationStatus.LOADING, null, null);
        }

        public static CreationState success(CreateTicketResponse response) {
            return new CreationState(CreationStatus.SUCCESS, response, null);
        }

        public static CreationState error(String message) {
            return new CreationState(CreationStatus.ERROR, null, message);
        }
    }

    private final MutableLiveData<CreationState> _creationState = new MutableLiveData<>(CreationState.idle());
    public final LiveData<CreationState> creationState = _creationState;

    public CreateTicketViewModel(@NonNull Application application) {
        super(application);
        // Initialize repository here
        this.ticketRepository = new TicketRepository(application.getApplicationContext());
    }

    public void createTicket(String kategori, String subject, String description) {
        // Basic validation
        if (kategori == null || kategori.isEmpty() || subject == null || subject.isEmpty() || description == null || description.isEmpty()) {
            _creationState.setValue(CreationState.error("Kategori, subjek, dan deskripsi tidak boleh kosong."));
            return;
        }

        _creationState.setValue(CreationState.loading());

        ticketRepository.createTicket(getApplication().getApplicationContext(), kategori, subject, description, new TicketRepository.CreateTicketCallback() {
            @Override
            public void onSuccess(CreateTicketResponse response) {
                _creationState.setValue(CreationState.success(response));
            }

            @Override
            public void onError(String message) {
                _creationState.setValue(CreationState.error(message));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ticketRepository.shutdown();
    }
}
