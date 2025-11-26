package com.project.inet_mobile.ui.payment;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;

import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.project.inet_mobile.R;
import com.project.inet_mobile.LoginActivity;
import com.project.inet_mobile.data.auth.AuthSession;
import com.project.inet_mobile.data.payment.PaymentRemoteDataSource;
import com.project.inet_mobile.data.session.TokenStorage;
import com.project.inet_mobile.data.remote.dto.ApiResponse;
import com.project.inet_mobile.data.remote.dto.CheckoutResponseData;
import com.project.inet_mobile.data.remote.dto.InvoiceDetailResponseData;
import com.project.inet_mobile.data.remote.dto.PaymentStatusResponseData;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PembayaranFragment extends Fragment {

    private static final String ARG_INVOICE_ID = "arg_invoice_id";
    private static final String DEFAULT_RETURN_URL = "inet://payment-result";

    private final Map<Integer, String> channelMap = new ConcurrentHashMap<>();

    private MaterialCardView selectedCard;
    private MaterialButton btnSnapPay;
    private TextView tvTotalPembayaran;
    private TextView tvNoInvoice;
    private TextView tvItemDescription;
    private TextView tvDeadline;
    private TextView tvStatusLabel;
    private TextView tvSnapStatus;
    private ProgressBar snapProgressBar;
    private CardView statusBannerCard;

    private PaymentRemoteDataSource paymentDataSource;
    private TokenStorage tokenStorage;
    private long invoiceId = -1L;
    private Long activePaymentId;
    private boolean awaitingStatusAfterSnap = false;
    private Call<?> ongoingCall;
    private InvoiceDetailResponseData currentInvoiceDetail;

    public PembayaranFragment() {
        super(R.layout.fragment_pembayaran);
    }

    public static PembayaranFragment newInstance(long invoiceId) {
        PembayaranFragment fragment = new PembayaranFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_INVOICE_ID, invoiceId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            invoiceId = args.getLong(ARG_INVOICE_ID, -1L);
        }
        channelMap.put(R.id.cardQris, "qris");
        channelMap.put(R.id.cardBankBRI, "bank_transfer_bri");
        channelMap.put(R.id.cardBankBCA, "bank_transfer_bca");
        channelMap.put(R.id.cardBankMandiri, "bank_transfer_mandiri");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        paymentDataSource = new PaymentRemoteDataSource(context);
        tokenStorage = new TokenStorage(context.getApplicationContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupPaymentCards(view);

        if (!ensureLoggedIn()) {
            return;
        }

        fetchInvoiceDetail();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideBottomNavigation();
        if (awaitingStatusAfterSnap && activePaymentId != null) {
            pollPaymentStatus();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        showBottomNavigation();
    }

    @Override
    public void onStop() {
        super.onStop();
        showBottomNavigation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        showBottomNavigation();
        if (ongoingCall != null && !ongoingCall.isCanceled()) {
            ongoingCall.cancel();
        }
        ongoingCall = null;
    }

    private void setupViews(@NonNull View root) {
        btnSnapPay = root.findViewById(R.id.btnSnapPay);
        tvTotalPembayaran = root.findViewById(R.id.tvTotalPembayaran);
        tvNoInvoice = root.findViewById(R.id.tvNoInvoice);
        tvItemDescription = root.findViewById(R.id.tvItem);
        tvDeadline = root.findViewById(R.id.tvDeadline);
        tvStatusLabel = root.findViewById(R.id.tvStatusLabel);
        tvSnapStatus = root.findViewById(R.id.tvSnapStatus);
        snapProgressBar = root.findViewById(R.id.snapProgressBar);
        statusBannerCard = root.findViewById(R.id.cardStatusBanner);
        View backIcon = root.findViewById(R.id.backIcon);
        if (backIcon != null) {
            backIcon.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
        btnSnapPay.setOnClickListener(v -> initiateCheckout());
        updatePayButtonState();
    }

    private boolean ensureLoggedIn() {
        AuthSession session = tokenStorage.getSession();
        if (session == null) {
            showToast("Sesi login berakhir, silakan login ulang");
            redirectToLogin();
            return false;
        }
        if (session.isExpired()) {
            showToast("Token kedaluwarsa, mohon login kembali");
            redirectToLogin();
            return false;
        }
        return true;
    }

    private void redirectToLogin() {
        Context context = requireContext();
        LoginActivity.logout((androidx.appcompat.app.AppCompatActivity) requireActivity());
    }

    private void setupPaymentCards(@NonNull View rootView) {
        for (Integer cardId : channelMap.keySet()) {
            MaterialCardView cardView = rootView.findViewById(cardId);
            if (cardView != null) {
                cardView.setCheckable(true);
                cardView.setChecked(false);
                cardView.setOnClickListener(v -> selectPaymentCard(cardView));
            }
        }
    }

    private void fetchInvoiceDetail() {
        if (invoiceId <= 0) {
            fetchFirstPayableInvoice();
            return;
        }

        requestInvoiceDetail(invoiceId);
    }

    private void fetchFirstPayableInvoice() {
        setLoadingState(true, "Mencari tagihan yang dapat dibayar...");
        Call<ApiResponse<InvoiceDetailResponseData>> fallbackDetailCall = null;
        requestInvoiceList("issued", new InvoiceListCallback() {
            @Override
            public void onFound(long foundInvoiceId) {
                invoiceId = foundInvoiceId;
                requestInvoiceDetail(foundInvoiceId);
            }

            @Override
            public void onNotFound() {
                requestInvoiceList("overdue", new InvoiceListCallback() {
                    @Override
                    public void onFound(long foundInvoiceId) {
                        invoiceId = foundInvoiceId;
                        requestInvoiceDetail(foundInvoiceId);
                    }

                    @Override
                    public void onNotFound() {
                        // Coba ambil invoice apa pun (misal sudah paid) supaya UI tetap informatif
                        requestInvoiceList(null, new InvoiceListCallback() {
                            @Override
                            public void onFound(long foundInvoiceId) {
                                invoiceId = foundInvoiceId;
                                requestInvoiceDetail(foundInvoiceId);
                            }

                            @Override
                            public void onNotFound() {
                                setLoadingState(false, null);
                                btnSnapPay.setEnabled(false);
                                updateStatus("Tidak ada tagihan aktif. Semua tagihan sudah lunas.");
                            }

                            @Override
                            public void onError(String message) {
                                setLoadingState(false, null);
                                btnSnapPay.setEnabled(false);
                                updateStatus("Invoice tidak tersedia");
                                showToast(message);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        setLoadingState(false, null);
                        btnSnapPay.setEnabled(false);
                        updateStatus("Invoice tidak tersedia");
                        showToast(message);
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoadingState(false, null);
                btnSnapPay.setEnabled(false);
                updateStatus("Invoice tidak tersedia");
                showToast(message);
            }
        });
    }

    private void requestInvoiceList(@NonNull String status, @NonNull InvoiceListCallback callback) {
        Call<ApiResponse<com.project.inet_mobile.data.remote.dto.InvoiceListResponseData>> call =
                paymentDataSource.getInvoices(1, 0, status);
        ongoingCall = call;
        call.enqueue(new Callback<ApiResponse<com.project.inet_mobile.data.remote.dto.InvoiceListResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<com.project.inet_mobile.data.remote.dto.InvoiceListResponseData>> call,
                                   @NonNull Response<ApiResponse<com.project.inet_mobile.data.remote.dto.InvoiceListResponseData>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Gagal memuat daftar invoice");
                    return;
                }
                ApiResponse<com.project.inet_mobile.data.remote.dto.InvoiceListResponseData> body = response.body();
                if (!body.isSuccess() || body.getData() == null || body.getData().getItems() == null || body.getData().getItems().isEmpty()) {
                    callback.onNotFound();
                    return;
                }
                long foundId = body.getData().getItems().get(0).getInvoiceId();
                callback.onFound(foundId);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<com.project.inet_mobile.data.remote.dto.InvoiceListResponseData>> call,
                                  @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    callback.onError("Tidak dapat memuat daftar invoice: " + t.getMessage());
                }
            }
        });
    }

    private void requestInvoiceDetail(long id) {
        setLoadingState(true, "Memuat detail invoice...");
        Call<ApiResponse<InvoiceDetailResponseData>> call = paymentDataSource.getInvoiceDetail(id);
        ongoingCall = call;
        call.enqueue(new Callback<ApiResponse<InvoiceDetailResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<InvoiceDetailResponseData>> call,
                                   @NonNull Response<ApiResponse<InvoiceDetailResponseData>> response) {
                setLoadingState(false, null);
                if (!response.isSuccessful() || response.body() == null) {
                    showToast("Gagal memuat invoice");
                    btnSnapPay.setEnabled(false);
                    updateStatus("Invoice tidak tersedia");
                    return;
                }

                ApiResponse<InvoiceDetailResponseData> body = response.body();
                if (!body.isSuccess() || body.getData() == null) {
                    showToast(body.getMessage() != null ? body.getMessage() : "Invoice tidak ditemukan");
                    btnSnapPay.setEnabled(false);
                    updateStatus("Invoice tidak tersedia");
                    return;
                }

                currentInvoiceDetail = body.getData();
                bindInvoiceDetail(currentInvoiceDetail);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<InvoiceDetailResponseData>> call,
                                  @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    setLoadingState(false, null);
                    showToast("Tidak dapat memuat invoice: " + t.getMessage());
                    btnSnapPay.setEnabled(false);
                    updateStatus("Invoice tidak tersedia");
                }
            }
        });
    }

    private void selectPaymentCard(@NonNull MaterialCardView cardView) {
        if (selectedCard != null) {
            selectedCard.setChecked(false);
        }
        cardView.setChecked(true);
        selectedCard = cardView;
        updatePayButtonState();
    }

    private void initiateCheckout() {
        if (currentInvoiceDetail == null || !currentInvoiceDetail.isCanPay()) {
            showToast("Invoice tidak dapat dibayar");
            return;
        }
        if (selectedCard == null) {
            showToast("Pilih metode pembayaran terlebih dahulu");
            return;
        }

        String channel = channelMap.get(selectedCard.getId());
        if (TextUtils.isEmpty(channel)) {
            showToast("Metode pembayaran belum tersedia");
            return;
        }

        setLoadingState(true, "Membuat sesi pembayaran...");
        Call<ApiResponse<CheckoutResponseData>> call = paymentDataSource.checkout(
                invoiceId,
                channel,
                DEFAULT_RETURN_URL
        );
        ongoingCall = call;
        call.enqueue(new Callback<ApiResponse<CheckoutResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<CheckoutResponseData>> call,
                                   @NonNull Response<ApiResponse<CheckoutResponseData>> response) {
                setLoadingState(false, null);
                if (!response.isSuccessful() || response.body() == null) {
                    showToast("Checkout gagal. Coba lagi.");
                    return;
                }

                ApiResponse<CheckoutResponseData> body = response.body();
                if (!body.isSuccess() || body.getData() == null) {
                    showToast(body.getMessage() != null ? body.getMessage() : "Checkout gagal");
                    return;
                }

                CheckoutResponseData data = body.getData();
                activePaymentId = data.getPaymentId();
                awaitingStatusAfterSnap = true;
                updateStatus("Menunggu pembayaran melalui Midtrans");
                openSnapRedirect(data.getRedirectUrl());
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<CheckoutResponseData>> call,
                                  @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    setLoadingState(false, null);
                    showToast("Jaringan bermasalah: " + t.getMessage());
                }
            }
        });
    }

    private void openSnapRedirect(@Nullable String redirectUrl) {
        if (TextUtils.isEmpty(redirectUrl)) {
            showToast("Link pembayaran tidak tersedia");
            return;
        }
        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build();
        intent.launchUrl(requireContext(), Uri.parse(redirectUrl));
    }

    private void pollPaymentStatus() {
        if (activePaymentId == null) {
            return;
        }

        setLoadingState(true, "Memeriksa status pembayaran...");
        Call<ApiResponse<PaymentStatusResponseData>> call = paymentDataSource.getPaymentStatus(invoiceId, activePaymentId);
        ongoingCall = call;
        call.enqueue(new Callback<ApiResponse<PaymentStatusResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<PaymentStatusResponseData>> call,
                                   @NonNull Response<ApiResponse<PaymentStatusResponseData>> response) {
                setLoadingState(false, null);
                awaitingStatusAfterSnap = false;

                if (!response.isSuccessful() || response.body() == null) {
                    updateStatus("Status pembayaran tidak tersedia");
                    showToast("Gagal memeriksa status pembayaran");
                    return;
                }

                ApiResponse<PaymentStatusResponseData> body = response.body();
                if (!body.isSuccess() || body.getData() == null) {
                    updateStatus("Status pembayaran tidak tersedia");
                    showToast(body.getMessage() != null ? body.getMessage() : "Status tidak tersedia");
                    return;
                }

                handleStatusResult(body.getData());
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<PaymentStatusResponseData>> call,
                                  @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    setLoadingState(false, null);
                    updateStatus("Status pembayaran tidak tersedia");
                    showToast("Tidak dapat memeriksa status: " + t.getMessage());
                }
            }
        });
    }

    private void handleStatusResult(@NonNull PaymentStatusResponseData data) {
        PaymentStatusResponseData.PaymentInfo payment = data.getPayment();
        if (payment == null) {
            updateStatus("Status pembayaran tidak tersedia");
            return;
        }

        String status = payment.getStatus();
        if ("settlement".equalsIgnoreCase(status)) {
            updateStatus("Pembayaran berhasil. Terima kasih!");
            // Mark invoice as paid locally to prevent re-checkout, then refresh from server
            if (currentInvoiceDetail != null) {
                currentInvoiceDetail = new InvoiceDetailResponseDataWrapper(currentInvoiceDetail, "paid", false);
            }
            updatePayButtonState();
            requestInvoiceDetail(invoiceId);
        } else if ("pending".equalsIgnoreCase(status)) {
            updateStatus("Pembayaran masih menunggu. Cek instruksi Midtrans.");
        } else if ("expire".equalsIgnoreCase(status) || "cancel".equalsIgnoreCase(status)) {
            updateStatus("Transaksi berakhir (" + status + "). Silakan pilih ulang metode.");
            activePaymentId = null;
            awaitingStatusAfterSnap = false;
            updatePayButtonState();
        } else {
            updateStatus("Status pembayaran: " + status);
        }
    }

    private void bindInvoiceDetail(@NonNull InvoiceDetailResponseData detail) {
        if (tvTotalPembayaran != null) {
            tvTotalPembayaran.setText(formatCurrency(detail.getAmount()));
        }
        if (tvNoInvoice != null) {
            tvNoInvoice.setText(detail.getInvoiceNumber());
        }
        if (tvItemDescription != null) {
            tvItemDescription.setText(detail.getDescription());
        }
        updateStatusBanner(detail);

        String status = detail.getStatus();
        boolean isIssued = "issued".equalsIgnoreCase(status);
        if (!isIssued) {
            updateStatus(getStatusMessage(status));
            btnSnapPay.setEnabled(false);
        } else if (!detail.isCanPay()) {
            updateStatus("Invoice ini tidak dapat dibayar");
            btnSnapPay.setEnabled(false);
        } else {
            updateStatus("Silakan pilih metode pembayaran");
            updatePayButtonState();
        }

        InvoiceDetailResponseData.LatestPayment latestPayment = detail.getLatestPayment();
        if (latestPayment != null && "pending".equalsIgnoreCase(latestPayment.getStatus())) {
            activePaymentId = latestPayment.getPaymentId();
            awaitingStatusAfterSnap = true;
            updateStatus("Ada pembayaran pending, memeriksa status...");
            pollPaymentStatus();
        }
    }

    private void setLoadingState(boolean loading, @Nullable String statusMessage) {
        if (snapProgressBar != null) {
            snapProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (loading && currentInvoiceDetail == null) {
            if (tvTotalPembayaran != null) {
                tvTotalPembayaran.setText("Memuat...");
            }
            if (tvNoInvoice != null) {
                tvNoInvoice.setText("-");
            }
            if (tvItemDescription != null) {
                tvItemDescription.setText("");
            }
            if (tvDeadline != null) {
                tvDeadline.setText("-");
            }
        }
        if (statusMessage != null) {
            updateStatus(statusMessage);
        }
        updatePayButtonState();
    }

    private boolean isLoading() {
        return snapProgressBar != null && snapProgressBar.getVisibility() == View.VISIBLE;
    }

    private void updatePayButtonState() {
        if (btnSnapPay != null) {
            boolean canPay = isInvoicePayable();
            btnSnapPay.setEnabled(!isLoading() && selectedCard != null && canPay);
        }
    }

    private void updateStatus(@NonNull String message) {
        if (tvSnapStatus != null) {
            tvSnapStatus.setText(message);
        }
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Wrapper sederhana untuk menandai invoice sudah paid secara lokal
     * sebelum refresh dari server, supaya tombol tidak aktif dan status berubah.
     */
    private static class InvoiceDetailResponseDataWrapper extends InvoiceDetailResponseData {
        private final InvoiceDetailResponseData delegate;
        private final String overrideStatus;
        private final boolean overrideCanPay;

        InvoiceDetailResponseDataWrapper(InvoiceDetailResponseData delegate, String status, boolean canPay) {
            this.delegate = delegate;
            this.overrideStatus = status;
            this.overrideCanPay = canPay;
        }

        @Override
        public String getStatus() {
            return overrideStatus;
        }

        @Override
        public boolean isCanPay() {
            return overrideCanPay;
        }

        // Delegate other getters
        @Override public long getInvoiceId() { return delegate.getInvoiceId(); }
        @Override public String getInvoiceNumber() { return delegate.getInvoiceNumber(); }
        @Override public String getMonthLabel() { return delegate.getMonthLabel(); }
        @Override public double getAmount() { return delegate.getAmount(); }
        @Override public String getDueDate() { return delegate.getDueDate(); }
        @Override public String getDescription() { return delegate.getDescription(); }
        @Override public String getCreatedAt() { return delegate.getCreatedAt(); }
        @Override public String getPaidAt() { return delegate.getPaidAt(); }
        @Override public boolean isOverdue() { return delegate.isOverdue(); }
        @Override public CustomerInfo getCustomer() { return delegate.getCustomer(); }
        @Override public java.util.List<PaymentMethod> getPaymentMethods() { return delegate.getPaymentMethods(); }
        @Override public LatestPayment getLatestPayment() { return delegate.getLatestPayment(); }
    }

    private boolean isInvoicePayable() {
        return currentInvoiceDetail != null
                && currentInvoiceDetail.isCanPay()
                && "issued".equalsIgnoreCase(currentInvoiceDetail.getStatus());
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String[] patterns = {
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd"
        };
        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
                parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = parser.parse(raw);
                if (date != null) {
                    SimpleDateFormat out = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    return out.format(date);
                }
            } catch (ParseException ignore) {
            }
        }
        return raw;
    }

    private String getStatusMessage(@Nullable String status) {
        if (status == null) {
            return "Status pembayaran tidak tersedia";
        }
        switch (status.toLowerCase()) {
            case "paid":
                return "Tagihan sudah lunas";
            case "overdue":
                return "Tagihan kedaluwarsa";
            case "cancelled":
                return "Tagihan dibatalkan";
            case "draft":
                return "Tagihan belum diterbitkan";
            default:
                return "Status pembayaran: " + status;
        }
    }

    private void updateStatusBanner(@NonNull InvoiceDetailResponseData detail) {
        String status = detail.getStatus();
        // Default colors: warning yellow
        int bgColor = Color.parseColor("#FFF9C4");
        int textColor = Color.parseColor("#F57F17");

        if ("paid".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#E8F5E9");
            textColor = Color.parseColor("#1B5E20");
            String paidAt = detail.getPaidAt();
            setStatusBannerContent("Tagihan sudah lunas",
                    (paidAt != null && !paidAt.isEmpty()) ? formatDate(paidAt) : "Terima kasih");
            applyStatusBannerColors(bgColor, textColor);
            return;
        }

        if ("overdue".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#FFF3E0");
            textColor = Color.parseColor("#E65100");
            setStatusBannerContent("Tagihan kedaluwarsa", detail.getDueDate());
            applyStatusBannerColors(bgColor, textColor);
            return;
        }

        if ("cancelled".equalsIgnoreCase(status)) {
            bgColor = Color.parseColor("#ECEFF1");
            textColor = Color.parseColor("#37474F");
            setStatusBannerContent("Tagihan dibatalkan", "-");
            applyStatusBannerColors(bgColor, textColor);
            return;
        }

        // Default: issued/draft
        setStatusBannerContent("Selesaikan pembayaran sebelum", formatDate(detail.getDueDate()));
        applyStatusBannerColors(bgColor, textColor);
    }

    private void setStatusBannerContent(String title, String value) {
        if (tvStatusLabel != null) {
            tvStatusLabel.setText(title);
        }
        if (tvDeadline != null) {
            tvDeadline.setText(value);
        }
    }

    private void applyStatusBannerColors(int bgColor, int textColor) {
        if (statusBannerCard != null) {
            statusBannerCard.setCardBackgroundColor(bgColor);
        }
        if (tvStatusLabel != null) {
            tvStatusLabel.setTextColor(textColor);
        }
        if (tvDeadline != null) {
            tvDeadline.setTextColor(textColor);
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        return format.format(amount).replace(",00", "");
    }

    /**
     * Sembunyikan Bottom Navigation saat masuk pembayaran
     */
    private void hideBottomNavigation() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.dashboardBottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Tampilkan Bottom Navigation saat keluar dari pembayaran
     */
    private void showBottomNavigation() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.dashboardBottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        }
    }

    interface InvoiceListCallback {
        void onFound(long foundInvoiceId);

        void onNotFound();

        void onError(String message);
    }
}
