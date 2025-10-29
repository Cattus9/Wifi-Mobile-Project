-- Enums
CREATE TYPE user_role AS ENUM ('admin', 'customer');
CREATE TYPE customer_status AS ENUM ('new', 'active', 'suspended', 'cancelled');
CREATE TYPE invoice_status AS ENUM ('draft', 'issued', 'overdue', 'paid', 'cancelled');
CREATE TYPE ticket_status AS ENUM ('open', 'in_progress', 'closed');
CREATE TYPE prioritas_tiket AS ENUM ('rendah', 'normal', 'tinggi', 'mendesak');
CREATE TYPE kategori_tiket AS ENUM ('koneksi', 'tagihan', 'instalasi', 'lainnya');
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
created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
CONSTRAINT fk_users_auth_user
FOREIGN KEY (auth_user_id)
REFERENCES auth.users(id)
ON DELETE CASCADE,
CONSTRAINT fk_users_customer
FOREIGN KEY (customer_id)
REFERENCES customers(id)
ON DELETE SET NULL
ON UPDATE CASCADE
);

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
CREATE UNIQUE INDEX idx_users_auth_user_id ON users(auth_user_id);
CREATE INDEX idx_invoices_customer_id ON invoices(customer_id);
CREATE INDEX idx_tickets_customer_id ON tickets(customer_id);
CREATE INDEX idx_tickets_status_prioritas ON tickets(status, prioritas);
CREATE INDEX idx_tickets_admin_penanggung_jawab ON tickets(admin_penanggung_jawab_id);
CREATE INDEX idx_tickets_sumber_menunggu_detail ON tickets(sumber, menunggu_detail);
CREATE INDEX idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);
CREATE INDEX idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);
