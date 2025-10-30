package com.project.inet_mobile.data.auth;

import androidx.annotation.Nullable;

public class UserProfile {
    private final long userId;
    private final String email;
    @Nullable
    private final Long customerId;
    private final String displayName;

    public UserProfile(long userId,
                       String email,
                       @Nullable Long customerId,
                       String displayName) {
        this.userId = userId;
        this.email = email;
        this.customerId = customerId;
        this.displayName = displayName;
    }

    public long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    @Nullable
    public Long getCustomerId() {
        return customerId;
    }

    public String getDisplayName() {
        return displayName;
    }
}
