-- Enums
CREATE TYPE user_role AS ENUM ('admin', 'customer');
CREATE TYPE customer_status AS ENUM ('new', 'active', 'suspended', 'cancelled');
CREATE TYPE invoice_status AS ENUM ('draft', 'issued', 'overdue', 'paid', 'cancelled');
CREATE TYPE ticket_status AS ENUM ('open', 'in_progress', 'closed');
CREATE TYPE prioritas_tiket AS ENUM ('rendah', 'normal', 'tinggi', 'mendesak');
CREATE TYPE kategori_tiket AS ENUM ('koneksi', 'tagihan', 'instalasi', 'lainnya', 'perubahan_paket', 'permintaan_info', 'saran_komplain', 'mendesak');
-- Migration Note (2025-11-19): Added 'permintaan_info' and 'saran_komplain' for simplified support UX
-- Command: ALTER TYPE kategori_tiket ADD VALUE IF NOT EXISTS 'permintaan_info';
--          ALTER TYPE kategori_tiket ADD VALUE IF NOT EXISTS 'saran_komplain';
-- Migration Note (2025-11-20): Added 'mendesak' for auto-created emergency WhatsApp tickets
-- Command: ALTER TYPE kategori_tiket ADD VALUE IF NOT EXISTS 'mendesak';
-- 'lainnya' kept for backward compatibility (PostgreSQL enum doesn't support DROP VALUE)
--
-- Usage Note (2025-11-20):
-- - 'perubahan_paket' has dedicated flow in profile.php (not shown in support category selector)
-- - 'mendesak' is auto-created when user clicks emergency WhatsApp button (track-only, not user-selectable)
-- - Customer-facing categories: koneksi, tagihan, instalasi, permintaan_info, saran_komplain
-- - Admin can filter all categories including 'perubahan_paket' and 'mendesak' for monitoring
-- - Icons migrated from emoji to Bootstrap Icons (2025-11-20) for consistency:
--   * koneksi: bi-wifi (primary - changed from danger to avoid redundancy with mendesak priority)
--   * tagihan: bi-credit-card-fill (warning), instalasi: bi-tools (info)
--   * perubahan_paket: bi-arrow-repeat (primary), permintaan_info: bi-question-circle-fill (secondary)
--   * saran_komplain: bi-chat-square-text-fill (dark/secondary)
--   * mendesak: bi-exclamation-triangle-fill (danger - emergency WhatsApp auto-created tickets)
CREATE TYPE sumber_tiket AS ENUM ('web', 'whatsapp');

-- Sequences for BIGINT primary keys
CREATE SEQUENCE users_id_seq AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE customers_id_seq AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE service_packages_id_seq AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE invoices_id_seq AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE tickets_id_seq AS BIGINT START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- service_packages
CREATE TABLE service_packages (
  id BIGINT PRIMARY KEY DEFAULT nextval('service_packages_id_seq'),
  name TEXT NOT NULL,
  description TEXT,
  description_points TEXT, -- 3 poin deskripsi (pisahkan newline)
  speed TEXT,
  price NUMERIC(10,2) NOT NULL DEFAULT 0.00,
  is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- customers
CREATE TABLE customers (
  id BIGINT PRIMARY KEY DEFAULT nextval('customers_id_seq'),
  name TEXT NOT NULL,
  phone TEXT,
  address TEXT,
  service_package_id BIGINT,
  status customer_status NOT NULL DEFAULT 'new',
  onboarding_cta_clicked_at TIMESTAMPTZ DEFAULT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_customers_service_package
    FOREIGN KEY (service_package_id)
    REFERENCES service_packages(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- users
CREATE TABLE users (
  id BIGINT PRIMARY KEY DEFAULT nextval('users_id_seq'),
  auth_user_id UUID NOT NULL,
  customer_id BIGINT,
  email TEXT NOT NULL UNIQUE,
  role user_role NOT NULL DEFAULT 'customer',
  full_name TEXT,
  avatar_url TEXT,
  phone TEXT,
  last_login_at TIMESTAMPTZ,
  is_active BOOLEAN DEFAULT true,
  created_by BIGINT REFERENCES users(id),
  updated_by BIGINT REFERENCES users(id),
  notes TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_users_auth_user
    FOREIGN KEY (auth_user_id)
    REFERENCES auth.users(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_users_customer
    FOREIGN KEY (customer_id)
    REFERENCES customers(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT check_not_self_reference CHECK (created_by IS NULL OR created_by != id),
  CONSTRAINT check_admin_active CHECK (role != 'admin' OR is_active = true)
);
-- Migration Note (2025-11-29): Added full_name, avatar_url, phone, last_login_at for admin profile settings
-- See: migrations/001_add_admin_profile_fields.sql
-- Migration Note (2025-11-30): Added is_active, created_by, updated_by, notes for admin management
-- See: migrations/003_add_admin_management_fields.sql

-- invoices
CREATE TABLE invoices (
  id BIGINT PRIMARY KEY DEFAULT nextval('invoices_id_seq'),
  customer_id BIGINT NOT NULL,
  amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
  description TEXT,
  due_date DATE,
  status invoice_status NOT NULL DEFAULT 'issued',
  paid_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_invoices_customer
    FOREIGN KEY (customer_id)
    REFERENCES customers(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

-- tickets
CREATE TABLE tickets (
  id BIGINT PRIMARY KEY DEFAULT nextval('tickets_id_seq'),
  customer_id BIGINT NOT NULL,
  subject TEXT NOT NULL,
  description TEXT,
  status ticket_status NOT NULL DEFAULT 'open',
  prioritas prioritas_tiket NOT NULL DEFAULT 'normal',
  kategori kategori_tiket NOT NULL DEFAULT 'lainnya',
  sumber sumber_tiket NOT NULL DEFAULT 'web',
  menunggu_detail BOOLEAN NOT NULL DEFAULT FALSE,
  admin_penanggung_jawab_id BIGINT,
  pengguna_pembuat_id BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  diperbarui_pada TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  respon_pertama_pada TIMESTAMPTZ,
  diselesaikan_pada TIMESTAMPTZ,
  CONSTRAINT fk_tickets_customer
    FOREIGN KEY (customer_id)
    REFERENCES customers(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,
  CONSTRAINT fk_tickets_admin_penanggung_jawab
    FOREIGN KEY (admin_penanggung_jawab_id)
    REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,
  CONSTRAINT fk_tickets_pengguna_pembuat
    FOREIGN KEY (pengguna_pembuat_id)
    REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
);

-- ticket_messages
CREATE TABLE ticket_messages (
  id BIGSERIAL PRIMARY KEY,
  ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  tipe_penulis TEXT NOT NULL CHECK (tipe_penulis IN ('customer','admin')),
  penulis_id BIGINT,
  isi TEXT NOT NULL,
  internal BOOLEAN NOT NULL DEFAULT FALSE,
  dibuat_pada TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ticket_attachments
CREATE TABLE ticket_attachments (
  id BIGSERIAL PRIMARY KEY,
  ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
  url_berkas TEXT NOT NULL,
  nama_berkas TEXT,
  tipe_mime TEXT,
  diunggah_oleh_id BIGINT,
  diunggah_pada TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_customers_service_package_id ON customers(service_package_id);
CREATE INDEX idx_users_customer_id ON users(customer_id);
CREATE INDEX idx_users_role ON users(role);
CREATE UNIQUE INDEX idx_users_auth_user_id ON users(auth_user_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX idx_tickets_status_prioritas ON tickets(status, prioritas);
CREATE INDEX idx_tickets_admin_penanggung_jawab ON tickets(admin_penanggung_jawab_id);
CREATE INDEX idx_tickets_sumber_menunggu_detail ON tickets(sumber, menunggu_detail);
CREATE INDEX idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);
CREATE INDEX idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);

-- ticket_perubahan_paket
CREATE TABLE ticket_perubahan_paket (
  ticket_id BIGINT PRIMARY KEY REFERENCES tickets(id) ON DELETE CASCADE,
  customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  paket_sekarang_id BIGINT REFERENCES service_packages(id),
  paket_diminta_id BIGINT NOT NULL REFERENCES service_packages(id),
  tanggal_aktif_diinginkan DATE,
  jadwal_aktivasi TIMESTAMPTZ,
  catatan_pelanggan TEXT,
  catatan_admin TEXT,
  inisiasi_oleh TEXT NOT NULL DEFAULT 'pelanggan' CHECK (inisiasi_oleh IN ('pelanggan','admin')),
  status_keputusan TEXT NOT NULL DEFAULT 'menunggu' CHECK (status_keputusan IN ('menunggu','disetujui','ditolak','dijadwalkan')),
  alasan_penolakan TEXT,
  diterapkan_pada TIMESTAMPTZ,
  admin_penanggung_jawab_id BIGINT REFERENCES users(id),
  admin_eksekutor_id BIGINT REFERENCES users(id),
  dibuat_pada TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
  diperbarui_pada TIMESTAMPTZ NOT NULL DEFAULT timezone('utc', now()),
  CONSTRAINT paket_diminta_berbeda CHECK (
    paket_sekarang_id IS NULL OR paket_diminta_id IS NULL OR paket_sekarang_id <> paket_diminta_id
  )
);

CREATE INDEX idx_ticket_perubahan_paket_status ON ticket_perubahan_paket(status_keputusan);
CREATE INDEX idx_ticket_perubahan_paket_diminta ON ticket_perubahan_paket(paket_diminta_id);
CREATE INDEX idx_ticket_perubahan_paket_customer ON ticket_perubahan_paket(customer_id);

CREATE UNIQUE INDEX idx_tickets_perubahan_paket_aktif
  ON tickets(customer_id)
  WHERE kategori = 'perubahan_paket'::kategori_tiket
    AND status IN ('open','in_progress');

CREATE OR REPLACE FUNCTION set_timestamp_ticket_perubahan_paket()
RETURNS TRIGGER AS $$
BEGIN
  NEW.diperbarui_pada = timezone('utc', now());
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ticket_perubahan_paket_set_timestamp
  BEFORE UPDATE ON ticket_perubahan_paket
  FOR EACH ROW
  EXECUTE FUNCTION set_timestamp_ticket_perubahan_paket();

-- business_settings
CREATE TABLE business_settings (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'string' CHECK (type IN ('string', 'number', 'boolean', 'json')),
  description TEXT,
  group_name TEXT,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT REFERENCES users(id) ON DELETE SET NULL
);
-- Migration Note (2025-11-29): Created business_settings table for centralized business configuration
-- See: migrations/002_create_business_settings.sql
-- Groups: company (name, logo, address), contact (email, phone, whatsapp),
--         billing (installation_fee, due_day), social (facebook, instagram, twitter),
--         operational (hours)

CREATE INDEX idx_business_settings_group ON business_settings(group_name);

CREATE OR REPLACE FUNCTION update_business_settings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_business_settings_timestamp
  BEFORE UPDATE ON business_settings
  FOR EACH ROW
  EXECUTE FUNCTION update_business_settings_timestamp();
