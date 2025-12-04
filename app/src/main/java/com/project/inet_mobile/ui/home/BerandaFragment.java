package com.project.inet_mobile.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.project.inet_mobile.databinding.FragmentBerandaBinding;
import com.project.inet_mobile.ui.payment.PembayaranFragment;
import com.project.inet_mobile.data.dashboard.DashboardSupabaseRepository;
import com.project.inet_mobile.data.remote.dto.SupabaseDashboardUserDto;
import com.project.inet_mobile.data.remote.dto.SupabaseDashboardInvoiceDto;
import com.project.inet_mobile.data.remote.dto.SupabaseCustomerDto;
import com.project.inet_mobile.data.remote.dto.SupabaseServicePackageDto;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BerandaFragment extends Fragment {

    private FragmentBerandaBinding binding;
    private DashboardSupabaseRepository dashboardRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 2. Inflate layout menggunakan binding
        binding = FragmentBerandaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize repository
        dashboardRepository = new DashboardSupabaseRepository(requireContext());

        // Setup UI
        setupDynamicGreeting();
        setupDynamicDate();
        setupClickListeners();

        // Fetch real data from Supabase
        fetchDashboardData();
    }

    /**
     * Mengatur greeting dinamis berdasarkan waktu saat ini.
     */
    private void setupDynamicGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (hour >= 0 && hour < 11) {
            greeting = "Selamat Pagi👋";
        } else if (hour >= 11 && hour < 15) {
            greeting = "Selamat Siang👋";
        } else if (hour >= 15 && hour < 18) {
            greeting = "Selamat Sore👋";
        } else {
            greeting = "Selamat Malam👋";
        }

        binding.berandaTitle.setText(greeting);
    }

    /**
     * Mengatur tanggal hari ini pada TextView.
     */
    private void setupDynamicDate() {
        // Menggunakan format "Hari, DD MMMM YYYY" (misal: Selasa, 18 November 2025)
        // Locale "id", "ID" digunakan untuk format Bahasa Indonesia
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        String currentDate = dateFormat.format(new Date());

        // Set tanggal ke TextView menggunakan binding
        binding.berandaDate.setText(currentDate);
    }

    /**
     * Mengatur semua OnClickListener untuk elemen yang bisa diklik.
     */
    private void setupClickListeners() {
        // Menggunakan lambda (->) untuk listener yang lebih ringkas

        // --- Menu Cepat ---
        binding.cardPembayaran.setOnClickListener(v -> {
            navigateToPembayaran();
        });

        binding.cardCs.setOnClickListener(v -> {
            // TODO: Tambahkan logika untuk pindah ke halaman Customer Service
            showToast("Membuka Customer Service...");
        });

        binding.cardHistory.setOnClickListener(v -> {
            // TODO: Tambahkan logika untuk pindah ke halaman History
            showToast("Membuka History...");
        });

        // --- Informasi ---
        binding.cardJatuhTempo.setOnClickListener(v -> {
            showToast("Info Jatuh Tempo diklik");
        });

        binding.cardTagihan.setOnClickListener(v -> {
            showToast("Info Tagihan diklik");
        });
    }

    /**
     * Fungsi helper untuk menampilkan Toast singkat.
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Navigasi ke PembayaranFragment
     */
    private void navigateToPembayaran() {
        if (getActivity() != null) {
            PembayaranFragment pembayaranFragment = new PembayaranFragment();

            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(com.project.inet_mobile.R.id.dashboardFragmentContainer, pembayaranFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    /**
     * Fetch dashboard data from Supabase
     */
    private void fetchDashboardData() {
        // Show loading state
        setLoading(true);

        dashboardRepository.fetchDashboardData(new DashboardSupabaseRepository.DashboardCallback() {
            @Override
            public void onSuccess(@NonNull SupabaseDashboardUserDto userData, @Nullable SupabaseDashboardInvoiceDto invoice) {
                if (!isAdded()) return;

                setLoading(false);
                updateUIWithData(userData, invoice);
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;

                setLoading(false);
                showToast(message);
            }
        });
    }

    /**
     * Update all UI elements with fetched data
     */
    private void updateUIWithData(@NonNull SupabaseDashboardUserDto userData, @Nullable SupabaseDashboardInvoiceDto invoice) {
        SupabaseCustomerDto customer = userData.getCustomer();
        if (customer == null) return;

        SupabaseServicePackageDto servicePackage = customer.getServicePackage();

        // Update package info
        if (servicePackage != null) {
            binding.tvPackageName.setText(servicePackage.getName());
            binding.tvPackageSpeed.setText(servicePackage.getSpeed() + " Mbps");
            binding.tvPackageQuota.setText(servicePackage.getQuota());
        } else {
            binding.tvPackageName.setText("Tidak ada paket aktif");
            binding.tvPackageSpeed.setText("-");
            binding.tvPackageQuota.setText("-");
        }

        // Update user status
        String statusDisplay = customer.getStatusDisplay();
        binding.tvUserStatus.setText(statusDisplay);

        // Set status color
        if (customer.isActive()) {
            binding.tvUserStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            binding.tvUserStatus.setTextColor(Color.parseColor("#F44336")); // Red
        }

        // Update invoice info
        if (invoice != null) {
            binding.tvJatuhTempoDate.setText(invoice.getFormattedDueDate());
            binding.tvTagihanAmount.setText(invoice.getFormattedAmount());

            // Calculate and display masa aktif (days until due)
            int daysUntilDue = invoice.getDaysUntilDue();
            if (daysUntilDue >= 0) {
                binding.tvMasaAktif.setText(daysUntilDue + " Hari");
            } else {
                binding.tvMasaAktif.setText("Terlambat " + Math.abs(daysUntilDue) + " Hari");
                binding.tvMasaAktif.setTextColor(Color.parseColor("#F44336")); // Red if overdue
            }
        } else {
            // No unpaid invoice
            binding.tvJatuhTempoDate.setText("-");
            binding.tvTagihanAmount.setText("Rp 0");
            binding.tvMasaAktif.setText("-");
        }
    }

    /**
     * Show/hide loading state
     */
    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.berandaPrimaryCardCard.setVisibility(View.GONE);
            // You can add a ProgressBar to the layout and show it here
        } else {
            binding.berandaPrimaryCardCard.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}