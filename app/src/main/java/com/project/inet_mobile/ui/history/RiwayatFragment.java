package com.project.inet_mobile.ui.history;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.project.inet_mobile.R;
import com.project.inet_mobile.data.payment.PaymentRemoteDataSource;
import com.project.inet_mobile.data.remote.dto.ApiResponse;
import com.project.inet_mobile.data.remote.dto.InvoiceListResponseData;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RiwayatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private View emptyContainer;
    private TextView txtNominal;
    private TextView txtBulanLunas;
    private TextView txtInvoiceCount;
    private TextView txtStatus;
    private TextView txtTunggakan;
    private View chipContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    private RiwayatAdapter riwayatAdapter;
    private final List<PaymentHistoryItem> fullData = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
    private PaymentRemoteDataSource paymentDataSource;
    private boolean dataLoaded = false;

    public RiwayatFragment() {
        super(R.layout.fragment_riwayat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        paymentDataSource = new PaymentRemoteDataSource(requireContext());
        dataLoaded = false;
        loadHistoryFromApi();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerRiwayat);
        progressBar = view.findViewById(R.id.riwayatProgress);
        emptyContainer = view.findViewById(R.id.riwayatEmptyContainer);
        emptyView = view.findViewById(R.id.riwayatEmptyView);
        txtNominal = view.findViewById(R.id.txtNominal);
        txtBulanLunas = view.findViewById(R.id.txtBulanLunas);
        txtInvoiceCount = view.findViewById(R.id.txtInvoiceCount);
        txtStatus = view.findViewById(R.id.txtStatus);
        txtTunggakan = view.findViewById(R.id.txtTunggakan);
        chipContainer = view.findViewById(R.id.chipContainer);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        setupFilterChips();
        swipeRefreshLayout.setOnRefreshListener(this::loadHistoryFromApi);
    }

    private void setupRecyclerView() {
        riwayatAdapter = new RiwayatAdapter(item -> {
            // Hanya buka invoice untuk item yang sudah PAID
            if (item.getStatus().isPaid()) {
                openInvoiceActivity(item);
            } else {
                Toast.makeText(requireContext(), "Invoice belum dibayar", Toast.LENGTH_SHORT).show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(riwayatAdapter);
    }

    private void openInvoiceActivity(PaymentHistoryItem item) {
        android.content.Intent intent = new android.content.Intent(requireContext(), com.project.inet_mobile.util.InvoiceActivity.class);
        intent.putExtra("invoice_id", item.getInvoiceId());
        intent.putExtra("invoice_number", item.getInvoiceNumber());
        startActivity(intent);
    }

    private void loadHistoryFromApi() {
        if (!swipeRefreshLayout.isRefreshing()) {
            showLoading(true);
        }
        Call<ApiResponse<InvoiceListResponseData>> call = paymentDataSource.getInvoices(50, 0, null);
        call.enqueue(new Callback<ApiResponse<InvoiceListResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<InvoiceListResponseData>> call,
                                   @NonNull Response<ApiResponse<InvoiceListResponseData>> response) {
                showLoading(false);
                swipeRefreshLayout.setRefreshing(false);
                dataLoaded = true;
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    showEmpty(true);
                    bindSummaryPlaceholder();
                    Toast.makeText(requireContext(), "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<InvoiceListResponseData.InvoiceItem> items = response.body().getData().getItems();
                List<PaymentHistoryItem> mapped = mapInvoices(items);
                fullData.clear();
                fullData.addAll(mapped);
                applyFilter("all");
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<InvoiceListResponseData>> call,
                                  @NonNull Throwable t) {
                if (!call.isCanceled()) {
                    showLoading(false);
                    swipeRefreshLayout.setRefreshing(false);
                    dataLoaded = true;
                    showEmpty(true);
                    bindSummaryPlaceholder();
                    Toast.makeText(requireContext(), "Tidak dapat memuat riwayat: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private List<PaymentHistoryItem> mapInvoices(List<InvoiceListResponseData.InvoiceItem> items) {
        List<PaymentHistoryItem> mapped = new ArrayList<>();
        if (items == null) return mapped;
        for (InvoiceListResponseData.InvoiceItem inv : items) {
            PaymentHistoryItem.InvoiceStatus status = mapStatus(inv.getStatus());
            @ColorInt int bg;
            @ColorInt int textColor;
            switch (status) {
                case OVERDUE:
                    bg = ContextCompat.getColor(requireContext(), R.color.status_overdue_bg);
                    textColor = ContextCompat.getColor(requireContext(), R.color.white);
                    break;
                case DRAFT:
                    bg = ContextCompat.getColor(requireContext(), R.color.status_draft_bg);
                    textColor = ContextCompat.getColor(requireContext(), R.color.status_text_dark);
                    break;
                case PENDING:
                    bg = ContextCompat.getColor(requireContext(), R.color.status_draft_bg);
                    textColor = ContextCompat.getColor(requireContext(), R.color.status_text_dark);
                    break;
                case CANCELLED:
                    bg = ContextCompat.getColor(requireContext(), R.color.status_draft_bg);
                    textColor = ContextCompat.getColor(requireContext(), R.color.status_text_dark);
                    break;
                case PAID:
                default:
                    bg = ContextCompat.getColor(requireContext(), R.color.status_paid_bg);
                    textColor = ContextCompat.getColor(requireContext(), R.color.white);
                    break;
            }

            String date = inv.getPaidAt() != null ? inv.getPaidAt() : inv.getDueDate();
            mapped.add(new PaymentHistoryItem(
                    inv.getInvoiceId(),
                    inv.getMonthLabel() != null ? inv.getMonthLabel() : inv.getInvoiceNumber(),
                    date != null ? formatDate(date) : "",
                    "—",
                    inv.getInvoiceNumber(),
                    status,
                    formatCurrency(inv.getAmount()),
                    inv.getAmount(),
                    textColor,
                    bg
            ));
        }
        return mapped;
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

    private PaymentHistoryItem.InvoiceStatus mapStatus(String statusRaw) {
        if (statusRaw == null) return PaymentHistoryItem.InvoiceStatus.UNKNOWN;
        switch (statusRaw.toLowerCase()) {
            case "paid":
                return PaymentHistoryItem.InvoiceStatus.PAID;
            case "overdue":
                return PaymentHistoryItem.InvoiceStatus.OVERDUE;
            case "issued":
                return PaymentHistoryItem.InvoiceStatus.PENDING;
            case "cancelled":
                return PaymentHistoryItem.InvoiceStatus.CANCELLED;
            default:
                return PaymentHistoryItem.InvoiceStatus.UNKNOWN;
        }
    }

    private void bindSummary(List<PaymentHistoryItem> items) {
        if (!dataLoaded) {
            bindSummaryPlaceholder();
            return;
        }

        double totalPaid = 0;
        double outstanding = 0;
        String lastPaidMonth = "-";
        int paidCount = 0;
        int pendingCount = 0;
        int overdueCount = 0;

        for (PaymentHistoryItem item : items) {
            PaymentHistoryItem.InvoiceStatus status = item.getStatus();
            if (status.isPaid()) {
                totalPaid += item.getAmountValue();
                lastPaidMonth = item.getMonthLabel();
                paidCount++;
            } else if (status.isOverdue()) {
                outstanding += item.getAmountValue();
                overdueCount++;
            } else if (status.isPending()) {
                pendingCount++;
            }
        }

        txtNominal.setText(formatCurrency(totalPaid));
        if (paidCount > 0) {
            String bulanTemplate = getString(R.string.riwayat_label_paid_month, lastPaidMonth);
            txtBulanLunas.setText(bulanTemplate);
        } else {
            txtBulanLunas.setText(getString(R.string.riwayat_label_no_payment));
        }
        txtInvoiceCount.setText(getString(R.string.riwayat_paid_transaction_count, paidCount));

        // Status transaksi ringkas
        if (overdueCount > 0) {
            txtStatus.setText(getString(R.string.riwayat_status_overdue, overdueCount));
        } else if (pendingCount > 0) {
            txtStatus.setText(getString(R.string.riwayat_status_pending_tx, pendingCount));
        } else if (paidCount > 0) {
            txtStatus.setText(getString(R.string.riwayat_status_paid_all));
        } else {
            txtStatus.setText(getString(R.string.riwayat_status_no_history));
        }

        txtTunggakan.setText(formatCurrency(outstanding));
    }

    private void bindSummaryPlaceholder() {
        txtNominal.setText(formatCurrency(0));
        txtBulanLunas.setText(getString(R.string.riwayat_label_no_payment));
        txtInvoiceCount.setText(getString(R.string.riwayat_paid_transaction_count, 0));
        txtStatus.setText(getString(R.string.loading_generic));
        txtTunggakan.setText(formatCurrency(0));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            emptyView.setVisibility(View.GONE);
        }
    }

    private void showEmpty(boolean isEmpty) {
        if (emptyContainer != null) {
            emptyContainer.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void setupFilterChips() {
        if (!(chipContainer instanceof ViewGroup)) return;
        ViewGroup container = (ViewGroup) chipContainer;
        container.removeAllViews();
        addChip(container, "all", "Semua", true);
        addChip(container, "paid", "Lunas", false);
        addChip(container, "pending", "Pending", false);
        addChip(container, "overdue", "Overdue", false);
        addChip(container, "cancelled", "Batal", false);
    }

    private void addChip(ViewGroup container, String key, String label, boolean checked) {
        View chipView = getLayoutInflater().inflate(R.layout.view_filter_chip, container, false);
        if (chipView instanceof com.google.android.material.chip.Chip) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipView;
            chip.setText(label);
            chip.setTag(key);
            chip.setChecked(checked);
            chip.setOnClickListener(v -> applyFilter((String) v.getTag()));
        }
        container.addView(chipView);
    }

    private void applyFilter(@NonNull String key) {
        List<PaymentHistoryItem> filtered = new ArrayList<>();
        for (PaymentHistoryItem item : fullData) {
            if (matchesFilter(item, key)) {
                filtered.add(item);
            }
        }
        // Summary tetap berdasarkan seluruh data agar ukuran kartu stabil
        bindSummary(fullData);
        riwayatAdapter.submitList(filtered);
        showEmpty(filtered.isEmpty() && dataLoaded);
    }

    private boolean matchesFilter(PaymentHistoryItem item, String key) {
        if ("all".equalsIgnoreCase(key)) return true;
        if ("paid".equalsIgnoreCase(key)) return item.getStatus().isPaid();
        if ("overdue".equalsIgnoreCase(key)) return item.getStatus().isOverdue();
        if ("pending".equalsIgnoreCase(key)) return item.getStatus().isPending();
        if ("cancelled".equalsIgnoreCase(key)) return PaymentHistoryItem.InvoiceStatus.CANCELLED == item.getStatus();
        return true;
    }

    private String formatCurrency(double amount) {
        String formatted = currencyFormat.format(amount);
        return formatted.replace(",00", "");
    }
}
