package com.project.inet_mobile.data.auth;

/**
 * Generic exception wrapper for Supabase authentication failures.
 */
public class AuthException extends Exception {

    private final int statusCode;
    private final String errorType;

    public AuthException(int statusCode, String errorType, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorType = errorType;
    }

    public AuthException(int statusCode, String message) {
        this(statusCode, null, message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.errorType = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorType() {
        return errorType;
    }

    /**
     * Maps the exception into a user-friendly message.
     */
    public String getUserMessage() {
        String message = getMessage();
        if (message == null || message.isEmpty()) {
            return "Terjadi kesalahan saat masuk. Coba lagi.";
        }

        String lower = message.toLowerCase();
        if (lower.contains("invalid") || lower.contains("wrong") || lower.contains("credentials")) {
            return "Email atau password salah";
        }
        if (lower.contains("email not confirmed")) {
            return "Email belum terverifikasi. Cek inbox Anda.";
        }
        if (statusCode == 0 || lower.contains("network")) {
            return "Tidak dapat terhubung ke server. Periksa koneksi Anda.";
        }
        return message;
    }
}
