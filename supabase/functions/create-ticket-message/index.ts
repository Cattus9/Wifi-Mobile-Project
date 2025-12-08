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

    const { ticket_id, isi } = await req.json();

    // --- Validation ---
    if (!ticket_id || !isi) {
      return new Response(JSON.stringify({ error: 'ticket_id and isi are required' }), {
        headers: { ...corsHeaders, 'Content-Type': 'application/json' },
        status: 400,
      });
    }

    // --- Logic ---
    // 1. Get customer profile
    const { data: userData, error: userError } = await supabase
      .from('users')
      .select('customer_id')
      .eq('auth_user_id', user.id)
      .single();

    if (userError || !userData) {
      throw new Error('Customer not found for the authenticated user.');
    }
    const customerId = userData.customer_id;

    // 2. Verify ticket ownership and status
    const { data: ticket, error: ticketError } = await supabase
      .from('tickets')
      .select('customer_id, status')
      .eq('id', ticket_id)
      .single();

    if (ticketError || !ticket) {
      throw new Error('Ticket not found.');
    }
    if (ticket.customer_id !== customerId) {
        return new Response(JSON.stringify({ error: 'User is not the owner of this ticket.' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 403, // Forbidden
        });
    }
    if (ticket.status === 'closed') {
        return new Response(JSON.stringify({ error: 'Cannot add message to a closed ticket.' }), {
            headers: { ...corsHeaders, 'Content-Type': 'application/json' },
            status: 400,
        });
    }


    // 3. Insert new message
    const newMessage = {
      ticket_id,
      isi,
      tipe_penulis: 'customer',
      penulis_id: customerId
    };

    const { data: messageData, error: messageError } = await supabase
      .from('ticket_messages')
      .insert(newMessage)
      .select()
      .single();

    if (messageError) {
      throw messageError;
    }

    return new Response(JSON.stringify({ success: true, data: messageData }), {
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
