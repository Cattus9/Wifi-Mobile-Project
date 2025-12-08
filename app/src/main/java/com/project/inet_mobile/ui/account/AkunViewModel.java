package com.project.inet_mobile.ui.account;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.project.inet_mobile.data.auth.User;
import com.project.inet_mobile.data.auth.UserRepository;
import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.session.TokenStorage; // Import TokenStorage

public class AkunViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    private final MutableLiveData<User> _userProfile = new MutableLiveData<>();
    public LiveData<User> userProfile = _userProfile;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    public AkunViewModel(@NonNull Application application, @NonNull UserRepository userRepository) {
        super(application);
        this.userRepository = userRepository;
    }

    public void fetchUserProfile() {
        _isLoading.setValue(true);
        _errorMessage.setValue(null); // Bersihkan error sebelumnya

        userRepository.getUserProfile(new UserRepository.UserProfileCallback() {
            @Override
            public void onSuccess(User user) {
                _userProfile.setValue(user);
                _isLoading.setValue(false);
            }

            @Override
            public void onError(String message) {
                _errorMessage.setValue(message);
                _isLoading.setValue(false);
            }
        });
    }

    // Factory untuk AkunViewModel agar bisa menangani dependensi
    public static class AkunViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final UserRepository userRepository;

        public AkunViewModelFactory(@NonNull Application application) {
            this.application = application;
            // Membangun UserRepository dengan dependensi yang benar (TokenStorage)
            TokenStorage tokenStorage = new TokenStorage(application.getApplicationContext());
                    this.userRepository = new UserRepository(SupabaseApiClient.getSupabaseUserService(), tokenStorage);
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(AkunViewModel.class)) {
                return (T) new AkunViewModel(application, userRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
