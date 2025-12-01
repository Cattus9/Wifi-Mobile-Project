package com.project.inet_mobile.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.project.inet_mobile.R;
import com.project.inet_mobile.data.auth.AuthException;
import com.project.inet_mobile.data.auth.AuthRepository;
import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.auth.SupabaseAuthService;
import com.project.inet_mobile.databinding.FragmentVerifyOtpBinding;
import com.project.inet_mobile.util.conn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VerifyOtpFragment extends Fragment {

    private FragmentVerifyOtpBinding binding;
    private List<EditText> otpEditTexts;
    private String userEmail;
    private AuthRepository authRepository;
    private CountDownTimer resendTimer;
    private boolean isTimerRunning = false;

    private static final long TIMER_DURATION = 60000; // 60 seconds

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authRepository = new AuthRepository(new SupabaseAuthService(conn.getSupabaseUrl(), conn.getSupabaseKey()));
        if (getArguments() != null) {
            userEmail = VerifyOtpFragmentArgs.fromBundle(getArguments()).getEmail();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVerifyOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initOtpEditTexts();
        setupListeners();
        startResendTimer();

        if (userEmail != null && !userEmail.isEmpty()) {
            binding.tvOtpSubtitle.setText(String.format("Masukkan 6 digit kode yang dikirim ke email\n%s", userEmail));
        }
    }

    private void initOtpEditTexts() {
        otpEditTexts = new ArrayList<>();
        otpEditTexts.add(binding.otpEditText1);
        otpEditTexts.add(binding.otpEditText2);
        otpEditTexts.add(binding.otpEditText3);
        otpEditTexts.add(binding.otpEditText4);
        otpEditTexts.add(binding.otpEditText5);
        otpEditTexts.add(binding.otpEditText6);
    }

        private void setupListeners() {

            binding.ivBack.setOnClickListener(v -> {

                if (isAdded()) NavHostFragment.findNavController(this).navigateUp();

            });

    

            for (int i = 0; i < otpEditTexts.size(); i++) {

                final int currentIndex = i;

                otpEditTexts.get(i).addTextChangedListener(new TextWatcher() {

                    @Override

                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    

                    @Override

                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                        if (s.length() == 1 && currentIndex < otpEditTexts.size() - 1) {

                            otpEditTexts.get(currentIndex + 1).requestFocus();

                        }

                    }

    

                    @Override

                    public void afterTextChanged(Editable s) {}

                });

    

                // Add KeyListener for backspace

                if (currentIndex > 0) {

                    otpEditTexts.get(currentIndex).setOnKeyListener((v, keyCode, event) -> {

                        if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {

                            if (otpEditTexts.get(currentIndex).getText().toString().isEmpty()) {

                                // If current box is empty, delete from previous box

                                otpEditTexts.get(currentIndex - 1).setText("");

                                otpEditTexts.get(currentIndex - 1).requestFocus();

                                return true;

                            }

                        }

                        return false;

                    });

                }

            }

    

            binding.buttonVerifyOtp.setOnClickListener(v -> {

                String otp = getEnteredOtp();

                if (otp.length() == 6) {

                    verifyOtp(otp);

                } else {

                    Toast.makeText(getContext(), "Please enter all 6 digits.", Toast.LENGTH_SHORT).show();

                }

            });

    

            binding.tvResendOtp.setOnClickListener(v -> {

                if (!isTimerRunning) {

                    resendOtp();

                }

            });

        }

    private void startResendTimer() {
        isTimerRunning = true;
        binding.tvResendOtp.setEnabled(false);
        binding.tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.clrTextSecondary));

        resendTimer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if(isAdded()) {
                    long seconds = millisUntilFinished / 1000;
                    binding.tvResendOtp.setText(String.format(Locale.getDefault(), "Kirim Ulang (%ds)", seconds));
                }
            }

            @Override
            public void onFinish() {
                if(isAdded()) {
                    isTimerRunning = false;
                    binding.tvResendOtp.setEnabled(true);
                    binding.tvResendOtp.setText("Kirim ulang");
                    binding.tvResendOtp.setTextColor(ContextCompat.getColor(requireContext(), R.color.register_primary));
                }
            }
        }.start();
    }
    
    private void resendOtp(){
        if(userEmail == null || userEmail.isEmpty()){
            Toast.makeText(getContext(), "Error: Email tidak ditemukan.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        startResendTimer();
        setLoading(true);
        authRepository.sendPasswordResetOtp(userEmail, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess() {
                if(isAdded()) getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "OTP telah dikirim ulang.", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onError(AuthException exception) {
                if(isAdded()) getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "Gagal mengirim ulang OTP: " + exception.getUserMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void verifyOtp(String otp) {
        if (!isAdded() || userEmail == null) return;
        setLoading(true);
        authRepository.verifyOtp(userEmail, otp, new AuthRepository.VerifyOtpCallback() {
            @Override
            public void onSuccess(AuthSession session) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getContext(), "OTP Verified Successfully!", Toast.LENGTH_SHORT).show();
                    String accessToken = session.getAccessToken();
                    VerifyOtpFragmentDirections.ActionVerifyOtpFragmentToResetPasswordFragment action = 
                            VerifyOtpFragmentDirections.actionVerifyOtpFragmentToResetPasswordFragment(accessToken);
                    NavHostFragment.findNavController(VerifyOtpFragment.this).navigate(action);
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

    private String getEnteredOtp() {
        StringBuilder sb = new StringBuilder();
        for (EditText et : otpEditTexts) {
            sb.append(et.getText().toString());
        }
        return sb.toString();
    }

    private void setLoading(boolean isLoading) {
        if (binding == null) return;
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonVerifyOtp.setEnabled(false);
            binding.tvResendOtp.setEnabled(false);
            for (EditText et : otpEditTexts) {
                et.setEnabled(false);
            }
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonVerifyOtp.setEnabled(true);
            binding.tvResendOtp.setEnabled(true);
            for (EditText et : otpEditTexts) {
                et.setEnabled(true);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
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