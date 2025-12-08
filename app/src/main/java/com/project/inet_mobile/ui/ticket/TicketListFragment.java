package com.project.inet_mobile.ui.ticket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.project.inet_mobile.R;
import com.project.inet_mobile.data.remote.dto.ticket.TicketDto;

import java.net.URLEncoder;

public class TicketListFragment extends Fragment implements TicketAdapter.OnTicketClickListener {

    private TicketListViewModel listViewModel;
    private CreateTicketViewModel createViewModel; // For emergency button
    private TicketAdapter ticketAdapter;

    // Views from fragment_bantuanv2.xml
    private RecyclerView rvTickets;
    private LinearLayout emptyState;
    private FloatingActionButton fabCreateTicket;
    private Button btnEmergency;
    private ChipGroup chipGroupFilter;
        private View loadingOverlay; // For loading state
        private Toolbar toolbar;
        private boolean isInitialLoad = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the new layout
        return inflater.inflate(R.layout.fragment_bantuanv2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModels
        listViewModel = new ViewModelProvider(this).get(TicketListViewModel.class);
        createViewModel = new ViewModelProvider(this).get(CreateTicketViewModel.class); // For emergency button

        // Initialize views using findViewById
        rvTickets = view.findViewById(R.id.rvTickets);
        emptyState = view.findViewById(R.id.emptyState);
        fabCreateTicket = view.findViewById(R.id.fabCreateTicket);
        btnEmergency = view.findViewById(R.id.btnEmergency);
        chipGroupFilter = view.findViewById(R.id.chipGroupFilter);
        loadingOverlay = view.findViewById(R.id.progressBar); // Corrected to use the actual ProgressBar ID
        toolbar = view.findViewById(R.id.toolbar);

        // Setup UI components and observers
        setupToolbar();
        setupRecyclerView();
        setupFab();
        setupEmergencyButton();
        setupFilterChips();
        observeListViewModel();
        observeCreateViewModel();

        // Perform initial load
        if (isInitialLoad) {
            listViewModel.fetchTickets();
            isInitialLoad = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the list only if it's not the initial load, to catch updates after creating a ticket
        if (!isInitialLoad) {
            listViewModel.fetchTickets();
        }
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupRecyclerView() {
        ticketAdapter = new TicketAdapter(this);
        rvTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTickets.setAdapter(ticketAdapter);
    }

    private void setupFab() {
        fabCreateTicket.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.dashboardFragmentContainer, new CreateTicketFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void setupEmergencyButton() {
        btnEmergency.setOnClickListener(v -> {
            // Use the createViewModel to handle the urgent ticket creation
            createViewModel.createTicket("mendesak", "Laporan Darurat Pengguna", "Pengguna menekan tombol darurat.");
        });
    }

    private void setupFilterChips() {
        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) {
                listViewModel.setFilterAndFetch(null);
            } else if (checkedId == R.id.chipOpen) {
                listViewModel.setFilterAndFetch("open");
            } else if (checkedId == R.id.chipInProgress) {
                listViewModel.setFilterAndFetch("in_progress");
            } else if (checkedId == R.id.chipClosed) {
                listViewModel.setFilterAndFetch("closed");
            }
        });
    }

    private void observeListViewModel() {
        listViewModel.ticketsState.observe(getViewLifecycleOwner(), state -> {
            boolean isLoading = state.status == TicketListViewModel.ListStatus.LOADING;
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            switch (state.status) {
                case SUCCESS:
                    emptyState.setVisibility(View.GONE);
                    rvTickets.setVisibility(View.VISIBLE);
                    ticketAdapter.submitList(state.tickets);
                    break;
                case EMPTY:
                    emptyState.setVisibility(View.VISIBLE);
                    rvTickets.setVisibility(View.GONE);
                    break;
                case ERROR:
                    emptyState.setVisibility(View.VISIBLE);
                    rvTickets.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error: " + state.errorMessage, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void observeCreateViewModel() {
        // This observer only cares about the result of the emergency button click
        createViewModel.creationState.observe(getViewLifecycleOwner(), state -> {
            if (state.status == CreateTicketViewModel.CreationStatus.LOADING) {
                loadingOverlay.setVisibility(View.VISIBLE);
            } else {
                loadingOverlay.setVisibility(View.GONE);
            }

            if (state.status == CreateTicketViewModel.CreationStatus.SUCCESS) {
                Toast.makeText(getContext(), "Tiket darurat dibuat. Membuka WhatsApp...", Toast.LENGTH_SHORT).show();
                if (state.response != null) {
                    openWhatsApp(state.response.getWhatsappNumber());
                }
            } else if (state.status == CreateTicketViewModel.CreationStatus.ERROR) {
                Toast.makeText(getContext(), "Error: " + state.errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onTicketClick(TicketDto ticket) {
        if (getActivity() != null) {
            TicketDetailFragment detailFragment = TicketDetailFragment.newInstance(ticket.getId());
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.dashboardFragmentContainer, detailFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    private void openWhatsApp(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "Nomor WhatsApp tidak tersedia.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber + "&text=" + URLEncoder.encode("Halo, saya butuh bantuan darurat.", "UTF-8");
            sendIntent.setData(Uri.parse(url));
            startActivity(sendIntent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "WhatsApp tidak terinstall.", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rvTickets != null) {
            rvTickets.setAdapter(null);
        }
    }
}
