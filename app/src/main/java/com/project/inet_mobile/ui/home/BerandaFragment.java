package com.project.inet_mobile.ui.home; // Ganti dengan package Anda

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.inet_mobile.databinding.FragmentBerandaBinding; // Impor class binding

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BerandaFragment extends Fragment {

    // 1. Deklarasikan variabel View Binding
    private FragmentBerandaBinding binding;

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

        // Panggil fungsi-fungsi untuk mengatur tampilan
        setupDynamicDate();
        setupClickListeners();
    }

    /**
     * Mengatur tanggal hari ini pada TextView.
     */
    private void setupDynamicDate() {
        // Menggunakan format "Hari, DD MMMM YYYY" (misal: Selasa, 18 November 2025)
        // Locale "id", "ID" digunakan untuk format Bahasa Indonesia
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID"));
        String currentDate = dateFormat.format(new Date());

    }

    /**
     * Mengatur semua OnClickListener untuk elemen yang bisa diklik.
     */
    private void setupClickListeners() {
        // Menggunakan lambda (->) untuk listener yang lebih ringkas

        // --- Menu Cepat ---
        binding.cardPembayaran.setOnClickListener(v -> {
            // TODO: Tambahkan logika untuk pindah ke halaman Pembayaran
            showToast("Membuka menu Pembayaran...");
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

        binding.cardHotline.setOnClickListener(v -> {
            showToast("Info Hotline diklik");
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 3. Bersihkan binding untuk menghindari memory leak
        binding = null;
    }
}