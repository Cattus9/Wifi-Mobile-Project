package com.project.inet_mobile;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;

import androidx.activity.EdgeToEdge;
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

public class DashboardActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_ITEM = "dashboard_selected_item";

    private final SparseArray<String> fragmentTags = new SparseArray<>();
    private Fragment currentFragment;
    private int currentItemId = View.NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.dashboardToolbar);
        toolbar.setTitle("");

        View root = findViewById(R.id.dashboardRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.dashboardBottomNav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switchFragment(item.getItemId());
            return true;
        });

        if (savedInstanceState != null) {
            currentItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, View.NO_ID);
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

    private void switchFragment(int itemId) {
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
            transaction.hide(currentFragment);
        }

        transaction.commit();
        currentFragment = target;
        currentItemId = itemId;
    }

    private Fragment createFragment(int itemId) {
        if (itemId == R.id.navigation_beranda) {
            return new BerandaFragment();
        } else if (itemId == R.id.navigation_paket) {
            return new PaketFragment();
        } else if (itemId == R.id.navigation_pembayaran) {
            return new PembayaranFragment();
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
    }
}
