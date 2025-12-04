package com.project.inet_mobile.ui.home; // Ganti dengan package Anda

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.project.inet_mobile.LoginActivity;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.databinding.FragmentBerandaBinding; // Impor class binding
import com.project.inet_mobile.ui.payment.PembayaranFragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BerandaFragment extends Fragment {

    // 1. Deklarasikan variabel View Binding
    private FragmentBerandaBinding binding;
    private TokenStorage tokenStorage;

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

        // Initialize TokenStorage
        tokenStorage = new TokenStorage(requireContext());

        // Panggil fungsi-fungsi untuk mengatur tampilan
        setupDynamicGreeting();
        setupDynamicDate();
        setupActivePackageSection();
        setupUserDataSections();
        setupClickListeners();
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
     * Setup active package section
     */
    private void setupActivePackageSection() {
        // Show real data (or keep hardcoded values for now)
        if (binding.tvHeaderTitle != null) {
            binding.tvHeaderTitle.setText("PAKET AKTIF");
        }

        // TODO: Load real data from API when available
        // For now, the hardcoded values in XML will be shown
    }

    /**
     * Setup user data sections
     */
    private void setupUserDataSections() {
        // Show all cards
        binding.cardJatuhTempo.setVisibility(View.VISIBLE);
        binding.cardTagihan.setVisibility(View.VISIBLE);
    }

    /**
     * Helper method to set grid item value by position
     */
    private void setGridItemValue(int row, int col, String value) {
        int index = row * 2 + col;
        View cardView = binding.gridLayout.getChildAt(index);
        if (cardView instanceof androidx.cardview.widget.CardView) {
            ViewGroup linearLayout = (ViewGroup) ((ViewGroup) cardView).getChildAt(0);
            if (linearLayout instanceof android.widget.LinearLayout && linearLayout.getChildCount() >= 2) {
                View valueTextView = linearLayout.getChildAt(1);
                if (valueTextView instanceof android.widget.TextView) {
                    ((android.widget.TextView) valueTextView).setText(value);
                    if (value.equals("-")) {
                        ((android.widget.TextView) valueTextView).setTextColor(getResources().getColor(android.R.color.darker_gray, null));
                    }
                }
            }
        }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 3. Bersihkan binding untuk menghindari memory leak
        binding = null;
    }
}