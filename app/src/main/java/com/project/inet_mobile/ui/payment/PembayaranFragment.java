package com.project.inet_mobile.ui.payment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import com.project.inet_mobile.R;

public class PembayaranFragment extends Fragment {

    public PembayaranFragment() {
        super(R.layout.fragment_pembayaran);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Sembunyikan Bottom Navigation saat masuk pembayaran
     */
    private void hideBottomNavigation() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.dashboardBottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Tampilkan Bottom Navigation saat keluar dari pembayaran
     */
    private void showBottomNavigation() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.dashboardBottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Sembunyikan bottom navbar setiap kali fragment menjadi aktif
        hideBottomNavigation();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Tampilkan kembali bottom navbar saat fragment tidak aktif
        showBottomNavigation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
