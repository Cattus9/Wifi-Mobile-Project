// Edge Function: change-package
// Purpose: HTTP endpoint untuk mobile app submit change package request
// Calls RPC function: submit_change_package

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// CORS headers untuk allow mobile app akses endpoint
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // Handle CORS preflight request
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // 1. Get Authorization header (JWT token dari mobile)
    const authHeader = req.headers.get('Authorization')

    if (!authHeader) {
      throw new Error('UNAUTHORIZED: Missing authorization header')
    }

    // 2. Create Supabase client dengan user's JWT token
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: { headers: { Authorization: authHeader } }
      }
    )

    // 3. Validate JWT dan get authenticated user
    const { data: { user }, error: userError } = await supabaseClient.auth.getUser()

    if (userError || !user) {
      throw new Error('UNAUTHORIZED: Invalid or expired token')
    }

    // 4. Parse request body dari mobile app
    const requestBody = await req.json()
    const { package_id, notes } = requestBody

    // Validate required fields
    if (!package_id) {
      throw new Error('BAD_REQUEST: package_id is required')
    }

    // 5. Call RPC function submit_change_package
    const { data, error } = await supabaseClient.rpc('submit_change_package', {
      p_auth_user_id: user.id,
      p_package_id: package_id,
      p_notes: notes || null
    })

    // 6. Handle RPC error (dari validations)
    if (error) {
      // Error dari RPC function (OUTSTANDING_INVOICE, PENDING_REQUEST, etc.)
      return new Response(
        JSON.stringify({
          success: false,
          error_code: error.message.split(':')[0] || 'UNKNOWN_ERROR',
          message: error.message
        }),
        {
          headers: { ...corsHeaders, 'Content-Type': 'application/json' },
          status: 400
        }
      )
    }

    // 7. Success! Return data dari RPC function
    return new Response(
      JSON.stringify({
        success: true,
        data: data
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 200
      }
    )

  } catch (error) {
    // Handle unexpected errors
    console.error('Edge Function error:', error)

    return new Response(
      JSON.stringify({
        success: false,
        error_code: 'INTERNAL_ERROR',
        message: error.message || 'An unexpected error occurred'
      }),
      {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 500
      }
    )
  }
})
