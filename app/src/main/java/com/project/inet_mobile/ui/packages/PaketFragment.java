package com.project.inet_mobile.ui.packages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.project.inet_mobile.R;
import com.project.inet_mobile.Paket;
import com.project.inet_mobile.PaketAdapter;
import com.project.inet_mobile.util.conn;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

        // Buat adapter dengan list kosong dan listener
        paketAdapter = new PaketAdapter(new ArrayList<>(), new PaketAdapter.OnPaketClickListener() {
            @Override
            public void onPaketClick(Paket paket) {
                // Tangani klik paket di sini jika diperlukan
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

        conn.fetchServicePackages(getContext(), new conn.PackagesCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // Cancel timeout karena data sudah berhasil dimuat
                    cancelTimeout();
                    isDataLoaded = true;

                    // Stop swipe refresh loading
                    if (swipeRefresh.isRefreshing()) {
                        swipeRefresh.setRefreshing(false);
                    }

                    if (data != null && data.length() > 0) {
                        bindToCards(data);
                        showData();

                        // Tampilkan toast sukses saat refresh
                        if (paketAdapter.getItemCount() > 0) {
                            Toast.makeText(getContext(), "Data berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        }
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

    private void bindToCards(JSONArray arr) {
        NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        List<Paket> paketList = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) {
                String name = ns(o.optString("name"));
                String desc = buildDesc(o.optString("speed"), o.optString("description"));
                String price = formatRupiah(rupiah, o);
                boolean isPopuler = o.optBoolean("is_popular", false);

                Paket paket = new Paket(name, desc, price, isPopuler);
                paketList.add(paket);
            }
        }

        // Update adapter dengan data baru
        paketAdapter.updateData(paketList);
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

    private String buildDesc(String speed, String description) {
        String s = ns(speed);
        String d = ns(description);
        if (!s.isEmpty() && !d.isEmpty()) return s + " • " + d;
        if (!s.isEmpty()) return s;
        return d;
    }

    private String formatRupiah(NumberFormat nf, JSONObject o) {
        String priceStr = o.optString("price", null);
        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                double val = Double.parseDouble(priceStr);
                return nf.format(val).replace(",00", "");
            } catch (NumberFormatException ignored) {}
        }
        double val = o.optDouble("price", Double.NaN);
        if (!Double.isNaN(val)) {
            return nf.format(val).replace(",00", "");
        }
        return "Rp -";
    }

    private String ns(String s) {
        return s == null ? "" : s;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancel timeout saat fragment di-destroy untuk mencegah memory leak
        cancelTimeout();
    }
}