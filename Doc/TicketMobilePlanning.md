# Rencana Arsitektur & UX Ticket Pelanggan (Mobile)

## 1) Definisi Mode Ticket
- **Dua Portal (Chat/Thread)**: Dipakai untuk kategori non-urgent (Permintaan Biasa, Saran, Komplain). Ada riwayat pesan dua arah (customer ↔ admin) di app mobile dan web admin.
- **One Shot (Sekali Kirim, Non-chat)**: Alternatif untuk kategori sederhana (opsional). User hanya mengirim sekali (subject + pesan), tanpa riwayat chat. Admin hanya memberi status.
- **WhatsApp (Urgent)**: Untuk kategori Urgent. Tidak ada chat/riwayat di portal. UI menampilkan CTA WhatsApp yang membuka nomor tujuan. Status tiket tetap disimpan (Open/In Progress/Closed) untuk tracking.

## 2) UI/UX Mobile yang Diusulkan
- **List Ticket**: Card dengan kategori, status (badge), created_at, dan hint apakah “Chat”, “One Shot”, atau “Urgent (WhatsApp)”.
- **Detail Ticket (Non-Urgent, Dua Portal)**:
  - Header: Kategori, Status badge, Created at.
  - Riwayat pesan: bubble chat, urut waktu. `sender_role` menentukan sisi/warna (customer kiri, admin kanan).
  - Input bar: textarea + tombol kirim (kirim ke ticket_messages).
  - Lampiran: bisa ditunda; kolom attachment_url sudah disiapkan.
- **Detail Ticket (One Shot)**:
  - Menampilkan subject + pesan awal, status, dan mungkin jawaban admin singkat (opsional).
  - Tidak ada input bar (disable).
- **Detail Ticket (Urgent/WhatsApp)**:
  - Tampilkan subject + pesan.
  - CTA tombol “Hubungi via WhatsApp” → buka nomor tujuan.
  - Riwayat chat disembunyikan/disabled; hanya status yang terlihat.
- **Form Buat Ticket**:
  - Field: Subject (wajib), Pesan (wajib), Pilih Kategori (Permintaan Biasa, Saran, Komplain, Urgent).
  - Jika kategori = Urgent → tampilkan CTA/info WA, hilangkan opsi chat.
  - Jika kategori = One Shot (jika diaktifkan) → tandai `is_one_shot = true` (opsional kolom tambahan).

## 3) Skema & Kolom (faktual sesuai `Doc/SchemaSupabase.md`)
> Catatan: Ini skema yang sudah ada di database sekarang. Beberapa kolom tidak dipakai di UX baru, tapi tetap ada di DB.

### Tabel `tickets`
- `id` (bigint, PK, seq `tickets_id_seq`)
- `customer_id` (FK -> customers.id)
- `subject` (text)
- `description` (text) — dipakai sebagai isi pesan awal
- `status` (ticket_status enum): `open`, `in_progress`, `closed`
- `prioritas` (prioritas_tiket enum): `rendah`, `normal`, `tinggi`, `mendesak`
- `kategori` (kategori_tiket enum): `koneksi`, `tagihan`, `instalasi`, `lainnya`, `perubahan_paket`, `permintaan_info`, `saran_komplain`, `mendesak`
- `sumber` (sumber_tiket enum): `web`, `whatsapp`
- `menunggu_detail` (boolean, default false)
- `admin_penanggung_jawab_id` (FK -> users.id, nullable)
- `pengguna_pembuat_id` (FK -> users.id, nullable)
- `created_at` (timestamptz, default now)
- `diperbarui_pada` (timestamptz, default now)
- `respon_pertama_pada` (timestamptz, nullable)
- `diselesaikan_pada` (timestamptz, nullable)
- Indeks: customer, status+prioritas, admin_penanggung_jawab, sumber+menunggu_detail.
- **Tidak ada kolom `whatsapp_target` di skema saat ini.** Gunakan nomor WhatsApp dari konfigurasi global: `business_settings.whatsapp_number` (dan `business_settings.whatsapp_cta_text` jika ada). Jika butuh nomor per tiket, baru tambahkan kolom `whatsapp_target`.

### Tabel `ticket_messages`
- `id` (bigserial, PK)
- `ticket_id` (FK -> tickets.id, on delete cascade)
- `tipe_penulis` (text check: `customer`/`admin`) — mapping ke `sender_role`
- `penulis_id` (bigint, nullable)
- `isi` (text) — mapping ke `message`
- `internal` (boolean, default false)
- `dibuat_pada` (timestamptz, default now)

### Tabel pendukung
- `customers` (id, name, phone, address, service_package_id, status, created_at, onboarding_cta_clicked_at)
- `users` (id, role, email, full_name, avatar_url, dsb.)

### Pemetaan skema → UX (supaya konsisten dengan flow mobile)
- Kategori untuk UI: gunakan subset `kategori`/`prioritas`:
  - Non-urgent (Dua Portal/One Shot): `kategori` salah satu dari `koneksi | tagihan | instalasi | permintaan_info | saran_komplain | lainnya`, dengan `prioritas` default `normal`.
  - Urgent/WA: set `prioritas = 'mendesak'` **atau** `kategori = 'mendesak'` + `sumber = 'whatsapp'`. Chat disembunyikan; CTA WA pakai nomor global.
- Status: pakai `status` enum existing (`open`, `in_progress`, `closed`).
- Pesan awal: simpan di `description` tiket.
- Riwayat chat (non-urgent): pakai `ticket_messages` (`isi` sebagai teks, `tipe_penulis` sebagai sender_role).

## 4) Logika Penentuan Mode
- **Urgent**: `category = 'urgent'` atau `is_urgent = true` → pakai mode WhatsApp, sembunyikan chat.
- **One Shot** (jika diaktifkan): tambah kolom opsional `is_one_shot` (boolean). Jika true → tidak boleh kirim pesan lanjutan; hanya tampilkan pesan awal dan status.
- **Default Dua Portal**: selain dua kondisi di atas → tampilkan chat (ticket_messages).

## 5) Simulasi Alur Data
### A. Buat Ticket Non-Urgent (Chat/Dua Portal)
1. Mobile kirim POST `/tickets` dengan: `customer_id`, `category='request'|'suggestion'|'complaint'`, `is_urgent=false`, `subject`, `message`, `status='open'`.
2. Admin melihat tiket → balas via web → INSERT `ticket_messages` (sender_role='admin').
3. Mobile fetch detail + messages → tampilkan sebagai chat.
4. Admin ubah status ke `in_progress` atau `closed` → mobile render badge.

### B. Buat Ticket Urgent (WhatsApp)
1. Mobile kirim POST `/tickets` dengan `category='urgent'`, `is_urgent=true`, `status='open'`, subject, message.
2. Mobile detail: sembunyikan chat, tampilkan tombol “Hubungi via WhatsApp” (nomor diambil dari `whatsapp_target` jika ada; jika tidak, pakai konfigurasi global).
3. Admin tetap bisa ubah status (tracking), tapi tidak ada chat di portal.

### C. One Shot (Jika dipakai)
1. Mobile kirim POST `/tickets` dengan `is_one_shot=true` (butuh kolom baru) + payload standar.
2. Mobile detail: tidak ada input chat; hanya menampilkan pesan awal dan status.
3. Admin dapat menambah satu balasan (opsional) via `ticket_messages`, atau langsung ubah status.

## 6) Respons & DTO Mobile (disarankan)
- Ticket DTO: `id, customerId, category, isUrgent, subject, message, status, createdAt, whatsappTarget?, resolvedAt?, attachmentUrl?`
- TicketMessage DTO: `id, ticketId, senderRole, message, createdAt, attachmentUrl?`
- Derived UI flags:
  - `mode = urgent ? "whatsapp" : is_one_shot ? "one-shot" : "chat"`
  - `showChat = (mode == "chat")`
  - `showWhatsAppCTA = (mode == "whatsapp")`

## 7) API/Endpoint (mengikuti pola REST Supabase yang ada)
- `GET /tickets?customer_id=...&order=created_at.desc&limit=...`
- `POST /tickets` (buat baru)
- `PATCH /tickets?id=eq.{id}` (ubah status)
- `GET /ticket_messages?ticket_id=eq.{id}&order=created_at.asc`
- `POST /ticket_messages` (kirim pesan baru; hanya untuk mode chat)

## 8) Keamanan & Validasi
- Validasi `customer_id` milik user login (mobile).
- Batasi kategori hanya yang diizinkan; hardcode mapping mobile ↔ backend.
- Untuk urgent, pastikan WA CTA hanya tampil jika ada nomor target (atau fallback ke nomor global).

## 9) Tindakan Lanjutan (jika butuh perubahan DB)
- Jika ingin One Shot: tambahkan kolom `is_one_shot boolean default false` di `tickets`.
- Jika ingin nomor WA per tiket: tambahkan `whatsapp_target text` di `tickets` (atau ambil dari settings global).
- Jika ingin lampiran di chat: gunakan `attachment_url` di `ticket_messages`.
