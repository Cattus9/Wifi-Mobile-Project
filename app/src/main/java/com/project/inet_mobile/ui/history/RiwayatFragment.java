package com.project.inet_mobile.ui.history;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.inet_mobile.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RiwayatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private TextView txtNominal;
    private TextView txtBulanLunas;
    private TextView txtInvoiceCount;
    private TextView txtStatus;
    private TextView txtTunggakan;
    private View chipContainer;

    private RiwayatAdapter riwayatAdapter;
    private final List<PaymentHistoryItem> fullData = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    public RiwayatFragment() {
        super(R.layout.fragment_riwayat);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadDummyHistory();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerRiwayat);
        progressBar = view.findViewById(R.id.riwayatProgress);
        emptyView = view.findViewById(R.id.riwayatEmptyView);
        txtNominal = view.findViewById(R.id.txtNominal);
        txtBulanLunas = view.findViewById(R.id.txtBulanLunas);
        txtInvoiceCount = view.findViewById(R.id.txtInvoiceCount);
        txtStatus = view.findViewById(R.id.txtStatus);
        txtTunggakan = view.findViewById(R.id.txtTunggakan);
        chipContainer = view.findViewById(R.id.chipContainer);
        setupFilterChips();
    }

    private void setupRecyclerView() {
        riwayatAdapter = new RiwayatAdapter(item -> {
            // Placeholder: ketika API siap, bisa arahkan ke detail invoice atau bukti bayar.
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(riwayatAdapter);
    }

    private void loadDummyHistory() {
        showLoading(true);
        List<PaymentHistoryItem> dummyData = createDummyData();
        fullData.clear();
        fullData.addAll(dummyData);
        bindSummary(dummyData);
        riwayatAdapter.submitList(new ArrayList<>(dummyData));
        showEmpty(dummyData.size() <= 1); // header-only means empty
        showLoading(false);
    }

    private List<PaymentHistoryItem> createDummyData() {
        List<PaymentHistoryItem> items = new ArrayList<>();

        items.add(createItem(
                "September 2024",
                "12 Sep 2024",
                "Transfer Bank",
                "INV-2024-0912",
                PaymentHistoryItem.InvoiceStatus.PAID,
                350000
        ));

        items.add(createItem(
                "Agustus 2024",
                "10 Agu 2024",
                "Virtual Account",
                "INV-2024-0810",
                PaymentHistoryItem.InvoiceStatus.PAID,
                350000
        ));

        items.add(createItem(
                "Juli 2024",
                "11 Jul 2024",
                "Kartu Kredit",
                "INV-2024-0711",
                PaymentHistoryItem.InvoiceStatus.OVERDUE,
                350000
        ));

        items.add(createItem(
                "Juni 2024",
                "12 Jun 2024",
                "Transfer Bank",
                "INV-2024-0612",
                PaymentHistoryItem.InvoiceStatus.DRAFT,
                350000
        ));

        return items;
    }

    private PaymentHistoryItem createItem(
            String monthLabel,
            String paymentDate,
            String method,
            String invoice,
            PaymentHistoryItem.InvoiceStatus status,
            double amount
    ) {
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
            case UNKNOWN:
                bg = ContextCompat.getColor(requireContext(), R.color.status_draft_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.status_text_dark);
                break;
            case PAID:
            default:
                bg = ContextCompat.getColor(requireContext(), R.color.status_paid_bg);
                textColor = ContextCompat.getColor(requireContext(), R.color.white);
                break;
        }

        return new PaymentHistoryItem(
                monthLabel,
                paymentDate,
                method,
                invoice,
                status,
                formatCurrency(amount),
                amount,
                textColor,
                bg
        );
    }

    private void bindSummary(List<PaymentHistoryItem> items) {
        double totalPaid = 0;
        double outstanding = 0;
        String lastPaidMonth = "-";
        int paidCount = 0;

        for (PaymentHistoryItem item : items) {
            PaymentHistoryItem.InvoiceStatus status = item.getStatus();
            if (status.isPaid()) {
                totalPaid += item.getAmountValue();
                lastPaidMonth = item.getMonthLabel();
                paidCount++;
            } else if (status.isOverdue()) {
                outstanding += item.getAmountValue();
            }
        }

        txtNominal.setText(formatCurrency(totalPaid));
        String bulanTemplate = getString(R.string.riwayat_label_paid_month, lastPaidMonth);
        String noPaymentLabel = getString(R.string.riwayat_label_no_payment);
        txtBulanLunas.setText("-".equals(lastPaidMonth) ? noPaymentLabel : bulanTemplate);
        txtInvoiceCount.setText(getString(R.string.riwayat_paid_transaction_count, paidCount));
        txtStatus.setText(getString(paidCount > 0
                ? R.string.riwayat_status_active
                : R.string.riwayat_status_pending));
        txtTunggakan.setText(formatCurrency(outstanding));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
    }

    private void showEmpty(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.INVISIBLE : View.VISIBLE);
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
        bindSummary(filtered);
        riwayatAdapter.submitList(filtered);
        showEmpty(filtered.isEmpty());
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
