package com.project.inet_mobile.data.remote.dto;

import androidx.annotation.Nullable;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    @Nullable
    private T data;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Nullable
    public T getData() {
        return data;
    }
}
