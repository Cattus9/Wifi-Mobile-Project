package com.project.inet_mobile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.project.inet_mobile.adapter.PackageSelectionAdapter;
import com.project.inet_mobile.data.packages.ServicePackagesRepository;

import java.util.List;

/**
 * Activity for selecting a service package before registration
 * Flow: SplashActivity -> PackageSelectionActivity -> RegisterActivity
 */
public class PackageSelectionActivity extends AppCompatActivity {

    private static final String TAG = "PackageSelection";

    private RecyclerView rvPackages;
    private View emptyState;
    private LottieAnimationView loadingAnimation;
    private androidx.cardview.widget.CardView cardLoadingAnimation;
    private View overlayBackground;

    private PackageSelectionAdapter adapter;
    private ServicePackagesRepository packagesRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_package_selection);

        initViews();
        initRepository();
        setupRecyclerView();
        fetchPackages();
    }

    private void initViews() {
        rvPackages = findViewById(R.id.rvPackages);
        emptyState = findViewById(R.id.emptyState);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        cardLoadingAnimation = findViewById(R.id.cardLoadingAnimation);
        overlayBackground = findViewById(R.id.overlayBackground);
    }

    private void initRepository() {
        // For package selection, we don't need authentication
        // We'll create a simpler version without TokenStorage dependency
        packagesRepository = new ServicePackagesRepository(this);
    }

    private void setupRecyclerView() {
        adapter = new PackageSelectionAdapter(paket -> {
            Log.d(TAG, "Package selected: " + paket.getName() + " (ID: " + paket.getId() + ")");
            navigateToRegister(paket.getId());
        });

        rvPackages.setLayoutManager(new LinearLayoutManager(this));
        rvPackages.setAdapter(adapter);
    }

    private void fetchPackages() {
        showLoading(true);

        // Use anonymous fetch since user hasn't logged in yet
        packagesRepository.fetchPackagesAnonymous(new ServicePackagesRepository.PackagesCallback() {
            @Override
            public void onSuccess(List<Paket> paketList) {
                showLoading(false);

                if (paketList == null || paketList.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    adapter.setPackages(paketList);
                    Log.d(TAG, "Loaded " + paketList.size() + " packages");
                }
            }

            @Override
            public void onError(String message) {
                showLoading(false);
                Log.e(TAG, "Error loading packages: " + message);

                // Show error message
                Toast.makeText(PackageSelectionActivity.this,
                        "Gagal memuat paket: " + message,
                        Toast.LENGTH_LONG).show();

                // Show empty state
                showEmptyState(true);
            }
        });
    }

    private void navigateToRegister(int packageId) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("SELECTED_PACKAGE_ID", packageId);
        startActivity(intent);
        finish(); // Don't allow back to package selection
    }

    private void showLoading(boolean show) {
        if (show) {
            cardLoadingAnimation.setVisibility(View.VISIBLE);
            overlayBackground.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
            rvPackages.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            cardLoadingAnimation.setVisibility(View.GONE);
            overlayBackground.setVisibility(View.GONE);
            loadingAnimation.cancelAnimation();
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyState.setVisibility(View.VISIBLE);
            rvPackages.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            rvPackages.setVisibility(View.VISIBLE);
        }
    }
}
