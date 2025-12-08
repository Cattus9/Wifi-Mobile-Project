import { serve } from 'https://deno.land/std@0.177.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';
import { corsHeaders } from '../_shared/cors.ts';

serve(async (req) => {
  // Handle CORS preflight request
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    const supabase = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    );

    // Get user from JWT
    const authHeader = req.headers.get('Authorization');
    const { data: { user } } = await supabase.auth.getUser(authHeader.replace('Bearer ', ''));
    if (!user) {
      return new Response(JSON.stringify({ error: 'Unauthorized' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 401,
      });
    }

    const { kategori, subject, description } = await req.json();

    // --- Validation ---
    if (!kategori || !subject || !description) {
      return new Response(JSON.stringify({ error: 'Kategori, subject, and description are required' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      });
    }

    // --- Logic ---
    // 1. Get customer_id from users table
    const { data: userData, error: userError } = await supabase
      .from('users')
      .select('customer_id')
      .eq('auth_user_id', user.id)
      .single();

    if (userError || !userData) {
      throw new Error('Customer not found for the authenticated user.');
    }
    const customerId = userData.customer_id;

    // 2. Check for existing open tickets
    const { data: existingTicket, error: existingTicketError } = await supabase
      .from('tickets')
      .select('id, status')
      .eq('customer_id', customerId)
      .in('status', ['open', 'in_progress']);
    
    if (existingTicketError) {
      throw new Error('Failed to check for existing tickets: ' + existingTicketError.message);
    }
    
    if (existingTicket && existingTicket.length > 0) {
      return new Response(JSON.stringify({ error: 'Anda sudah memiliki tiket yang sedang aktif. Harap selesaikan tiket tersebut terlebih dahulu.' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 409, // Conflict
      });
    }

    // 3. Insert new ticket
    const newTicket = {
      customer_id: customerId,
      kategori,
      subject,
      description,
      status: 'open', // Default status
      prioritas: kategori === 'mendesak' ? 'mendesak' : 'normal', // Example logic
      sumber: 'web' // FIX: Use a valid enum value. 'web' is a safe default.
    };

    const { data: ticketData, error: ticketError } = await supabase
      .from('tickets')
      .insert(newTicket)
      .select()
      .single();

    if (ticketError) {
      throw ticketError;
    }

    const responsePayload = {
      ticket: ticketData,
      whatsapp_number: null
    };

    if (kategori === 'mendesak') {
      const { data: settingData, error: settingError } = await supabase
        .from('business_settings')
        .select('value')
        .eq('key', 'contact_whatsapp')
        .single();

      if (settingError) {
        console.error('WhatsApp number not found in business_settings, but proceeding.');
      } else {
        responsePayload.whatsapp_number = settingData.value;
      }
    }

    return new Response(JSON.stringify(responsePayload), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 201,
    });

  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      status: 500,
    });
  }
});
