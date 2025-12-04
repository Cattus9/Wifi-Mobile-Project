-- Migration: Create RPC function for User Registration
-- Purpose: Register new user with customer data in Supabase
-- Date: 2025-12-04

-- ============================================================================
-- FUNCTION: register_user
-- ============================================================================
-- This function handles user registration by creating both customer and user records.
-- It should be called AFTER Supabase Auth signup.
--
-- Parameters:
--   p_auth_user_id: UUID from Supabase Auth (auth.users.id)
--   p_email: TEXT user's email
--   p_phone: TEXT user's phone number
--   p_name: TEXT user's full name
--   p_address: TEXT user's address
--
-- Returns: JSON object with success/error
-- ============================================================================

CREATE OR REPLACE FUNCTION register_user(
  p_auth_user_id UUID,
  p_email TEXT,
  p_phone TEXT,
  p_name TEXT,
  p_address TEXT
)
RETURNS JSON
LANGUAGE plpgsql
SECURITY DEFINER -- Run with function creator privileges
SET search_path = public -- Security: explicit schema
AS $$
DECLARE
  v_customer_id BIGINT;
  v_user_id BIGINT;
  v_existing_email_count INT;
  v_existing_user_count INT;
  v_result JSON;
BEGIN
  -- =========================================================================
  -- STEP 1: Validate email is not already registered
  -- =========================================================================
  SELECT COUNT(*)
  INTO v_existing_email_count
  FROM users
  WHERE email = p_email;

  IF v_existing_email_count > 0 THEN
    RAISE EXCEPTION 'EMAIL_ALREADY_EXISTS: Email sudah terdaftar dalam sistem';
  END IF;

  -- =========================================================================
  -- STEP 2: Validate auth_user_id is not already linked
  -- =========================================================================
  SELECT COUNT(*)
  INTO v_existing_user_count
  FROM users
  WHERE auth_user_id = p_auth_user_id;

  IF v_existing_user_count > 0 THEN
    RAISE EXCEPTION 'AUTH_USER_ALREADY_LINKED: User auth sudah terhubung dengan akun lain';
  END IF;

  -- =========================================================================
  -- STEP 3: Create customer record
  -- =========================================================================
  INSERT INTO customers (
    name,
    phone,
    address,
    status,
    service_package_id,
    created_at,
    updated_at
  ) VALUES (
    p_name,
    p_phone,
    p_address,
    'inactive', -- New customer starts as inactive until admin activates
    NULL, -- No package assigned yet
    NOW(),
    NOW()
  )
  RETURNING id INTO v_customer_id;

  -- =========================================================================
  -- STEP 4: Create user record
  -- =========================================================================
  INSERT INTO users (
    auth_user_id,
    customer_id,
    email,
    phone,
    full_name,
    role,
    is_active,
    created_at,
    updated_at
  ) VALUES (
    p_auth_user_id,
    v_customer_id,
    p_email,
    p_phone,
    p_name,
    'customer', -- Default role
    true, -- User is active
    NOW(),
    NOW()
  )
  RETURNING id INTO v_user_id;

  -- =========================================================================
  -- STEP 5: Build success response
  -- =========================================================================
  v_result := json_build_object(
    'success', true,
    'user_id', v_user_id,
    'customer_id', v_customer_id,
    'message', 'Registrasi berhasil! Akun Anda telah dibuat. Silakan hubungi admin untuk aktivasi layanan.'
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
-- Allow anonymous users to call this function (for registration)
GRANT EXECUTE ON FUNCTION register_user TO anon;
GRANT EXECUTE ON FUNCTION register_user TO authenticated;

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON FUNCTION register_user IS
  'Handles new user registration by creating customer and user records. ' ||
  'Should be called after Supabase Auth signup. ' ||
  'Validates email uniqueness before creating records.';
