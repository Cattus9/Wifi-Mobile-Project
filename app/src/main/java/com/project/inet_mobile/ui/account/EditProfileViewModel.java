package com.project.inet_mobile.ui.account;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.auth.User;
import com.project.inet_mobile.data.auth.UserRepository;
import com.project.inet_mobile.data.profile.AvatarRepository;
import com.project.inet_mobile.data.remote.SupabaseApiClient;
import com.project.inet_mobile.data.session.TokenStorage;

public class EditProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final TokenStorage tokenStorage;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<Boolean> _isUpdateSuccess = new MutableLiveData<>(false);
    public LiveData<Boolean> isUpdateSuccess = _isUpdateSuccess;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _avatarUrl = new MutableLiveData<>();
    public LiveData<String> avatarUrl = _avatarUrl;

    public EditProfileViewModel(@NonNull Application application, @NonNull UserRepository userRepository) {
        super(application);
        this.userRepository = userRepository;
        this.avatarRepository = new AvatarRepository(application.getApplicationContext());
        this.tokenStorage = new TokenStorage(application.getApplicationContext());
    }

    public void saveProfileChanges(Long customerId, String newName, String newPhone) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        _isUpdateSuccess.setValue(false);

        if (customerId == null) {
            _errorMessage.setValue("Customer ID is missing. Cannot update profile.");
            _isLoading.setValue(false);
            return;
        }

        userRepository.updateCustomerDetails(customerId, newName, newPhone, new UserRepository.UpdateCallback() {
            @Override
            public void onSuccess() {
                _isLoading.setValue(false);
                _isUpdateSuccess.setValue(true);
            }

            @Override
            public void onError(String message) {
                _isLoading.setValue(false);
                _errorMessage.setValue(message);
            }
        });
    }

    public void uploadAvatar(long customerId, long userId, android.net.Uri imageUri) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        avatarRepository.uploadAvatar(customerId, userId, imageUri, new AvatarRepository.CallbackResult() {
            @Override
            public void onSuccess(String publicUrl) {
                _isLoading.postValue(false);
                _avatarUrl.postValue(publicUrl);
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue(message);
            }
        });
    }

    public void deleteAvatar(long userId) {
        _isLoading.setValue(true);
        _errorMessage.setValue(null);
        avatarRepository.deleteAvatar(userId, new AvatarRepository.CallbackResult() {
            @Override
            public void onSuccess(String publicUrl) {
                _isLoading.postValue(false);
                _avatarUrl.postValue("");
            }

            @Override
            public void onError(String message) {
                _isLoading.postValue(false);
                _errorMessage.postValue(message);
            }
        });
    }

    // Factory for EditProfileViewModel
    public static class EditProfileViewModelFactory implements ViewModelProvider.Factory {
        private final Application application;
        private final UserRepository userRepository;
        private final TokenStorage tokenStorage;

        public EditProfileViewModelFactory(@NonNull Application application) {
            this.application = application;
            TokenStorage tokenStorage = new TokenStorage(application.getApplicationContext());
            this.userRepository = new UserRepository(
                SupabaseApiClient.getSupabaseUserService(),
                tokenStorage
            );
            this.tokenStorage = tokenStorage;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(EditProfileViewModel.class)) {
                return (T) new EditProfileViewModel(application, userRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
