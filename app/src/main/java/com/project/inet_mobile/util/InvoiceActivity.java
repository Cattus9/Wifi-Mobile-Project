package com.project.inet_mobile.util;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.inet_mobile.R;
import com.project.inet_mobile.data.payment.PaymentRemoteDataSource;
import com.project.inet_mobile.data.remote.dto.ApiResponse;
import com.project.inet_mobile.data.remote.dto.InvoiceDetailResponseData;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private View invoiceContainer;

    // Header
    private TextView tvPaymentStatus;
    private TextView tvInvoiceNumber;

    // Customer Info
    private TextView tvCustomerName;
    private TextView tvCustomerDetails;

    // Service Info
    private TextView tvServiceName;
    private TextView tvServiceDescription;
    private TextView tvServiceType;
    private TextView tvDueDate;
    private TextView tvPeriod;
    private TextView tvServicePrice;

    // Summary
    private TextView tvSubtotal;
    private TextView tvTotal;

    // Transaction
    private TextView tvTransactionDate;
    private TextView tvGateway;
    private TextView tvTransactionId;
    private TextView tvAmount;

    private PaymentRemoteDataSource paymentDataSource;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_invoice);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        paymentDataSource = new PaymentRemoteDataSource(this);

        // Get invoice_id from intent
        long invoiceId = getIntent().getLongExtra("invoice_id", -1);
        if (invoiceId == -1) {
            Toast.makeText(this, "Invoice ID tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadInvoiceDetail(invoiceId);
    }

    private void initViews() {
        progressBar = findViewById(R.id.invoiceProgress);
        invoiceContainer = findViewById(R.id.invoiceContainer);

        // Header
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        tvInvoiceNumber = findViewById(R.id.tvInvoiceNumber);

        // Customer Info
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerDetails = findViewById(R.id.tvCustomerDetails);

        // Service Info
        tvServiceName = findViewById(R.id.tvServiceName);
        tvServiceDescription = findViewById(R.id.tvServiceDescription);
        tvServiceType = findViewById(R.id.tvServiceType);
        tvDueDate = findViewById(R.id.tvDueDate);
        tvPeriod = findViewById(R.id.tvPeriod);
        tvServicePrice = findViewById(R.id.tvServicePrice);

        // Summary
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvTotal = findViewById(R.id.tvTotal);

        // Transaction
        tvTransactionDate = findViewById(R.id.tvTransactionDate);
        tvGateway = findViewById(R.id.tvGateway);
        tvTransactionId = findViewById(R.id.tvTransactionId);
        tvAmount = findViewById(R.id.tvAmount);
    }

    private void loadInvoiceDetail(long invoiceId) {
        showLoading(true);

        Call<ApiResponse<InvoiceDetailResponseData>> call = paymentDataSource.getInvoiceDetail(invoiceId);
        call.enqueue(new Callback<ApiResponse<InvoiceDetailResponseData>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<InvoiceDetailResponseData>> call,
                                   @NonNull Response<ApiResponse<InvoiceDetailResponseData>> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    populateInvoiceData(response.body().getData());
                } else {
                    Toast.makeText(InvoiceActivity.this, "Gagal memuat invoice", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<InvoiceDetailResponseData>> call, @NonNull Throwable t) {
                showLoading(false);
                Toast.makeText(InvoiceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateInvoiceData(InvoiceDetailResponseData data) {
        // Header
        tvPaymentStatus.setText("SUDAH BAYAR");
        tvInvoiceNumber.setText("No: " + data.getInvoiceNumber());

        // Customer Info
        if (data.getCustomer() != null) {
            tvCustomerName.setText(data.getCustomer().getName() != null ? data.getCustomer().getName() : "-");

            String customerDetails = "";
            if (data.getCustomer().getCustomerId() != null) {
                customerDetails += "ID: " + data.getCustomer().getCustomerId() + "\n";
            }
            if (data.getCustomer().getAddress() != null) {
                customerDetails += data.getCustomer().getAddress() + "\n";
            }
            if (data.getCustomer().getPhone() != null) {
                customerDetails += "📞 " + data.getCustomer().getPhone();
            }
            tvCustomerDetails.setText(customerDetails.trim());
        }

        // Service Info
        if (data.getCustomer() != null && data.getCustomer().getServicePackage() != null) {
            InvoiceDetailResponseData.ServicePackageInfo pkg = data.getCustomer().getServicePackage();

            tvServiceName.setText(pkg.getPackageCode() != null ? pkg.getPackageCode() : "-");
            tvServiceDescription.setText(pkg.getPackageName() != null ? pkg.getPackageName() : "High Speed Internet Package Service");
            tvServiceType.setText(pkg.getType() != null ? pkg.getType().toUpperCase() : "REGULAR PREPAID");
            tvPeriod.setText(pkg.getDuration() != null ? pkg.getDuration() : "-");
            tvServicePrice.setText(formatCurrency(pkg.getPrice()));
        }

        // Dates
        tvDueDate.setText(formatDate(data.getDueDate()));

        // Set total directly
        double total = data.getAmount();

        tvSubtotal.setText(formatCurrency(total));
        tvTotal.setText(formatCurrency(total));

        // Transaction Info
        tvTransactionDate.setText(formatDate(data.getPaidAt()));
        tvGateway.setText("Manual"); // Default or from latestPayment if available
        tvTransactionId.setText(data.getInvoiceNumber());
        tvAmount.setText(formatCurrency(total));
    }

    private String formatCurrency(double amount) {
        String formatted = currencyFormat.format(amount);
        return formatted.replace(",00", "");
    }

    private String formatDate(String raw) {
        if (raw == null || raw.isEmpty()) return "-";

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
                    SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    return out.format(date);
                }
            } catch (ParseException ignore) {
            }
        }
        return raw;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (invoiceContainer != null) {
            invoiceContainer.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}