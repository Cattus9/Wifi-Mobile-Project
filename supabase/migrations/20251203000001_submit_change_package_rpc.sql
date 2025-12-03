-- Migration: Create RPC function for Change Package
-- Purpose: Move business logic from PHP to Supabase
-- Author: Migration from Dual Backend to Supabase-First
-- Date: 2025-12-03

-- ============================================================================
-- FUNCTION: submit_change_package
-- ============================================================================
-- This function handles all business logic for submitting a change package request.
-- It performs validations and creates ticket atomically in a single transaction.
--
-- Parameters:
--   p_auth_user_id: UUID of the authenticated user (from JWT)
--   p_package_id: BIGINT of the new package to change to
--   p_notes: TEXT optional notes from customer
--
-- Returns: JSON object with ticket details or error
-- ============================================================================

CREATE OR REPLACE FUNCTION submit_change_package(
  p_auth_user_id UUID,
  p_package_id BIGINT,
  p_notes TEXT DEFAULT NULL
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER -- Run with function creator privileges
SET search_path = public -- Security: explicit schema
AS $$
DECLARE
  v_customer_id BIGINT;
  v_current_package_id BIGINT;
  v_current_package_name TEXT;
  v_new_package_name TEXT;
  v_outstanding_count INT;
  v_pending_count INT;
  v_ticket_id BIGINT;
  v_package_exists BOOLEAN;
  v_result JSON;
BEGIN
  -- =========================================================================
  -- STEP 1: Get customer_id from auth_user_id
  -- =========================================================================
  SELECT customer_id
  INTO v_customer_id
  FROM users
  WHERE auth_user_id = p_auth_user_id
    AND is_active = true;

  IF v_customer_id IS NULL THEN
    RAISE EXCEPTION 'CUSTOMER_NOT_FOUND: Data customer tidak ditemukan atau user tidak aktif';
  END IF;

  -- =========================================================================
  -- STEP 2: Validate new package exists and is active
  -- =========================================================================
  SELECT
    EXISTS(SELECT 1 FROM service_packages WHERE id = p_package_id AND is_active = true),
    name
  INTO v_package_exists, v_new_package_name
  FROM service_packages
  WHERE id = p_package_id;

  IF NOT v_package_exists THEN
    RAISE EXCEPTION 'PACKAGE_NOT_AVAILABLE: Paket dengan ID % tidak tersedia atau tidak aktif', p_package_id;
  END IF;

  -- =========================================================================
  -- STEP 3: Get current package
  -- =========================================================================
  SELECT
    c.service_package_id,
    sp.name
  INTO
    v_current_package_id,
    v_current_package_name
  FROM customers c
  LEFT JOIN service_packages sp ON c.service_package_id = sp.id
  WHERE c.id = v_customer_id;

  -- =========================================================================
  -- STEP 4: Validate new package is different from current
  -- =========================================================================
  IF v_current_package_id IS NOT NULL AND p_package_id = v_current_package_id THEN
    RAISE EXCEPTION 'PACKAGE_SAME_AS_CURRENT: Paket yang dipilih (%) sudah aktif saat ini', v_current_package_name;
  END IF;

  -- =========================================================================
  -- STEP 5: Check for outstanding invoices
  -- =========================================================================
  SELECT COUNT(*)
  INTO v_outstanding_count
  FROM invoices
  WHERE customer_id = v_customer_id
    AND status IN ('issued', 'overdue', 'draft');

  IF v_outstanding_count > 0 THEN
    RAISE EXCEPTION 'OUTSTANDING_INVOICE: Tidak dapat mengajukan perubahan paket. Harap selesaikan % tagihan tertunda terlebih dahulu', v_outstanding_count;
  END IF;

  -- =========================================================================
  -- STEP 6: Check for existing pending change package requests
  -- =========================================================================
  SELECT COUNT(*)
  INTO v_pending_count
  FROM tickets
  WHERE customer_id = v_customer_id
    AND kategori = 'perubahan_paket'
    AND status IN ('open', 'in_progress');

  IF v_pending_count > 0 THEN
    RAISE EXCEPTION 'PENDING_REQUEST: Permintaan perubahan paket aktif sudah ada. Harap tunggu hingga permintaan sebelumnya diproses';
  END IF;

  -- =========================================================================
  -- STEP 7: Create ticket (main record)
  -- =========================================================================
  INSERT INTO tickets (
    customer_id,
    subject,
    description,
    kategori,
    status,
    prioritas,
    sumber,
    created_at,
    diperbarui_pada
  ) VALUES (
    v_customer_id,
    'Permintaan perubahan paket',
    COALESCE(p_notes, 'Permintaan perubahan paket layanan'),
    'perubahan_paket',
    'open',
    'normal',
    'web', -- Changed from 'mobile' to match backend convention
    NOW(),
    NOW()
  )
  RETURNING id INTO v_ticket_id;

  -- =========================================================================
  -- STEP 8: Create ticket_perubahan_paket (detail record)
  -- =========================================================================
  INSERT INTO ticket_perubahan_paket (
    ticket_id,
    customer_id,
    paket_sekarang_id,
    paket_diminta_id,
    catatan_pelanggan,
    inisiasi_oleh,
    status_keputusan,
    dibuat_pada,
    diperbarui_pada
  ) VALUES (
    v_ticket_id,
    v_customer_id,
    v_current_package_id,
    p_package_id,
    p_notes,
    'pelanggan',
    'menunggu',
    NOW(),
    NOW()
  );

  -- =========================================================================
  -- STEP 9: Build success response
  -- =========================================================================
  v_result := json_build_object(
    'success', true,
    'ticket_id', v_ticket_id,
    'status', 'pending',
    'current_package', COALESCE(v_current_package_name, 'Belum ada paket'),
    'requested_package', v_new_package_name,
    'notes', COALESCE(p_notes, ''),
    'message', 'Permintaan perubahan paket berhasil dikirim. Paket baru akan diproses admin dan aktif pada periode berikutnya setelah disetujui.'
  );

  RETURN v_result;

EXCEPTION
  WHEN OTHERS THEN
    -- Catch and re-raise with proper error format
    RAISE EXCEPTION '%', SQLERRM;
END;
$$;

-- ============================================================================
-- GRANT PERMISSIONS
-- ============================================================================
-- Allow authenticated users to call this function
GRANT EXECUTE ON FUNCTION submit_change_package TO authenticated;

-- ============================================================================
-- FUNCTION: get_active_change_package_status
-- ============================================================================
-- Get active change package request status for current user
-- ============================================================================

CREATE OR REPLACE FUNCTION get_active_change_package_status(
  p_auth_user_id UUID
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_customer_id BIGINT;
  v_result JSON;
BEGIN
  -- Get customer_id
  SELECT customer_id
  INTO v_customer_id
  FROM users
  WHERE auth_user_id = p_auth_user_id
    AND is_active = true;

  IF v_customer_id IS NULL THEN
    RAISE EXCEPTION 'CUSTOMER_NOT_FOUND';
  END IF;

  -- Get active ticket with details
  SELECT json_build_object(
    'ticket_id', t.id,
    'status', t.status,
    'status_keputusan', tpp.status_keputusan,
    'paket_sekarang_id', tpp.paket_sekarang_id,
    'paket_diminta_id', tpp.paket_diminta_id,
    'catatan_admin', tpp.catatan_admin,
    'catatan_pelanggan', tpp.catatan_pelanggan,
    'jadwal_aktivasi', tpp.jadwal_aktivasi,
    'diterapkan_pada', tpp.diterapkan_pada,
    'created_at', t.created_at
  )
  INTO v_result
  FROM tickets t
  INNER JOIN ticket_perubahan_paket tpp ON t.id = tpp.ticket_id
  WHERE t.customer_id = v_customer_id
    AND t.kategori = 'perubahan_paket'
    AND t.status IN ('open', 'in_progress')
  ORDER BY t.created_at DESC
  LIMIT 1;

  IF v_result IS NULL THEN
    -- No active request
    RETURN NULL;
  END IF;

  RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION get_active_change_package_status TO authenticated;

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON FUNCTION submit_change_package IS
  'Handles change package request submission with all validations. ' ||
  'Validates invoices, existing requests, package availability. ' ||
  'Creates ticket and detail records atomically.';

COMMENT ON FUNCTION get_active_change_package_status IS
  'Returns active change package request status for authenticated user.';
