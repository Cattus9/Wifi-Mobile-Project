package com.project.inet_mobile.util;

import android.animation.ValueAnimator;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.project.inet_mobile.Paket;
import com.project.inet_mobile.R;
import com.project.inet_mobile.ui.payment.PembayaranFragment;

public class DetailPaketFragment extends Fragment {

    // Arguments keys
    private static final String ARG_PAKET_ID = "paket_id";
    private static final String ARG_PAKET_NAME = "paket_name";
    private static final String ARG_PAKET_DESC = "paket_desc";
    private static final String ARG_PAKET_PRICE = "paket_price";
    private static final String ARG_PAKET_PRICE_ORIGINAL = "paket_price_original";
    private static final String ARG_PAKET_SPEED = "paket_speed";
    private static final String ARG_PAKET_DURATION = "paket_duration";
    private static final String ARG_PAKET_QUOTA = "paket_quota";
    private static final String ARG_PAKET_PHONE = "paket_phone";
    private static final String ARG_PAKET_IS_POPULAR = "paket_is_popular";

    // Views
    private Toolbar toolbar;
    private TextView tvBadgePopuler, tvBadgeDuration;
    private TextView tvPaketName, tvPaketDesc;
    private TextView tvPriceCurrent, tvPriceOriginal;
    private TextView tvSpeedValue, tvDurationValue, tvQuotaValue, tvPhoneValue;
    private LinearLayout benefitPhone;
    private View dividerPhone;
    private TextView tvDescription;
    private LinearLayout termsHeader, faqHeader;
    private TextView termsContent, faqContent, termsIcon, faqIcon;
    private TextView tvTotalPrice;
    private Button btnBuy;

    // Data
    private String paketId;
    private String paketName;
    private String paketDesc;
    private String paketPrice;
    private String paketPriceOriginal;
    private String paketSpeed;
    private String paketDuration;
    private String paketQuota;
    private String paketPhone;
    private boolean isPaketPopular;

    // Expandable state
    private boolean isTermsExpanded = false;
    private boolean isFaqExpanded = false;

    public DetailPaketFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create DetailPaketFragment with Paket object
     */
    public static DetailPaketFragment newInstance(Paket paket) {
        DetailPaketFragment fragment = new DetailPaketFragment();
        Bundle args = new Bundle();

        // Map dari database ke fragment
        args.putString(ARG_PAKET_ID, String.valueOf(paket.getId()));
        args.putString(ARG_PAKET_NAME, paket.getName() != null ? paket.getName() : "");
        args.putString(ARG_PAKET_DESC, paket.getDescription() != null ? paket.getDescription() : "");
        args.putString(ARG_PAKET_PRICE, paket.getHarga() != null ? paket.getHarga() : "");
        args.putString(ARG_PAKET_PRICE_ORIGINAL, paket.getHargaAsli());
        args.putString(ARG_PAKET_SPEED, paket.getSpeed() != null ? paket.getSpeed() : "");
        args.putString(ARG_PAKET_DURATION, paket.getDuration() != null ? paket.getDuration() : "");
        args.putString(ARG_PAKET_QUOTA, paket.getQuota());
        args.putString(ARG_PAKET_PHONE, paket.getPhone());
        args.putBoolean(ARG_PAKET_IS_POPULAR, paket.isPopuler());

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            paketId = getArguments().getString(ARG_PAKET_ID);
            paketName = getArguments().getString(ARG_PAKET_NAME);
            paketDesc = getArguments().getString(ARG_PAKET_DESC);
            paketPrice = getArguments().getString(ARG_PAKET_PRICE);
            paketPriceOriginal = getArguments().getString(ARG_PAKET_PRICE_ORIGINAL);
            paketSpeed = getArguments().getString(ARG_PAKET_SPEED);
            paketDuration = getArguments().getString(ARG_PAKET_DURATION);
            paketQuota = getArguments().getString(ARG_PAKET_QUOTA);
            paketPhone = getArguments().getString(ARG_PAKET_PHONE);
            isPaketPopular = getArguments().getBoolean(ARG_PAKET_IS_POPULAR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail_paket, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        initViews(view);

        // Setup toolbar
        setupToolbar();

        // Populate data
        populateData();

        // Setup expandable sections
        setupExpandableSections();

        // Setup buy button
        setupBuyButton();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);

        // Badges
        tvBadgePopuler = view.findViewById(R.id.tvBadgePopuler);
        tvBadgeDuration = view.findViewById(R.id.tvBadgeDuration);

        // Main info
        tvPaketName = view.findViewById(R.id.tvPaketName);
        tvPaketDesc = view.findViewById(R.id.tvPaketDesc);
        tvPriceCurrent = view.findViewById(R.id.tvPriceCurrent);
        tvPriceOriginal = view.findViewById(R.id.tvPriceOriginal);

        // Benefits
        tvSpeedValue = view.findViewById(R.id.tvSpeedValue);
        tvDurationValue = view.findViewById(R.id.tvDurationValue);
        tvQuotaValue = view.findViewById(R.id.tvQuotaValue);
        tvPhoneValue = view.findViewById(R.id.tvPhoneValue);
        benefitPhone = view.findViewById(R.id.benefitPhone);
        dividerPhone = view.findViewById(R.id.dividerPhone);

        // Description
        tvDescription = view.findViewById(R.id.tvDescription);

        // Expandable sections
        termsHeader = view.findViewById(R.id.termsHeader);
        termsContent = view.findViewById(R.id.termsContent);
        termsIcon = view.findViewById(R.id.termsIcon);

        faqHeader = view.findViewById(R.id.faqHeader);
        faqContent = view.findViewById(R.id.faqContent);
        faqIcon = view.findViewById(R.id.faqIcon);

        // Bottom bar
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        btnBuy = view.findViewById(R.id.btnBuy);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });
    }

    private void populateData() {
        // Set paket name and description
        if (paketName != null) {
            tvPaketName.setText(paketName);
        }

        if (paketDesc != null) {
            tvPaketDesc.setText(paketDesc);
        }

        // Set price
        if (paketPrice != null) {
            tvPriceCurrent.setText(paketPrice);
            tvTotalPrice.setText(paketPrice);
        }

        // Set original price if available
        if (paketPriceOriginal != null && !paketPriceOriginal.isEmpty()) {
            tvPriceOriginal.setVisibility(View.VISIBLE);
            tvPriceOriginal.setText(paketPriceOriginal);
            tvPriceOriginal.setPaintFlags(tvPriceOriginal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            tvPriceOriginal.setVisibility(View.GONE);
        }

        // Set popular badge
        if (isPaketPopular) {
            tvBadgePopuler.setVisibility(View.VISIBLE);
        } else {
            tvBadgePopuler.setVisibility(View.GONE);
        }

        // Set duration badge
        if (paketDuration != null && !paketDuration.isEmpty()) {
            tvBadgeDuration.setText(paketDuration);
        }

        // Set benefits
        if (paketSpeed != null && !paketSpeed.isEmpty()) {
            tvSpeedValue.setText(paketSpeed);
        }

        if (paketDuration != null && !paketDuration.isEmpty()) {
            tvDurationValue.setText(paketDuration);
        }

        if (paketQuota != null && !paketQuota.isEmpty()) {
            tvQuotaValue.setText(paketQuota);
        }

        // Set phone benefit (optional)
        if (paketPhone != null && !paketPhone.isEmpty()) {
            benefitPhone.setVisibility(View.VISIBLE);
            dividerPhone.setVisibility(View.VISIBLE);
            tvPhoneValue.setText(paketPhone);
        } else {
            benefitPhone.setVisibility(View.GONE);
            dividerPhone.setVisibility(View.GONE);
        }

        // Set description
        setDescription();

        // Set terms and conditions
        setTermsAndConditions();

        // Set FAQ
        setFAQ();
    }

    private void setDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("Paket internet rumah dengan kecepatan optimal untuk kebutuhan sehari-hari:\n\n");
        desc.append("• Cocok untuk streaming HD\n");
        desc.append("• Video conference lancar\n");
        desc.append("• Gaming online stabil\n");
        desc.append("• Browsing super cepat\n");
        desc.append("• Download file besar\n\n");
        desc.append("🎁 Gratis instalasi dan modem untuk pelanggan baru!");

        tvDescription.setText(desc.toString());
    }

    private void setTermsAndConditions() {
        StringBuilder terms = new StringBuilder();
        terms.append("• Paket berlaku selama masa aktif sejak aktivasi\n\n");
        terms.append("• Kuota akan hangus jika tidak digunakan dalam masa aktif\n\n");
        terms.append("• FUP (Fair Usage Policy) berlaku untuk unlimited quota\n\n");
        terms.append("• Tidak dapat digabungkan dengan promo lain\n\n");
        terms.append("• Harga sudah termasuk PPN 11%\n\n");
        terms.append("• Pembayaran dapat dilakukan via transfer, e-wallet, atau kartu kredit\n\n");
        terms.append("• Pembatalan tidak dapat dilakukan setelah pembayaran berhasil");

        termsContent.setText(terms.toString());
    }

    private void setFAQ() {
        StringBuilder faq = new StringBuilder();
        faq.append("Bagaimana cara aktivasi?\n");
        faq.append("Paket akan aktif otomatis setelah pembayaran dikonfirmasi dalam 1x24 jam.\n\n");
        faq.append("Apakah bisa diperpanjang?\n");
        faq.append("Ya, Anda bisa perpanjang paket sebelum masa aktif habis.\n\n");
        faq.append("Bagaimana jika kuota habis?\n");
        faq.append("Kecepatan akan dikurangi sesuai FUP atau bisa upgrade paket.");

        faqContent.setText(faq.toString());
    }

    private void setupExpandableSections() {
        // Terms & Conditions expandable
        termsHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleExpandable(termsContent, termsIcon, isTermsExpanded);
                isTermsExpanded = !isTermsExpanded;
            }
        });

        // FAQ expandable
        faqHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleExpandable(faqContent, faqIcon, isFaqExpanded);
                isFaqExpanded = !isFaqExpanded;
            }
        });
    }

    private void toggleExpandable(final TextView content, final TextView icon, boolean isExpanded) {
        if (isExpanded) {
            // Collapse
            collapseView(content);
            rotateIcon(icon, 90, 0);
        } else {
            // Expand
            expandView(content);
            rotateIcon(icon, 0, 90);
        }
    }

    private void expandView(final View view) {
        view.setVisibility(View.VISIBLE);
        view.measure(View.MeasureSpec.makeMeasureSpec(((View) view.getParent()).getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED);
        final int targetHeight = view.getMeasuredHeight();

        view.getLayoutParams().height = 0;
        view.requestLayout();

        ValueAnimator animator = ValueAnimator.ofInt(0, targetHeight);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.getLayoutParams().height = (int) animation.getAnimatedValue();
                view.requestLayout();
            }
        });
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void collapseView(final View view) {
        final int initialHeight = view.getMeasuredHeight();

        ValueAnimator animator = ValueAnimator.ofInt(initialHeight, 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.getLayoutParams().height = (int) animation.getAnimatedValue();
                view.requestLayout();

                if ((int) animation.getAnimatedValue() == 0) {
                    view.setVisibility(View.GONE);
                }
            }
        });
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    private void rotateIcon(final TextView icon, float fromDegree, float toDegree) {
        icon.animate()
                .rotation(toDegree)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    private void setupBuyButton() {
        btnBuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle buy action
                handleBuyAction();
            }
        });
    }

    private void handleBuyAction() {
        // Navigate to PembayaranFragment
        if (getActivity() != null) {
            PembayaranFragment pembayaranFragment = new PembayaranFragment();

            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.dashboardFragmentContainer, pembayaranFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }

    /**
     * Sembunyikan Bottom Navigation saat masuk detail paket
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
     * Tampilkan Bottom Navigation saat keluar dari detail paket
     */
    private void showBottomNavigation() {
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.dashboardBottomNav);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Sembunyikan bottom navbar setiap kali fragment menjadi aktif
        hideBottomNavigation();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Tampilkan kembali bottom navbar saat fragment tidak aktif
        showBottomNavigation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}