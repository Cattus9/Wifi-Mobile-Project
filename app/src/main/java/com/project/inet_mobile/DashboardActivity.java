package com.project.inet_mobile;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.inet_mobile.ui.account.AkunFragment;
import com.project.inet_mobile.ui.history.RiwayatFragment;
import com.project.inet_mobile.ui.home.BerandaFragment;
import com.project.inet_mobile.ui.packages.PaketFragment;
import com.project.inet_mobile.ui.payment.PembayaranFragment;

import java.util.LinkedList;

public class DashboardActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_ITEM = "dashboard_selected_item";
    private static final String KEY_NAVIGATION_HISTORY = "dashboard_navigation_history";
    private static final int MAX_HISTORY_SIZE = 5;

    private final SparseArray<String> fragmentTags = new SparseArray<>();
    private Fragment currentFragment;
    private int currentItemId = View.NO_ID;

    // LinkedList untuk menyimpan history navigasi
    private LinkedList<Integer> navigationHistory = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.dashboardToolbar);
        toolbar.setTitle("");

        // Apply window insets ke toolbar
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        // Apply window insets ke bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.dashboardBottomNav);
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigationView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId(), true);
            return true;
        });

        // Handle tombol back
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                    return;
                }

                if (navigationHistory.size() > 1) {
                    // Hapus fragment saat ini dari history
                    navigationHistory.removeLast();

                    // Ambil fragment sebelumnya
                    int previousItemId = navigationHistory.getLast();

                    // Switch ke fragment sebelumnya tanpa menambah ke history
                    switchFragment(previousItemId, false);

                    // Update bottom navigation selection
                    bottomNavigationView.setSelectedItemId(previousItemId);
                } else {
                    // Jika tidak ada history, keluar dari aplikasi
                    finish();
                }
            }
        });

        if (savedInstanceState != null) {
            currentItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, View.NO_ID);

            // Restore navigation history
            int[] historyArray = savedInstanceState.getIntArray(KEY_NAVIGATION_HISTORY);
            if (historyArray != null) {
                navigationHistory.clear();
                for (int itemId : historyArray) {
                    navigationHistory.add(itemId);
                }
            }

            if (currentItemId != View.NO_ID) {
                String tag = getFragmentTag(currentItemId);
                currentFragment = getSupportFragmentManager().findFragmentByTag(tag);
                bottomNavigationView.setSelectedItemId(currentItemId);
            } else {
                bottomNavigationView.setSelectedItemId(R.id.navigation_beranda);
            }
        } else {
            bottomNavigationView.setSelectedItemId(R.id.navigation_beranda);
        }
    }

    private void switchFragment(int itemId, boolean addToHistory) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String tag = getFragmentTag(itemId);
        Fragment target = fragmentManager.findFragmentByTag(tag);

        boolean isCurrentSameItem = currentItemId == itemId && currentFragment != null && currentFragment == target && currentFragment.isAdded();
        if (isCurrentSameItem) {
            return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        if (target == null) {
            target = createFragment(itemId);
            if (target == null) {
                return;
            }
            transaction.add(R.id.dashboardFragmentContainer, target, tag);
        } else {
            transaction.show(target);
        }

        if (currentFragment != null) {
            if (currentFragment.isAdded()) {
                transaction.hide(currentFragment);
            }
        }

        transaction.commit();
        currentFragment = target;
        currentItemId = itemId;

        // Tambahkan ke history jika diperlukan
        if (addToHistory) {
            // Hapus itemId dari history jika sudah ada (untuk menghindari duplikat berurutan)
            navigationHistory.remove(Integer.valueOf(itemId));

            // Tambahkan ke akhir history
            navigationHistory.add(itemId);

            // Batasi ukuran history maksimal 5
            while (navigationHistory.size() > MAX_HISTORY_SIZE) {
                navigationHistory.removeFirst();
            }
        }
    }

    private Fragment createFragment(int itemId) {
        if (itemId == R.id.navigation_beranda) {
            return new BerandaFragment();
        } else if (itemId == R.id.navigation_paket) {
            return new PaketFragment();
        } else if (itemId == R.id.navigation_riwayat) {
            return new RiwayatFragment();
        } else if (itemId == R.id.navigation_akun) {
            return new AkunFragment();
        }
        return null;
    }

    private String getFragmentTag(int itemId) {
        String tag = fragmentTags.get(itemId);
        if (tag == null) {
            tag = "dashboard_nav_" + itemId;
            fragmentTags.put(itemId, tag);
        }
        return tag;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_ITEM, currentItemId);

        // Simpan navigation history
        int[] historyArray = new int[navigationHistory.size()];
        for (int i = 0; i < navigationHistory.size(); i++) {
            historyArray[i] = navigationHistory.get(i);
        }
        outState.putIntArray(KEY_NAVIGATION_HISTORY, historyArray);
    }
}
