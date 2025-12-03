-- Check pending requests untuk leon@gmail.com
SELECT 
  t.id as ticket_id,
  t.subject,
  t.status,
  t.created_at,
  tpp.paket_sekarang_id,
  tpp.paket_diminta_id,
  tpp.catatan_pelanggan
FROM tickets t
INNER JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
WHERE t.customer_id = (
  SELECT customer_id FROM users 
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND t.kategori = 'perubahan_paket'
AND t.status IN ('open', 'in_progress')
ORDER BY t.created_at DESC;

-- CLEANUP: Delete pending test requests (OPTIONAL - run jika mau cleanup)
-- Uncomment lines below untuk cleanup:
/*
DELETE FROM ticket_perubahan_paket 
WHERE ticket_id IN (
  SELECT id FROM tickets 
  WHERE customer_id = (
    SELECT customer_id FROM users 
    WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
  )
  AND kategori = 'perubahan_paket'
  AND status IN ('open', 'in_progress')
  AND (description LIKE '%Test%' OR description LIKE '%test%')
);

DELETE FROM tickets 
WHERE customer_id = (
  SELECT customer_id FROM users 
  WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'
)
AND kategori = 'perubahan_paket'
AND status IN ('open', 'in_progress')
AND (description LIKE '%Test%' OR description LIKE '%test%');
*/
