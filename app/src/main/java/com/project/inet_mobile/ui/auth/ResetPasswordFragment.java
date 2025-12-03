package com.project.inet_mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.project.inet_mobile.LoginActivity;
import com.project.inet_mobile.data.auth.AuthException;
import com.project.inet_mobile.data.auth.AuthRepository;
import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.databinding.FragmentResetPasswordBinding;
import com.project.inet_mobile.util.conn;

public class ResetPasswordFragment extends Fragment {

    private FragmentResetPasswordBinding binding;
    private AuthRepository authRepository;
    private String accessToken;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository(new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey()));
        if (getArguments() != null) {
            accessToken = ResetPasswordFragmentArgs.fromBundle(getArguments()).getAccessToken();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResetPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonSavePassword.setOnClickListener(v -> {
            updatePassword();
        });
    }

    private void updatePassword() {
        if (!isAdded() || accessToken == null || accessToken.isEmpty()) {
            Toast.makeText(getContext(), "Error: Otorisasi tidak valid.", Toast.LENGTH_SHORT).show();
            return;
        }

        String newPassword = binding.etNewPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 6) {
            binding.tilNewPassword.setError("Password minimal 6 karakter.");
            binding.etNewPassword.requestFocus();
            return;
        } else {
            binding.tilNewPassword.setError(null);
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Password tidak cocok.");
            binding.etConfirmPassword.requestFocus();
            return;
        } else {
            binding.tilConfirmPassword.setError(null);
        }

        setLoading(true);

        authRepository.updateUserPassword(accessToken, newPassword, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Password berhasil diubah. Silakan login kembali.", Toast.LENGTH_LONG).show();

                    // Navigate back to LoginActivity
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    getActivity().finish();
                });
            }

            @Override
            public void onError(AuthException exception) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Error: " + exception.getUserMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean isLoading) {
        if (binding == null) return;
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSavePassword.setEnabled(false);
            binding.etNewPassword.setEnabled(false);
            binding.etConfirmPassword.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonSavePassword.setEnabled(true);
            binding.etNewPassword.setEnabled(true);
            binding.etConfirmPassword.setEnabled(true);
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (authRepository != null) {
            authRepository.shutdown();
        }
    }
}
