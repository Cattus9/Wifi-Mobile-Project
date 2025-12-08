package com.project.inet_mobile.ui.ticket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.inet_mobile.R;
import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;

public class TicketDetailFragment extends Fragment {

    private static final String ARG_TICKET_ID = "ticket_id";

    private TicketDetailViewModel viewModel;
    private TicketMessageAdapter messageAdapter;
    private long ticketId;

    // View fields
    private Toolbar toolbar;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ProgressBar progressBar;
    private TextView textViewEmptyChat;

    public static TicketDetailFragment newInstance(long ticketId) {
        TicketDetailFragment fragment = new TicketDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_TICKET_ID, ticketId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ticketId = getArguments().getLong(ARG_TICKET_ID);
        } else {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Error: Ticket ID tidak ditemukan.", Toast.LENGTH_SHORT).show();
                getActivity().getSupportFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ticket_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views with findViewById
        toolbar = view.findViewById(R.id.toolbar);
        recyclerViewMessages = view.findViewById(R.id.recycler_view_messages);
        editTextMessage = view.findViewById(R.id.edit_text_message);
        buttonSend = view.findViewById(R.id.button_send);
        progressBar = view.findViewById(R.id.progress_bar);
        textViewEmptyChat = view.findViewById(R.id.text_view_empty_chat);
        
        viewModel = new ViewModelProvider(this).get(TicketDetailViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupSendButton();
        observeViewModel();

        if (savedInstanceState == null) {
            // Fetch both details and messages for the initial load
            viewModel.fetchTicketDetails(ticketId);
            viewModel.fetchTicketMessages(ticketId);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        viewModel.startPolling(); // Start polling when fragment is active
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopPolling(); // Stop polling when fragment is not visible
    }
    
    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupRecyclerView() {
        messageAdapter = new TicketMessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }
    
    private void setupSendButton() {
        buttonSend.setOnClickListener(v -> {
            String message = editTextMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendNewMessage(ticketId, message);
            }
        });
    }

    private void observeViewModel() {
        // Observer for header details
        viewModel.ticketDetails.observe(getViewLifecycleOwner(), ticketDto -> {
            if (ticketDto != null) {
                toolbar.setTitle(ticketDto.getSubject());
                String details = "Tiket #" + ticketDto.getId() + " • Status: " + ticketDto.getStatus();
                toolbar.setSubtitle(details);
            }
        });

        // Observer for message list
        viewModel.messagesState.observe(getViewLifecycleOwner(), state -> {
            // Only show ProgressBar if it's not a background poll (i.e., initial load or explicit refresh)
            progressBar.setVisibility(state.status == TicketDetailViewModel.ListStatus.LOADING ? View.VISIBLE : View.GONE);
            textViewEmptyChat.setVisibility(state.status == TicketDetailViewModel.ListStatus.EMPTY ? View.VISIBLE : View.GONE);
            recyclerViewMessages.setVisibility(state.status == TicketDetailViewModel.ListStatus.SUCCESS ? View.VISIBLE : View.GONE);

            if (state.status == TicketDetailViewModel.ListStatus.SUCCESS) {
                messageAdapter.submitList(state.messages, () -> {
                    if (messageAdapter.getItemCount() > 0) {
                        recyclerViewMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                });
            } else if (state.status == TicketDetailViewModel.ListStatus.ERROR) {
                Toast.makeText(getContext(), "Error: " + state.errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observer for sending state
        viewModel.sendingState.observe(getViewLifecycleOwner(), state -> {
            buttonSend.setEnabled(state.status != TicketDetailViewModel.SendingStatus.SENDING);
            editTextMessage.setEnabled(state.status != TicketDetailViewModel.SendingStatus.SENDING);

            if (state.status == TicketDetailViewModel.SendingStatus.SUCCESS) {
                editTextMessage.setText("");
                viewModel.resetSendingState();
            } else if (state.status == TicketDetailViewModel.SendingStatus.ERROR) {
                Toast.makeText(getContext(), "Gagal mengirim: " + state.errorMessage, Toast.LENGTH_SHORT).show();
                viewModel.resetSendingState();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerViewMessages != null) {
            recyclerViewMessages.setAdapter(null);
        }
    }
}
