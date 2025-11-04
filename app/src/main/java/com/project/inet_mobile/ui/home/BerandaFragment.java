package com.project.inet_mobile.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import com.project.inet_mobile.R;
import com.project.inet_mobile.databinding.FragmentBerandaBinding;

public class BerandaFragment extends Fragment {

    public BerandaFragment() {
        super(R.layout.fragment_beranda);
    }

    private FragmentBerandaBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Menginisialisasi View Binding
        binding = FragmentBerandaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Panggil fungsi untuk mengisi data paket
        displayActivePackageData();
    }

    // --- Fungsi untuk Mengisi Data Paket Aktif ---
    private void displayActivePackageData() {
        // 1. Data Dummy (Ganti dengan data yang diambil dari API/Database)
        String packageName = "Paket Kebelet";
        String price = "Rp 100K";
        String details = "90 Mbps · Unlimited";
        int quotaUsedGB = 204;
        int quotaLimitGB = 300; // Contoh batas FUP 300 GB
        String expiryDate = "15 Nov 2025";
        int remainingDays = 15;

        // Hitung persentase progress bar (204 / 300 * 100)
        // Kita menggunakan casting ke double untuk memastikan hasil perhitungan akurat
        int progressPercent = (int) (((double) quotaUsedGB / quotaLimitGB) * 100);

        // 2. Mengikat Data ke Elemen XML (Menggunakan View Binding)

        // TextViews di dalam CardView Paket Aktif (pastikan ID sesuai XML Anda)
        binding.tvPackageName.setText(packageName);
        binding.tvPackagePrice.setText(price);
        binding.tvPackageDetails.setText(details);

        // Progress Bar
        // Set nilai maksimum (misalnya 100) dan progress yang dihitung
        binding.quotaProgressBar.setMax(100);
        binding.quotaProgressBar.setProgress(progressPercent);

        // Detail Kuota
        binding.tvQuotaUsed.setText(quotaUsedGB + " GB terpakai");
        binding.tvQuotaLimit.setText("Unlimited"); // Atau tampilkan batas kuota

        // Detail Tanggal
        binding.tvExpiryDate.setText("Berakhir: " + expiryDate);
        binding.tvRemainingDays.setText(remainingDays + " hari lagi");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Membersihkan binding untuk menghindari kebocoran memori
        binding = null;
    }
}