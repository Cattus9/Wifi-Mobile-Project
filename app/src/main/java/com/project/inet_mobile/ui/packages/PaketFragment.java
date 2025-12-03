package com.project.inet_mobile.ui.packages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.project.inet_mobile.R;
import com.project.inet_mobile.Paket;
import com.project.inet_mobile.PaketAdapter;
import com.project.inet_mobile.util.DetailPaketFragment;
import com.project.inet_mobile.data.packages.ServicePackagesRepository;

import java.util.ArrayList;
import java.util.List;

public class PaketFragment extends Fragment {

    private RecyclerView recyclerViewPaket;
    private PaketAdapter paketAdapter;
    private LottieAnimationView loadingAnimation;
    private LinearLayout errorLayout;
    private TextView errorMessage;
    private Button btnRetry;
    private SwipeRefreshLayout swipeRefresh;

    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private boolean isDataLoaded = false;
    private static final int TIMEOUT_DURATION = 10000; // 10 detik
    private ServicePackagesRepository packagesRepository;

    public PaketFragment() {
        super(R.layout.fragment_paket);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi Views
        recyclerViewPaket = view.findViewById(R.id.recyclerViewPaket);
        loadingAnimation = view.findViewById(R.id.loadingAnimation);
        errorLayout = view.findViewById(R.id.errorLayout);
        errorMessage = view.findViewById(R.id.errorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        // Inisialisasi Handler untuk timeout
        timeoutHandler = new Handler(Looper.getMainLooper());
        packagesRepository = new ServicePackagesRepository(requireContext());

        // Buat adapter dengan list kosong dan listener
        paketAdapter = new PaketAdapter(new ArrayList<>(), new PaketAdapter.OnPaketClickListener() {
            @Override
            public void onPaketClick(Paket paket) {
                openDetailFragment(paket);
            }
        });

        // Atur LayoutManager dan Adapter
        recyclerViewPaket.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPaket.setAdapter(paketAdapter);

        // Setup SwipeRefresh
        setupSwipeRefresh();

        // Setup tombol retry
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadPackages();
            }
        });

        // Load data pertama kali
        loadPackages();
    }

    private void setupSwipeRefresh() {
        // Set warna loading indicator
        swipeRefresh.setColorSchemeColors(
                getResources().getColor(R.color.register_primary, null)
        );

        // Set listener untuk swipe refresh
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Reload data saat user swipe down
                loadPackages();
            }
        });
    }

    private void loadPackages() {
        if (getContext() == null) return;

        // Reset flag
        isDataLoaded = false;

        // Tampilkan loading (hanya jika tidak sedang swipe refresh)
        if (!swipeRefresh.isRefreshing()) {
            showLoading();
        }

        // Setup timeout (10 detik)
        startTimeout();

        packagesRepository.fetchPackages(new ServicePackagesRepository.PackagesCallback() {
            @Override
            public void onSuccess(List<Paket> paketList) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // Cancel timeout karena data sudah berhasil dimuat
                    cancelTimeout();
                    isDataLoaded = true;

                    // Stop swipe refresh loading
                    if (swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(false);
                    }

                    if (paketList != null && !paketList.isEmpty()) {
                        paketAdapter.updateData(paketList);
                        showData();
                    } else {
                        showError("Tidak ada paket tersedia");
                    }
                });
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // Cancel timeout karena sudah ada response (meskipun error)
                    cancelTimeout();
                    isDataLoaded = true;

                    // Stop swipe refresh loading
                    if (swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(false);
                    }

                    // Tampilkan pesan user-friendly saja
                    showError("Gagal memuat data paket");
                });
            }
        });
    }

    private void startTimeout() {
        // Buat runnable untuk timeout
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                // Jika setelah 10 detik data belum dimuat, tampilkan error
                if (!isDataLoaded && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Stop swipe refresh loading
                        if (swipeRefresh.isRefreshing()) {
                            swipeRefresh.setRefreshing(false);
                        }
                        showError("Koneksi terlalu lama\nSilakan coba lagi");
                    });
                }
            }
        };

        // Jalankan timeout setelah 10 detik
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DURATION);
    }

    private void cancelTimeout() {
        // Cancel timeout jika data sudah dimuat sebelum 10 detik
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    // Method untuk show/hide views
    private void showLoading() {
        if (loadingAnimation != null) {
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
        }
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        if (recyclerViewPaket != null) recyclerViewPaket.setVisibility(View.GONE);
        if (swipeRefresh != null) swipeRefresh.setEnabled(false);
    }

    private void showError(String message) {
        if (loadingAnimation != null) {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.cancelAnimation();
        }
        if (errorLayout != null) errorLayout.setVisibility(View.VISIBLE);
        if (recyclerViewPaket != null) recyclerViewPaket.setVisibility(View.GONE);
        if (errorMessage != null) errorMessage.setText(message);
        if (swipeRefresh != null) swipeRefresh.setEnabled(false);
    }

    private void showData() {
        if (loadingAnimation != null) {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.cancelAnimation();
        }
        if (errorLayout != null) errorLayout.setVisibility(View.GONE);
        if (recyclerViewPaket != null) recyclerViewPaket.setVisibility(View.VISIBLE);
        if (swipeRefresh != null) swipeRefresh.setEnabled(true);
    }

    private void openDetailFragment(Paket paket) {
        if (paket == null || !isAdded() || getActivity() == null) {
            return;
        }

        DetailPaketFragment detailFragment = DetailPaketFragment.newInstance(paket);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.dashboardFragmentContainer, detailFragment)
                .addToBackStack("detail_paket")
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelTimeout();
    }
}
