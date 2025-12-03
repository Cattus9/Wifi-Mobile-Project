-- Get user data untuk leon@gmail.com
SELECT 
  u.id as user_id,
  u.auth_user_id,
  u.email,
  u.customer_id,
  c.service_package_id as current_package_id,
  sp.name as current_package_name
FROM users u
LEFT JOIN customers c ON u.customer_id = c.id
LEFT JOIN service_packages sp ON c.service_package_id = sp.id
WHERE u.auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801'::uuid;

-- Get available packages (untuk pilih package yang berbeda)
SELECT id, name, speed, price, is_active
FROM service_packages
WHERE is_active = true
ORDER BY id;

-- Check apakah ada pending requests
SELECT COUNT(*) as pending_count
FROM tickets
WHERE customer_id = (SELECT customer_id FROM users WHERE auth_user_id = '28833d1c-c016-4721-86e6-ffa56b9a6801')
  AND kategori = 'perubahan_paket'
  AND status IN ('open', 'in_progress');
