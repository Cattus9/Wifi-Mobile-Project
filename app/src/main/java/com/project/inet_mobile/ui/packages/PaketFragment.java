package com.project.inet_mobile.ui.packages;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // Tambahkan ini

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.project.inet_mobile.R;
import com.project.inet_mobile.Paket; // Tambahkan ini
import com.project.inet_mobile.PaketAdapter; // Tambahkan ini
import com.project.inet_mobile.util.conn;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList; // Tambahkan ini
import java.util.List; // Tambahkan ini
import java.util.Locale;

public class PaketFragment extends Fragment {

    // refs card 1 - Hapus karena tidak digunakan lagi
    // private TextView tvName1, tvDetail1, tvPrice1;
    // refs card 2 - Hapus karena tidak digunakan lagi
    // private TextView tvName2, tvDetail2, tvPrice2;
    // private TextView badgePopuler2; // opsional: untuk badge "POPULER"
    // refs card 3 - Hapus karena tidak digunakan lagi
    // private TextView tvName3, tvDetail3, tvPrice3;

    // Tambahkan RecyclerView dan Adapter
    private RecyclerView recyclerViewPaket;
    private PaketAdapter paketAdapter;

    public PaketFragment() {
        super(R.layout.fragment_paket);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi RecyclerView
        recyclerViewPaket = view.findViewById(R.id.recyclerViewPaket);

        // Buat adapter dengan list kosong dan listener (listener bisa null jika tidak digunakan untuk sekarang)
        paketAdapter = new PaketAdapter(new ArrayList<>(), new PaketAdapter.OnPaketClickListener() {
            @Override
            public void onPaketClick(Paket paket) {
                // Tangani klik paket di sini jika diperlukan
                // Contoh: Toast.makeText(getContext(), "Klik: " + paket.getNama(), Toast.LENGTH_SHORT).show();
            }
        });

        // Atur LayoutManager dan Adapter
        recyclerViewPaket.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewPaket.setAdapter(paketAdapter);

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
                    // Kita tidak lagi menggunakan TextView statis.
                    // Anda bisa menangani error di sini, misalnya dengan Toast.
                    // Atau biarkan adapter kosong jika gagal.
                    // Contoh:
                    // Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();

                    // Untuk sekarang, kita kosongkan agar tidak terjadi error.
                    // RecyclerView akan menampilkan list kosong jika adapter kosong.
                });
            }
        });
    }

    private void bindToCards(JSONArray arr) {
        NumberFormat rupiah = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        List<Paket> paketList = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) {
                String name = ns(o.optString("name"));
                String desc = buildDesc(o.optString("speed"), o.optString("description"));
                String price = formatRupiah(rupiah, o);
                // Asumsikan field boolean untuk populer adalah "is_popular" di database
                // Ganti "is_popular" jika nama field berbeda
                boolean isPopuler = o.optBoolean("is_popular", false);

                Paket paket = new Paket(name, desc, price, isPopuler);
                paketList.add(paket);
            }
        }

        // Update adapter dengan data baru
        paketAdapter.updateData(paketList);
    }

    private String buildDesc(String speed, String description) {
        String s = ns(speed);
        String d = ns(description);
        if (!s.isEmpty() && !d.isEmpty()) return s + " • " + d;
        if (!s.isEmpty()) return s;
        return d;
    }

    private String formatRupiah(NumberFormat nf, JSONObject o) {
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