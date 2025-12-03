## Mobile API - Change Package

- **Endpoint**: `POST /api/v1/customer/change-package`
- **Auth**: Bearer access token (Supabase Auth), role: `customer`
- **Body**:
  ```json
  {
    "package_id": 123,
    "notes": "Opsional, catatan tambahan"
  }
  ```
  - `package_id` (int, required): paket baru yang aktif.
  - `notes` (string, optional): catatan tambahan untuk admin.

### Validasi
- Harus login sebagai customer.
- Tidak boleh ada permintaan perubahan paket yang masih `open/in_progress` (kategori `perubahan_paket`).
- Tidak boleh ada invoice outstanding (`issued/overdue/draft`).
- `package_id` harus paket aktif dan berbeda dari paket saat ini.

### Proses
- Membuat tiket `tickets`:
  - `kategori = perubahan_paket`, `status = open`, `sumber = mobile`, `prioritas = normal`.
  - `subject = "Permintaan perubahan paket"`, `description = notes` (atau default teks).
- Membuat detail `ticket_perubahan_paket`:
  - `customer_id`, `paket_sekarang_id` (jika ada), `paket_diminta_id`, `catatan_pelanggan`, `inisiasi_oleh = pelanggan`, `status_keputusan = menunggu`.
- Tidak ada input tanggal aktivasi; kebijakan: paket baru aktif periode berikutnya setelah disetujui admin.

### Response Sukses (201)
```json
{
  "success": true,
  "message": "Permintaan perubahan paket berhasil dikirim",
  "data": {
    "ticket_id": 456,
    "status": "pending",
    "current_package": "Paket Aktif",
    "requested_package": "Paket Baru",
    "notes": "…",
    "message": "Paket baru akan diproses admin dan aktif pada periode berikutnya setelah disetujui."
  }
}
```

### Response Error (contoh)
- 400 `PENDING_REQUEST`: Masih ada permintaan perubahan paket aktif.
- 400 `OUTSTANDING_INVOICE`: Tagihan berjalan belum dibayar.
- 400 `PACKAGE_NOT_AVAILABLE`: Paket tidak aktif/ada.
- 400 `PACKAGE_SAME_AS_CURRENT`: Paket tujuan sama dengan paket aktif.
- 404 `CUSTOMER_NOT_FOUND`: Data customer tidak ditemukan.
- 500: Masalah Supabase atau server.
