package com.project.inet_mobile.ui.ticket;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.project.inet_mobile.R;
import java.net.URLEncoder;

public class CreateTicketFragment extends Fragment {

    private CreateTicketViewModel viewModel;

    // Views
    private Toolbar toolbar;
    private AutoCompleteTextView spinnerCategory;
    private TextInputEditText editTextSubject;
    private TextInputEditText editTextDescription;
    private Button buttonSubmit;
    private ProgressBar progressBar;

    // Define categories, mapping UI string to backend enum value
    private static final String CATEGORY_CONNECTION = "koneksi";
    private static final String CATEGORY_BILLING = "tagihan";
    private static final String CATEGORY_INSTALLATION = "instalasi";
    private static final String CATEGORY_INFO_REQUEST = "permintaan_info";
    private static final String CATEGORY_COMPLAINT = "saran_komplain";
    
    private String selectedCategory = CATEGORY_INFO_REQUEST; // Default value

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_ticket, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        toolbar = view.findViewById(R.id.toolbar);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        editTextSubject = view.findViewById(R.id.edit_text_subject);
        editTextDescription = view.findViewById(R.id.edit_text_description);
        buttonSubmit = view.findViewById(R.id.button_submit);
        progressBar = view.findViewById(R.id.progress_bar);

        viewModel = new ViewModelProvider(this).get(CreateTicketViewModel.class);

        setupToolbar();
        setupCategorySpinner();
        setupClickListeners();
        observeViewModel();
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupCategorySpinner() {
        // As per documentation, these are the customer-facing categories
        String[] categories = new String[]{
                "Permintaan Informasi",
                "Saran & Komplain",
                "Masalah Koneksi",
                "Masalah Tagihan",
                "Masalah Instalasi"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setText(categories[0], false); // Set default selection

        spinnerCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            switch (selection) {
                case "Saran & Komplain":
                    selectedCategory = CATEGORY_COMPLAINT;
                    break;
                case "Masalah Koneksi":
                    selectedCategory = CATEGORY_CONNECTION;
                    break;
                case "Masalah Tagihan":
                    selectedCategory = CATEGORY_BILLING;
                    break;
                case "Masalah Instalasi":
                    selectedCategory = CATEGORY_INSTALLATION;
                    break;
                case "Permintaan Informasi":
                default:
                    selectedCategory = CATEGORY_INFO_REQUEST;
                    break;
            }
        });
    }

    private void setupClickListeners() {
        buttonSubmit.setOnClickListener(v -> {
            String subject = editTextSubject.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();
            viewModel.createTicket(selectedCategory, subject, description);
        });
    }

    private void observeViewModel() {
        viewModel.creationState.observe(getViewLifecycleOwner(), state -> {
            switch (state.status) {
                case LOADING:
                    setLoading(true);
                    break;
                case SUCCESS:
                    setLoading(false);
                    Toast.makeText(getContext(), "Laporan berhasil dikirim.", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                    break;
                case ERROR:
                    setLoading(false);
                    Toast.makeText(getContext(), "Error: " + state.errorMessage, Toast.LENGTH_LONG).show();
                    break;
                case IDLE:
                    setLoading(false);
                    break;
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        buttonSubmit.setEnabled(!isLoading);
        spinnerCategory.setEnabled(!isLoading);
        editTextSubject.setEnabled(!isLoading);
        editTextDescription.setEnabled(!isLoading);
    }
}
