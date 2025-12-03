package com.project.inet_mobile.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Response DTO dari Supabase Edge Function: change-package
 *
 * Success Response Format:
 * {
 *   "success": true,
 *   "data": {
 *     "success": true,
 *     "ticket_id": 123,
 *     "status": "pending",
 *     "current_package": "Paket Basic",
 *     "requested_package": "Paket Premium",
 *     "notes": "...",
 *     "message": "..."
 *   }
 * }
 *
 * Error Response Format:
 * {
 *   "success": false,
 *   "error_code": "PENDING_REQUEST",
 *   "message": "PENDING_REQUEST: Masih ada permintaan aktif..."
 * }
 */
public class SupabaseChangePackageResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("data")
    private ChangePackageData data;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("message")
    private String message;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public ChangePackageData getData() {
        return data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Inner class untuk nested "data" object dari RPC function result
     */
    public static class ChangePackageData {
        @SerializedName("success")
        private boolean success;

        @SerializedName("ticket_id")
        private long ticketId;

        @SerializedName("status")
        private String status;

        @SerializedName("current_package")
        private String currentPackage;

        @SerializedName("requested_package")
        private String requestedPackage;

        @SerializedName("notes")
        private String notes;

        @SerializedName("message")
        private String message;

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public long getTicketId() {
            return ticketId;
        }

        public String getStatus() {
            return status;
        }

        public String getCurrentPackage() {
            return currentPackage;
        }

        public String getRequestedPackage() {
            return requestedPackage;
        }

        public String getNotes() {
            return notes;
        }

        public String getMessage() {
            return message;
        }
    }
}
