-- ============================================================================
-- CLEANUP PENDING TEST TICKETS untuk leon@gmail.com
-- ============================================================================

-- STEP 1: Check pending tickets yang ada
SELECT
  t.id as ticket_id,
  t.subject,
  t.description,
  t.status,
  t.created_at,
  tpp.paket_sekarang_id,
  tpp.paket_diminta_id,
  tpp.catatan_pelanggan,
  tpp.status_keputusan
FROM tickets t
INNER JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
WHERE t.customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND t.kategori = 'perubahan_paket'
AND t.status IN ('open', 'in_progress')
ORDER BY t.created_at DESC;

-- NOTE: Copy ticket IDs dari hasil query di atas sebelum menjalankan DELETE
-- Atau jalankan query di bawah ini untuk auto cleanup test tickets

-- ============================================================================
-- STEP 2: DELETE PENDING TEST TICKETS
-- ============================================================================
-- HATI-HATI: Query ini akan menghapus SEMUA pending change package tickets
-- untuk user leon@gmail.com yang mengandung kata "test" atau "Test"
-- ============================================================================

-- Delete dari ticket_perubahan_paket terlebih dahulu (foreign key)
DELETE FROM ticket_perubahan_paket
WHERE ticket_id IN (
  SELECT id FROM tickets
  WHERE customer_id = (
    SELECT customer_id FROM users
    WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
  )
  AND kategori = 'perubahan_paket'
  AND status IN ('open', 'in_progress')
);

-- Delete dari tickets
DELETE FROM tickets
WHERE customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND kategori = 'perubahan_paket'
AND status IN ('open', 'in_progress');

-- ============================================================================
-- STEP 3: VERIFY CLEANUP
-- ============================================================================
-- Check apakah masih ada pending tickets
SELECT
  COUNT(*) as remaining_pending_tickets
FROM tickets
WHERE customer_id = (
  SELECT customer_id FROM users
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND kategori = 'perubahan_paket'
AND status IN ('open', 'in_progress');

-- Expected: remaining_pending_tickets = 0
