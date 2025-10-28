package com.project.inet_mobile.ui.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.project.inet_mobile.LoginActivity;
import com.project.inet_mobile.R;

public class AkunFragment extends Fragment {

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";

    public AkunFragment() {
        super(R.layout.fragment_akun);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView profileName = view.findViewById(R.id.profileName);
        TextView profileEmail = view.findViewById(R.id.profileEmail);
        MaterialButton buttonLogout = view.findViewById(R.id.buttonLogout);

        bindProfileInfo(profileName, profileEmail);

        buttonLogout.setOnClickListener(v -> showLogoutDialog(buttonLogout));
    }

    private void bindProfileInfo(TextView profileName, TextView profileEmail) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_USER_NAME, "");
        String email = prefs.getString(KEY_USER_EMAIL, "");

        profileName.setText(!TextUtils.isEmpty(name) ? name : getString(R.string.profile_name_placeholder));
        profileEmail.setText(!TextUtils.isEmpty(email) ? email : getString(R.string.profile_email_placeholder));
    }

    private void showLogoutDialog(MaterialButton buttonLogout) {
        if (!(requireActivity() instanceof AppCompatActivity)) {
            Toast.makeText(requireContext(), R.string.logout_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setNegativeButton(R.string.logout_dialog_cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.logout_dialog_confirm, (dialog, which) -> {
                    dialog.dismiss();
                    buttonLogout.setEnabled(false);
                    performLogout((AppCompatActivity) requireActivity());
                })
                .show();
    }

    private void performLogout(AppCompatActivity activity) {
        LoginActivity.logout(activity);
    }
}
