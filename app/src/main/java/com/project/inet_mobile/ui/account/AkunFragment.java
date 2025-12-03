package com.project.inet_mobile.ui.account;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater; // Added for ViewBinding
import android.view.View;
import android.view.ViewGroup; // Added for ViewBinding
import android.widget.ImageView; // Added for avatar
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Added for ViewModel

import com.bumptech.glide.Glide; // Added for Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions; // Added for Glide transitions
import com.project.inet_mobile.LoginActivity;
import com.project.inet_mobile.R;
import com.project.inet_mobile.data.auth.User; // Added User model
import com.project.inet_mobile.databinding.FragmentAkunBinding; // Added ViewBinding
import com.project.inet_mobile.ui.account.EditProfileFragment;
import com.project.inet_mobile.ui.account.ChangePackageFragment;

import com.google.android.material.button.MaterialButton; // Added MaterialButton import
import com.google.android.material.dialog.MaterialAlertDialogBuilder; // Added MaterialAlertDialogBuilder import

public class AkunFragment extends Fragment {

    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_EMAIL = "userEmail";

    private FragmentAkunBinding binding; // ViewBinding instance
    private AkunViewModel akunViewModel; // ViewModel instance
    private static final String RESULT_KEY_PROFILE_REFRESH = "profile_refresh_key";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAkunBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        AkunViewModel.AkunViewModelFactory factory = new AkunViewModel.AkunViewModelFactory(requireActivity().getApplication());
        akunViewModel = new ViewModelProvider(this, factory).get(AkunViewModel.class);

        // Observe ViewModel data
        observeViewModel();

        // Trigger data fetch
        akunViewModel.fetchUserProfile();

        binding.swipeRefreshProfile.setOnRefreshListener(() -> akunViewModel.fetchUserProfile());

        // Listen for refresh event from EditProfileFragment
        getParentFragmentManager().setFragmentResultListener(RESULT_KEY_PROFILE_REFRESH, this, (requestKey, bundle) -> {
            akunViewModel.fetchUserProfile();
        });

        // Set up logout button listener
        binding.buttonLogout.setOnClickListener(v -> showLogoutDialog(binding.buttonLogout));

        // Set up edit profile button listener
        binding.buttonEditProfile.setOnClickListener(v -> {
            User currentUser = akunViewModel.userProfile.getValue();
            if (currentUser != null) {
                EditProfileFragment editFragment = new EditProfileFragment();
                Bundle args = new Bundle();
                args.putSerializable("currentUser", currentUser);
                editFragment.setArguments(args);

                getParentFragmentManager().beginTransaction()
                        .replace(getId(), editFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Data profil belum dimuat, coba lagi.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.buttonChangePackage.setOnClickListener(v -> {
            ChangePackageFragment changeFragment = new ChangePackageFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(getId(), changeFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void observeViewModel() {
        akunViewModel.userProfile.observe(getViewLifecycleOwner(), this::updateUiWithProfile);
        akunViewModel.isLoading.observe(getViewLifecycleOwner(), this::showLoading);
        akunViewModel.errorMessage.observe(getViewLifecycleOwner(), this::showError);
    }

    private void updateUiWithProfile(User user) {
        if (user == null) return;

        String customerName = getString(R.string.profile_name_placeholder);
        if (user.getCustomer() != null && user.getCustomer().getName() != null) {
            customerName = user.getCustomer().getName();
        }

        String email = user.getEmail() != null ? user.getEmail() : getString(R.string.profile_email_placeholder);
        
        // Prioritaskan nomor telepon dari tabel customer jika ada, jika tidak, gunakan dari tabel user
        String phone = getString(R.string.profile_phone_placeholder);
        if (user.getCustomer() != null && user.getCustomer().getPhone() != null && !user.getCustomer().getPhone().isEmpty()) {
            phone = user.getCustomer().getPhone();
        } else if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            phone = user.getPhone();
        }

        // Update main profile info
        binding.profileName.setText(customerName);
        binding.profileEmail.setText(email);
        
        // Update "Identitas Pengguna" card title
        binding.textTitleIdentitasPengguna.setText(customerName);
        binding.textSubtitleIdentitasPengguna.setText(email);
        
        // Update "Identitas Pengguna" card details
        binding.textNameIdentitasPengguna.setText("Nama: " + customerName);
        binding.textEmailIdentitasPengguna.setText("Email: " + email);
        binding.textPhoneIdentitasPengguna.setText("No. Telp: " + phone);

        // Load avatar using Glide
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_user) // Placeholder while loading
                    .error(R.drawable.ic_user) // Error placeholder
                    .circleCrop() // Make avatar circular
                    .into(binding.profileAvatar);
        } else {
            binding.profileAvatar.setImageResource(R.drawable.ic_user);
        }
    }

    private void showLoading(Boolean isLoading) {
        // Implement logic to show/hide a progress indicator
        if (isLoading) {
            // binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonLogout.setEnabled(false); // Disable interaction
            // binding.buttonEditProfile.setEnabled(false);
            binding.swipeRefreshProfile.setRefreshing(true);
        } else {
            // binding.progressBar.setVisibility(View.GONE);
            binding.buttonLogout.setEnabled(true);
            // binding.buttonEditProfile.setEnabled(true);
            binding.swipeRefreshProfile.setRefreshing(false);
        }
    }

    private void showError(String message) {
        if (message != null && !message.isEmpty()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
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
        // The original LoginActivity.logout uses SharedPreferences,
        // which still refers to PREF_NAME, KEY_USER_NAME, KEY_USER_EMAIL
        // If these are no longer needed for logout logic, they can be removed from AkunFragment
        // For now, keeping the static final fields as they might be used by LoginActivity.logout internally.
        LoginActivity.logout(activity);
    }
}

