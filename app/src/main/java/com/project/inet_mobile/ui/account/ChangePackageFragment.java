package com.project.inet_mobile.ui.account;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.project.inet_mobile.Paket;
import com.project.inet_mobile.R;
import com.project.inet_mobile.data.packages.ChangePackageRepository;
import com.project.inet_mobile.data.packages.ChangePackageSupabaseRepository;
import com.project.inet_mobile.data.packages.CurrentPackageRepository;
import com.project.inet_mobile.data.packages.ServicePackagesRepository;
import com.project.inet_mobile.databinding.FragmentChangePackageBinding;
import com.project.inet_mobile.data.remote.dto.ChangePackageStatusResponse;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * UI untuk pengajuan perubahan paket.
 *
 * FEATURE FLAG: USE_SUPABASE_BACKEND
 * - true: Gunakan Supabase Edge Function (new)
 * - false: Gunakan PHP Backend (old)
 */
public class ChangePackageFragment extends Fragment {

    private static final String TAG = "ChangePackageFragment";

    // ========================================
    // FEATURE FLAG: Switch between backends
    // ========================================
    private static final boolean USE_SUPABASE_BACKEND = true; // ← Change this to switch backends

    private FragmentChangePackageBinding binding;
    private ChangePackageAdapter adapter;
    private ServicePackagesRepository packagesRepository;
    private CurrentPackageRepository currentPackageRepo; // Get current package
    private ChangePackageRepository changeRepo; // For PHP backend
    private ChangePackageSupabaseRepository supabaseRepo; // For Supabase backend
    private Integer currentPackageId = null;
    private Long selectedPackageId = null;

    public ChangePackageFragment() {
        super(R.layout.fragment_change_package);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePackageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        packagesRepository = new ServicePackagesRepository(requireContext());
        currentPackageRepo = new CurrentPackageRepository(requireContext());

        // Initialize repository based on feature flag
        if (USE_SUPABASE_BACKEND) {
            Log.d(TAG, "Using Supabase backend for Change Package");
            supabaseRepo = new ChangePackageSupabaseRepository(requireContext());
        } else {
            Log.d(TAG, "Using PHP backend for Change Package");
            changeRepo = new ChangePackageRepository(requireContext());
        }

        setupViews();
        loadCurrentPackage(); // NEW: Load current package first
        loadStatus();
        loadPackages();
    }

    private void setupViews() {
        adapter = new ChangePackageAdapter(paket -> {
            selectedPackageId = (long) paket.getId();
            updateSubmitState();
        });
        binding.recyclerPackages.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPackages.setAdapter(adapter);

        binding.btnBackChangePackage.setOnClickListener(v -> requireActivity().onBackPressed());
        binding.btnSubmitChange.setOnClickListener(v -> submitChange());
    }

    private void setLoading(boolean loading) {
        binding.progressChangePackage.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSubmitChange.setEnabled(!loading);
        binding.scrollChangePackage.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    private void loadCurrentPackage() {
        Log.d(TAG, "Loading current package ID...");
        currentPackageRepo.getCurrentPackageId(new CurrentPackageRepository.CurrentPackageCallback() {
            @Override
            public void onSuccess(Integer packageId) {
                if (!isAdded()) return;
                currentPackageId = packageId;
                Log.d(TAG, "Current package ID: " + (packageId != null ? packageId : "null (no active package)"));

                // Update adapter jika sudah loaded
                if (adapter != null) {
                    adapter.setCurrentPackageId(currentPackageId == null ? -1 : currentPackageId);
                    updateSubmitState();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Log.e(TAG, "Failed to load current package: " + message);
                // Don't show error toast, just log it
                // User can still proceed without knowing current package
            }
        });
    }

    private void loadPackages() {
        setLoading(true);
        packagesRepository.fetchPackages(new ServicePackagesRepository.PackagesCallback() {
            @Override
            public void onSuccess(List<Paket> paketList) {
                if (!isAdded()) return;
                adapter.setCurrentPackageId(currentPackageId == null ? -1 : currentPackageId);
                adapter.setItems(paketList);
                setLoading(false);
                updateSubmitState();
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(), "Gagal memuat paket: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadStatus() {
        // Note: Get status currently only supported for PHP backend
        // For Supabase backend, status check is done via RPC but not yet implemented here
        // For now, skip status check when using Supabase
        if (USE_SUPABASE_BACKEND) {
            Log.d(TAG, "Skipping status check (not implemented for Supabase yet)");
            hideStatusCard();
            return;
        }

        // PHP backend status check
        changeRepo.getActiveChangeStatus(new ChangePackageRepository.StatusCallback() {
            @Override
            public void onSuccess(@Nullable ChangePackageStatusResponse status) {
                if (!isAdded()) return;
                if (status != null) {
                    currentPackageId = status.getPaketSekarangId() != null ? status.getPaketSekarangId().intValue() : null;
                    showStatusCard(status);
                    binding.btnSubmitChange.setEnabled(false);
                } else {
                    hideStatusCard();
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                hideStatusCard();
            }
        });
    }

    private void showStatusCard(ChangePackageStatusResponse status) {
        binding.cardStatus.setVisibility(View.VISIBLE);
        String statusText = "Status: " + status.getStatusKeputusan();
        if (!TextUtils.isEmpty(status.getJadwalAktivasi())) {
            statusText += " • Jadwal: " + status.getJadwalAktivasi();
        }
        if (!TextUtils.isEmpty(status.getDiterapkanPada())) {
            statusText += " • Diterapkan: " + status.getDiterapkanPada();
        }
        binding.textStatusDetail.setText(statusText);
    }

    private void hideStatusCard() {
        binding.cardStatus.setVisibility(View.GONE);
    }

    private void submitChange() {
        if (selectedPackageId == null) {
            Toast.makeText(requireContext(), "Pilih paket baru terlebih dahulu.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPackageId != null && selectedPackageId == currentPackageId.longValue()) {
            Toast.makeText(requireContext(), "Paket sudah aktif.", Toast.LENGTH_SHORT).show();
            return;
        }
        String note = binding.etNote.getText() != null ? binding.etNote.getText().toString().trim() : "";
        setLoading(true);

        // Use Supabase or PHP backend based on feature flag
        if (USE_SUPABASE_BACKEND) {
            submitViaSupabase(selectedPackageId, note);
        } else {
            submitViaPHP(selectedPackageId, note);
        }
    }

    private void submitViaPHP(long packageId, String note) {
        changeRepo.submitChangePackage(packageId, note, new ChangePackageRepository.SubmitCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Permintaan dikirim. Paket akan diproses admin.", Toast.LENGTH_LONG).show();
                loadStatus();
                setLoading(false);
                binding.btnSubmitChange.setEnabled(false);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void submitViaSupabase(long packageId, String note) {
        supabaseRepo.submitChangePackage(packageId, note, new ChangePackageSupabaseRepository.SubmitCallback() {
            @Override
            public void onSuccess(String message, long ticketId) {
                if (!isAdded()) return;
                Log.d(TAG, "Submit success via Supabase! Ticket ID: " + ticketId);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                loadStatus();
                setLoading(false);
                binding.btnSubmitChange.setEnabled(false);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                Log.e(TAG, "Submit error via Supabase: " + message);
                setLoading(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateSubmitState() {
        boolean enabled = selectedPackageId != null && (currentPackageId == null || selectedPackageId != currentPackageId.longValue());
        binding.btnSubmitChange.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
