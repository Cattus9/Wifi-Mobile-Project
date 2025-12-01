package com.project.inet_mobile.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.project.inet_mobile.data.auth.AuthException;
import com.project.inet_mobile.data.auth.AuthRepository;
import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.databinding.FragmentForgotPasswordBinding;
import com.project.inet_mobile.util.conn;

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private AuthRepository authRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize AuthRepository, it's lightweight.
        authRepository = new AuthRepository(new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey()));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setListeners();
    }

    private void setListeners() {
        binding.ivBack.setOnClickListener(v -> {
            if (!isAdded()) return;
            requireActivity().finish();
        });

        binding.buttonSendOtp.setOnClickListener(v -> {
            sendOtp();
        });
    }

    private void sendOtp() {
        if (!isAdded()) return;

        String email = binding.editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.editTextEmail.setError("Email is required");
            binding.editTextEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.setError("Please enter a valid email");
            binding.editTextEmail.requestFocus();
            return;
        }

        setLoading(true);

        authRepository.sendPasswordResetOtp(email, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                // Ensure fragment is still added and has a view
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "OTP sent to " + email, Toast.LENGTH_LONG).show();

                    // Navigate to VerifyOtpFragment, passing the email
                    ForgotPasswordFragmentDirections.ActionForgotPasswordFragmentToVerifyOtpFragment action =
                            ForgotPasswordFragmentDirections.actionForgotPasswordFragmentToVerifyOtpFragment(email);
                    NavHostFragment.findNavController(ForgotPasswordFragment.this).navigate(action);
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
        if (!isAdded()) return;
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSendOtp.setEnabled(false);
            binding.editTextEmail.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonSendOtp.setEnabled(true);
            binding.editTextEmail.setEnabled(true);
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