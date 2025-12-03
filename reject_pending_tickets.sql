-- ============================================================================
-- UPDATE PENDING TICKETS STATUS menjadi DITOLAK
-- ============================================================================
-- User: leon@gmail.com
-- Auth User ID: 28833d1c-c016-4721-86e6-ffa56b9a6801
-- ============================================================================

-- STEP 1: Check pending tickets sebelum update
SELECT
  t.id as ticket_id,
  t.subject,
  t.status as current_status,
  t.created_at,
  tpp.status_keputusan as current_keputusan,
  tpp.catatan_pelanggan
FROM tickets t
INNER JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
WHERE t.customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND t.kategori = 'perubahan_paket'
AND t.status IN ('open', 'in_progress');

-- ============================================================================
-- STEP 2: UPDATE STATUS menjadi DITOLAK
-- ============================================================================

-- Update ticket_perubahan_paket (status_keputusan dan catatan_admin)
UPDATE ticket_perubahan_paket
SET
  status_keputusan = 'ditolak',
  catatan_admin = 'Ditolak untuk keperluan testing Edge Function dan mobile app integration',
  diperbarui_pada = NOW()
WHERE ticket_id IN (
  SELECT id FROM tickets
  WHERE customer_id = (
    SELECT customer_id FROM users
    WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
  )
  AND kategori = 'perubahan_paket'
  AND status IN ('open', 'in_progress')
);

-- Update tickets (status menjadi closed)
UPDATE tickets
SET
  status = 'closed',
  diperbarui_pada = NOW()
WHERE customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND kategori = 'perubahan_paket'
AND status IN ('open', 'in_progress');

-- ============================================================================
-- STEP 3: VERIFY UPDATE SUCCESS
-- ============================================================================

-- Check apakah masih ada pending tickets (should be 0)
SELECT
  COUNT(*) as remaining_pending_tickets
FROM tickets
WHERE customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND kategori = 'perubahan_paket'
AND status IN ('open', 'in_progress');
-- Expected: 0

-- Check rejected tickets (should show the updated tickets)
SELECT
  t.id as ticket_id,
  t.subject,
  t.status as new_status,
  t.created_at,
  t.diperbarui_pada as updated_at,
  tpp.status_keputusan as new_keputusan,
  tpp.catatan_admin,
  tpp.catatan_pelanggan
FROM tickets t
INNER JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
WHERE t.customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND t.kategori = 'perubahan_paket'
AND t.status = 'closed'
AND tpp.status_keputusan = 'ditolak'
ORDER BY t.diperbarui_pada DESC;

-- ============================================================================
-- ALTERNATIVE: Jika ingin reject hanya 1 ticket spesifik
-- ============================================================================
-- Uncomment dan ganti TICKET_ID dengan ID ticket yang ingin ditolak

/*
-- Update 1 ticket spesifik
UPDATE ticket_perubahan_paket
SET
  status_keputusan = 'ditolak',
  catatan_admin = 'Ditolak: [ALASAN PENOLAKAN]',
  diperbarui_pada = NOW()
WHERE ticket_id = TICKET_ID;

UPDATE tickets
SET
  status = 'closed',
  diperbarui_pada = NOW()
WHERE id = TICKET_ID;
*/
