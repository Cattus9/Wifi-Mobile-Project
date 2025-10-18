package com.project.inet_mobile.ui.packages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.project.inet_mobile.R;
import com.project.inet_mobile.util.conn;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

public class PaketFragment extends Fragment {

    // refs card 1
    private TextView tvName1, tvDesc1, tvPrice1;
    // refs card 2
    private TextView tvName2, tvDesc2, tvPrice2;
    // refs card 3
    private TextView tvName3, tvDesc3, tvPrice3;

    public PaketFragment() {
        super(R.layout.fragment_paket);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // init views
        tvName1  = view.findViewById(R.id.tvPaketName1);
        tvDesc1  = view.findViewById(R.id.tvPaketDesc1);
        tvPrice1 = view.findViewById(R.id.tvPaketPrice1);

        tvName2  = view.findViewById(R.id.tvPaketName2);
        tvDesc2  = view.findViewById(R.id.tvPaketDesc2);
        tvPrice2 = view.findViewById(R.id.tvPaketPrice2);

        tvName3  = view.findViewById(R.id.tvPaketName3);
        tvDesc3  = view.findViewById(R.id.tvPaketDesc3);
        tvPrice3 = view.findViewById(R.id.tvPaketPrice3);

        // ambil data dari Supabase via conn
        loadPackages();
    }

    private void loadPackages() {
        if (getContext() == null) return;

        conn.fetchServicePackages(getContext(), new conn.PackagesCallback() {
            @Override
            public void onSuccess(JSONArray data) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> bindToCards(data));
            }

            @Override
            public void onError(String error) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    tvName1.setText("Gagal memuat paket");
                    tvDesc1.setText(error);
                    tvPrice1.setText("-");
                });
            }
        });
    }

    private void bindToCards(JSONArray arr) {
        NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));

        // card 1
        if (arr.length() >= 1) {
            JSONObject o = arr.optJSONObject(0);
            tvName1.setText(ns(o.optString("name")));
            tvDesc1.setText(buildDesc(o.optString("speed"), o.optString("description")));
            tvPrice1.setText(formatRupiah(rupiah, o));
        }

        // card 2
        if (arr.length() >= 2) {
            JSONObject o = arr.optJSONObject(1);
            tvName2.setText(ns(o.optString("name")));
            tvDesc2.setText(buildDesc(o.optString("speed"), o.optString("description")));
            tvPrice2.setText(formatRupiah(rupiah, o));
        }

        // card 3
        if (arr.length() >= 3) {
            JSONObject o = arr.optJSONObject(2);
            tvName3.setText(ns(o.optString("name")));
            tvDesc3.setText(buildDesc(o.optString("speed"), o.optString("description")));
            tvPrice3.setText(formatRupiah(rupiah, o));
        }
    }

    private String buildDesc(String speed, String description) {
        String s = ns(speed);
        String d = ns(description);
        if (!s.isEmpty() && !d.isEmpty()) return s + " • " + d;
        if (!s.isEmpty()) return s;
        return d;
    }

    private String formatRupiah(NumberFormat nf, JSONObject o) {
        // Supabase numeric bisa dikembalikan sebagai number (double) atau string—kita handle dua-duanya
        String priceStr = o.optString("price", null);
        if (priceStr != null && !priceStr.isEmpty()) {
            try {
                double val = Double.parseDouble(priceStr);
                return nf.format(val).replace(",00", "");
            } catch (NumberFormatException ignored) {}
        }
        double val = o.optDouble("price", Double.NaN);
        if (!Double.isNaN(val)) {
            return nf.format(val).replace(",00", "");
        }
        return "Rp -";
    }

    private String ns(String s) {
        return s == null ? "" : s;
    }
}
