package com.project.inet_mobile.ui.cs;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import com.project.inet_mobile.R;

public class FormLaporanActivity extends AppCompatActivity {

    private TextInputEditText inputNama, inputAlamat, inputLaporan, inputTanggal;
    private Button buttonKirim;
    private Calendar myCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragment_cs);

        // Inisialisasi Views
//        inputNama = findViewById(R.id.input_nama);
//        inputAlamat = findViewById(R.id.input_alamat);
        inputLaporan = findViewById(R.id.input_laporan);
//        inputTanggal = findViewById(R.id.input_tanggal);
        buttonKirim = findViewById(R.id.button_kirim);

        // Inisialisasi Kalender
        myCalendar = Calendar.getInstance();

        // Buat DatePickerDialog listener
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabelTanggal(); // Panggil method untuk update teks
            }
        };

        // Atur OnClickListener untuk inputTanggal
        inputTanggal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(FormLaporanActivity.this, dateSetListener,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        // (Tambahkan OnClickListener untuk buttonKirim di sini)
        buttonKirim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Panggil method untuk mengambil semua data
                kirimDataLaporan();
            }
        });
    }

    // Method untuk memformat dan menampilkan tanggal di EditText
    private void updateLabelTanggal() {
        String myFormat = "dd/MM/yyyy"; // Format tanggal
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        inputTanggal.setText(sdf.format(myCalendar.getTime()));
    }

    // Method untuk mengambil semua data (contoh)
    private void kirimDataLaporan() {
        String nama = inputNama.getText().toString();
        String alamat = inputAlamat.getText().toString();
        String laporan = inputLaporan.getText().toString();
        String tanggal = inputTanggal.getText().toString();

        // (Di sini Anda bisa memvalidasi input atau mengirimnya ke database/API)
        // Contoh:
        if (nama.isEmpty()) {
            inputNama.setError("Nama tidak boleh kosong");
            return;
        }

        // ... Lakukan sesuatu dengan data ini ...
    }
}