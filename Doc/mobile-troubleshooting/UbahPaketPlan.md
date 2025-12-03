• Berikut detail/policy avatar yang bisa Anda share ke tim mobile (Android):

- Bucket & URL format:
    - Bucket: avatars (public).
    - Nama file deterministik per user: user_{id}.ext untuk admin/user, customer_{id}.ext untuk pelanggan (prefix berdasarkan peran).
    - Public URL format: {SUPABASE_URL}/storage/v1/object/public/avatars/user_{id}.ext (atau customer_{id}.ext), extension mengikuti file upload (jpg/png/webp).
- Validasi & batasan:
    - Maksimum 2 MB.
    - MIME yang diterima: image/jpeg, image/png, image/webp.
    - Harus gambar valid (getimagesize OK); jika tidak, ditolak.
    - Ekstensi dipetakan dari MIME (jpg/png/webp).
- Overwrite behavior:
    - Upload baru akan menghapus file lama dengan nama sama (replace), tetapi karena nama mengandung ID, user lain tidak bisa menimpa avatar pengguna lain.
- Autentikasi/headers:
    - Web menggunakan Supabase Storage REST dengan header: apikey: SUPABASE_ANON_KEY, Authorization: Bearer <access_token user>, Content-Type: <mime>.
    - Untuk mobile: gunakan Supabase Storage API resmi atau REST dengan header sama; pastikan access token valid.
- Penyimpanan profil:
    - URL avatar disimpan di kolom avatar_url tabel users. Setelah upload berhasil, aplikasi web memperbarui profil user dengan URL ini.
    - Mobile dapat update avatar_url via endpoint/backend yang menulis ke users.avatar_url, atau langsung ke REST jika policy mengizinkan.
- Konvensi keamanan:
    - Nama file sudah “safe” karena deterministik; tidak perlu menambahkan UUID. Jangan izinkan nama file custom dari client.

Pesan singkat ke tim mobile:

- Upload avatar ke bucket avatars sebagai customer_{id}.ext (jpg/png/webp, max 2MB). Gunakan Supabase Storage API dengan anon key + bearer token user. Setelah upload, simpan
  URL ke users.avatar_url. Upload baru otomatis replace avatar lama untuk ID yang sama, tidak akan menimpa user lain. URL publik: {SUPABASE_URL}/storage/v1/object/public/      
  avatars/customer_{id}.ext.

Berikut pesan singkat untuk tim mobile terkait perubahan “Ubah Paket”:

Revisi ----- 

1. Tidak ada lagi input “tanggal aktif diinginkan” dari pelanggan.      
   Pengajuan hanya perlu:
    - Pilihan paket baru (tidak boleh sama dengan paket aktif).
    - Catatan tambahan (opsional) untuk preferensi/kebutuhan khusus.
2. Kebijakan aktivasi:
    - Paket baru akan aktif otomatis pada periode/bulan berikutnya      
      setelah admin menyetujui (bukan berdasarkan tanggal yang diisi    
      pelanggan).
    - Constraint tetap: satu tiket perubahan paket aktif per customer   
      (kategori perubahan_paket, status open/in_progress).
3. Data yang disimpan saat submit:
    - Buat tiket di tickets dengan kategori perubahan_paket, status
        - status_keputusan = 'menunggu'
    - Tidak perlu kirim tanggal_aktif_diinginkan.
4. Validasi:
    - Tolak jika paket tujuan sama dengan paket aktif.
    - Admin yang menentukan persetujuan/penjadwalan. Paket baru akan    
      diterapkan untuk periode penagihan berikutnya (logic admin/       
      skrip generate invoice). Mobile cukup menampilkan status tiket/   
      keputusan.

setelah disetujui admin.