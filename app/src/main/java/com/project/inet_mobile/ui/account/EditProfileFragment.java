package com.project.inet_mobile.ui.account;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.bumptech.glide.Glide;
import com.project.inet_mobile.R;
import com.project.inet_mobile.data.auth.User;
import com.project.inet_mobile.databinding.FragmentEditProfileBinding;

public class EditProfileFragment extends Fragment {

    private FragmentEditProfileBinding binding;
    private EditProfileViewModel editProfileViewModel;
    private User currentUser;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;
    private ActivityResultLauncher<String> legacyPickImageLauncher;
    private ActivityResultLauncher<String> permissionLauncher;
    private android.net.Uri pendingAvatarUri;
    private boolean pendingDeleteAvatar = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        EditProfileViewModel.EditProfileViewModelFactory factory = new EditProfileViewModel.EditProfileViewModelFactory(requireActivity().getApplication());
        editProfileViewModel = new ViewModelProvider(this, factory).get(EditProfileViewModel.class);
        
        observeViewModel();
        setupClickListeners();
        setupPickers();
        loadInitialData();
    }

    private void observeViewModel() {
        editProfileViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null && isLoading) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.btnSave.setEnabled(false);
                binding.fabEditAvatar.setEnabled(false);
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.btnSave.setEnabled(true);
                binding.fabEditAvatar.setEnabled(true);
            }
        });

        editProfileViewModel.isUpdateSuccess.observe(getViewLifecycleOwner(), isSuccess -> {
            if (isSuccess) {
                proceedAvatarActionsAfterProfileUpdate();
            }
        });

        editProfileViewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        editProfileViewModel.avatarUrl.observe(getViewLifecycleOwner(), url -> {
            if (url != null && !url.isEmpty()) {
                Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .into(binding.imgAvatar);
                Toast.makeText(getContext(), "Avatar diperbarui.", Toast.LENGTH_SHORT).show();
                sendRefreshResult();
                getParentFragmentManager().popBackStack();
            } else if (url != null && url.isEmpty()) {
                binding.imgAvatar.setImageResource(R.drawable.ic_user);
                Toast.makeText(getContext(), "Avatar dihapus.", Toast.LENGTH_SHORT).show();
                sendRefreshResult();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void setupClickListeners() {
        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.fabEditAvatar.setOnClickListener(v -> {
            showAvatarOptions();
        });

        binding.btnSave.setOnClickListener(v -> saveChanges());
    }

    private void setupPickers() {
        photoPickerLauncher = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri != null) {
                handleImageUri(uri);
            }
        });

        legacyPickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleImageUri(uri);
            }
        });

        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                openLegacyImagePicker();
            } else {
                Toast.makeText(getContext(), "Izin diperlukan untuk memilih gambar.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestImagePick() {
        if (Build.VERSION.SDK_INT >= 33) {
            openPhotoPicker();
            return;
        }

        String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openLegacyImagePicker();
        } else if (shouldShowRequestPermissionRationale(permission)) {
            showPermissionRationale(permission);
        } else {
            permissionLauncher.launch(permission);
        }
    }

    private void openPhotoPicker() {
        if (photoPickerLauncher != null) {
            photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        }
    }

    private void openLegacyImagePicker() {
        if (legacyPickImageLauncher != null) {
            legacyPickImageLauncher.launch("image/*");
        }
    }

    private void showPermissionRationale(String permission) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Izin diperlukan")
                .setMessage("Aplikasi membutuhkan akses galeri untuk mengganti avatar Anda.")
                .setPositiveButton("Lanjutkan", (dialog, which) -> permissionLauncher.launch(permission))
                .setNegativeButton("Batal", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void handleImageUri(android.net.Uri uri) {
        if (currentUser == null || currentUser.getCustomerId() == null || currentUser.getId() == null) {
            Toast.makeText(getContext(), "Data pengguna tidak lengkap untuk upload avatar.", Toast.LENGTH_SHORT).show();
            return;
        }
        pendingAvatarUri = uri;
        pendingDeleteAvatar = false;
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.imgAvatar);
    }

    private void showAvatarOptions() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] options = new String[]{"Ganti avatar", "Hapus avatar"};
        new MaterialAlertDialogBuilder(requireContext())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        requestImagePick();
                    } else if (which == 1) {
                        confirmDeleteAvatar();
                    }
                })
                .show();
    }

    private void confirmDeleteAvatar() {
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(getContext(), "Data pengguna tidak lengkap untuk hapus avatar.", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Hapus avatar?")
                .setMessage("Avatar akan dihapus dan diganti placeholder.")
                .setNegativeButton("Batal", (d, w) -> d.dismiss())
                .setPositiveButton("Hapus", (d, w) -> {
                    pendingAvatarUri = null;
                    pendingDeleteAvatar = true;
                    binding.imgAvatar.setImageResource(R.drawable.ic_user);
                })
                .show();
    }

    private void loadInitialData() {
        if (getArguments() != null) {
            currentUser = (User) getArguments().getSerializable("currentUser");
        }

        if (currentUser != null) {
            String name = "";
            if (currentUser.getCustomer() != null && currentUser.getCustomer().getName() != null) {
                name = currentUser.getCustomer().getName();
            }
            binding.etName.setText(name);

            String phone = "";
            if (currentUser.getCustomer() != null && currentUser.getCustomer().getPhone() != null) {
                phone = currentUser.getCustomer().getPhone();
            } else if (currentUser.getPhone() != null) {
                phone = currentUser.getPhone();
            }
            binding.etPhone.setText(phone);
            
            Glide.with(this)
                .load(currentUser.getAvatarUrl())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.imgAvatar);
        }
    }
    
    private void saveChanges() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newName = binding.etName.getText().toString().trim();
        String newPhone = binding.etPhone.getText().toString().trim();
        Long customerId = currentUser.getCustomerId();

        if (newName.isEmpty()) {
            binding.tilName.setError("Nama tidak boleh kosong.");
            return;
        } else {
            binding.tilName.setError(null);
        }

        editProfileViewModel.saveProfileChanges(customerId, newName, newPhone);
    }

    private void sendRefreshResult() {
        getParentFragmentManager().setFragmentResult("profile_refresh_key", new Bundle());
    }

    private void proceedAvatarActionsAfterProfileUpdate() {
        if (currentUser == null || currentUser.getCustomerId() == null || currentUser.getId() == null) {
            Toast.makeText(getContext(), "Profil berhasil diperbarui.", Toast.LENGTH_SHORT).show();
            sendRefreshResult();
            getParentFragmentManager().popBackStack();
            return;
        }

        if (pendingAvatarUri != null) {
            editProfileViewModel.uploadAvatar(currentUser.getCustomerId(), currentUser.getId(), pendingAvatarUri);
            pendingAvatarUri = null;
            return;
        }
        if (pendingDeleteAvatar) {
            editProfileViewModel.deleteAvatar(currentUser.getId());
            pendingDeleteAvatar = false;
            return;
        }

        Toast.makeText(getContext(), "Profil berhasil diperbarui.", Toast.LENGTH_SHORT).show();
        sendRefreshResult();
        getParentFragmentManager().popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }
}
