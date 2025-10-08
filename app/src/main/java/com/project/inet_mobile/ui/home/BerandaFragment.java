package com.project.inet_mobile.ui.home;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;

import com.project.inet_mobile.R;

public class BerandaFragment extends Fragment {

    public BerandaFragment() {
        super(R.layout.fragment_beranda);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // No header title yet; keep empty as requested.
    }
}
