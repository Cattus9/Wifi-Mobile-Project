# 04 - Edge Functions Implementation

> **Purpose:** Complete guide untuk create & deploy Supabase Edge Function sebagai wrapper untuk RPC functions.

---

## 📋 **OVERVIEW**

**What are Edge Functions?**
- Serverless TypeScript/JavaScript functions yang run on Deno runtime
- Deployed di Supabase edge network (globally distributed)
- Auto-scaling, no server management needed
- Perfect untuk API endpoints yang perlu authentication

**Why do we need Edge Function?**
- Mobile app needs REST API endpoint (not raw RPC)
- Handle authentication & token validation
- Provide clean error responses
- Add CORS support for web clients
- Logging & monitoring

---

## 🎯 **ARCHITECTURE**

```
Mobile App
    │
    └─→ POST /functions/v1/change-package
        │ Headers: Authorization: Bearer {jwt_token}
        │ Body: { package_id: 2, notes: "..." }
        │
        ↓
    Edge Function (change-package)
        │
        ├─→ (1) Validate JWT token
        ├─→ (2) Extract user_id from token
        └─→ (3) Call RPC function
            │
            ↓
        Database RPC: submit_change_package()
            │
            └─→ Return JSON response
```

---

## 📝 **EDGE FUNCTION CODE**

### **File Structure**

```
supabase/
└── functions/
    └── change-package/
        └── index.ts
```

### **Complete Code: index.ts**

```typescript
// Import Deno standard library
import { serve } from 'https://deno.land/std@0.168.0/http/server.ts'
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

// CORS headers for web clients
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// Main handler
serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // ======================
    // 1. AUTHENTICATION
    // ======================

    // Get JWT token from Authorization header
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      throw new Error('UNAUTHORIZED: Missing authorization header')
    }

    // Create Supabase client with user's token
    const supabaseClient = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_ANON_KEY') ?? '',
      {
        global: {
          headers: { Authorization: authHeader }
        }
      }
    )

    // Verify token and get user
    const {
      data: { user },
      error: userError
    } = await supabaseClient.auth.getUser()

    if (userError || !user) {
      throw new Error('UNAUTHORIZED: Invalid or expired token')
    }

    // ======================
    // 2. PARSE REQUEST
    // ======================

    const { package_id, notes } = await req.json()

    // Validate required fields
    if (!package_id) {
      throw new Error('VALIDATION_ERROR: package_id is required')
    }

    // ======================
    // 3. CALL RPC FUNCTION
    // ======================

    const { data, error } = await supabaseClient.rpc('submit_change_package', {
      p_auth_user_id: user.id,
      p_package_id: package_id,
      p_notes: notes || null
    })

    if (error) {
      // Extract error code from RPC exception
      // Format: "ERROR_CODE: Human message"
      const errorParts = error.message.split(':')
      const errorCode = errorParts[0].trim()
      const errorMessage = errorParts.slice(1).join(':').trim()

      throw new Error(`${errorCode}: ${errorMessage}`)
    }

    // ======================
    // 4. SUCCESS RESPONSE
    // ======================

    return new Response(
      JSON.stringify({
        success: true,
        data: data
      }),
      {
        headers: {
          ...corsHeaders,
          'Content-Type': 'application/json'
        },
        status: 200
      }
    )

  } catch (error) {
    // ======================
    // 5. ERROR HANDLING
    // ======================

    const errorMessage = error.message || 'Unknown error'
    const errorParts = errorMessage.split(':')
    const errorCode = errorParts[0].trim()
    const userMessage = errorParts.slice(1).join(':').trim() || errorMessage

    // Determine HTTP status code
    let statusCode = 400
    if (errorCode === 'UNAUTHORIZED') {
      statusCode = 401
    } else if (errorCode === 'CUSTOMER_NOT_FOUND' || errorCode === 'PACKAGE_NOT_AVAILABLE') {
      statusCode = 404
    }

    return new Response(
      JSON.stringify({
        success: false,
        error_code: errorCode,
        message: userMessage
      }),
      {
        headers: {
          ...corsHeaders,
          'Content-Type': 'application/json'
        },
        status: statusCode
      }
    )
  }
})
```

---

## 🚀 **DEPLOYMENT OPTIONS**

### **Option 1: Via Supabase Dashboard (RECOMMENDED)**

#### **Step-by-Step:**

1. **Go to Functions Page**
   ```
   URL: https://supabase.com/dashboard/project/rqmzvonjytyjdfhpqwvc/functions
   ```

2. **Create New Function**
   - Click "Create a new function"
   - Name: `change-package`
   - Click "Create function"

3. **Copy Code**
   - Copy the entire `index.ts` code above
   - Paste into the editor

4. **Deploy**
   - Click "Deploy" button
   - Wait for deployment to complete (~30 seconds)

5. **Verify**
   ```
   Should see: "Function deployed successfully"
   Endpoint: https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package
   ```

---

### **Option 2: Via Supabase CLI (If CLI Available)**

**Prerequisites:**
- Supabase CLI installed
- Logged in: `supabase login`
- Project linked: `supabase link --project-ref rqmzvonjytyjdfhpqwvc`

**Deploy Command:**
```bash
# Deploy single function
supabase functions deploy change-package

# Expected output:
# Deploying function change-package...
# Function deployed successfully!
```

---

### **Option 3: Via REST API (Advanced)**

**Using curl:**

```bash
# Get your access token from Supabase Dashboard > Settings > API
ACCESS_TOKEN="your_supabase_access_token"
PROJECT_REF="rqmzvonjytyjdfhpqwvc"

# Create function
curl -X POST \
  "https://api.supabase.com/v1/projects/${PROJECT_REF}/functions" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "slug": "change-package",
    "name": "change-package",
    "verify_jwt": true
  }'

# Deploy code
curl -X POST \
  "https://api.supabase.com/v1/projects/${PROJECT_REF}/functions/change-package/deploy" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -F "index.ts=@supabase/functions/change-package/index.ts"
```

---

## 🧪 **TESTING EDGE FUNCTION**

### **Test 1: Via curl**

```bash
# Get your JWT token (from mobile app or Supabase dashboard)
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Test success case
curl -X POST \
  https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "package_id": 2,
    "notes": "Test from curl"
  }'
```

**Expected Response (Success):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "ticket_id": 123,
    "status": "pending",
    "current_package": "Basic 20 Mbps",
    "requested_package": "Super 50 Mbps",
    "notes": "Test from curl",
    "message": "Permintaan berhasil dikirim..."
  }
}
```

**Expected Response (Error):**
```json
{
  "success": false,
  "error_code": "PENDING_REQUEST",
  "message": "Masih ada permintaan aktif yang sedang diproses"
}
```

---

### **Test 2: Via Postman**

**Setup:**
1. Create new POST request
2. URL: `https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package`
3. Headers:
   - `Authorization: Bearer {your_jwt_token}`
   - `Content-Type: application/json`
4. Body (raw JSON):
   ```json
   {
     "package_id": 2,
     "notes": "Test from Postman"
   }
   ```
5. Send request

---

### **Test 3: Via Supabase Dashboard**

1. Go to Functions page
2. Click on `change-package` function
3. Go to "Invocations" tab
4. Click "Invoke function"
5. Enter test payload:
   ```json
   {
     "package_id": 2,
     "notes": "Test from dashboard"
   }
   ```
6. Click "Run"

---

## 🔐 **ENVIRONMENT VARIABLES**

Edge Functions automatically have access to these environment variables:

| Variable | Value | Description |
|----------|-------|-------------|
| `SUPABASE_URL` | `https://rqmzvonjytyjdfhpqwvc.supabase.co` | Your Supabase URL |
| `SUPABASE_ANON_KEY` | `eyJhbG...` (auto-injected) | Public anon key |
| `SUPABASE_SERVICE_ROLE_KEY` | `eyJhbG...` (auto-injected) | Service role key |

**No manual setup needed!** Supabase automatically injects these.

---

## 📊 **MONITORING & LOGS**

### **View Logs**

1. **Via Dashboard:**
   ```
   Go to: Functions > change-package > Logs
   Real-time logs will appear here
   ```

2. **Via CLI:**
   ```bash
   supabase functions logs change-package --follow
   ```

### **What to Monitor:**

- **Invocation Count:** How many times function is called
- **Error Rate:** Percentage of failed requests
- **Response Time:** P50, P95, P99 latency
- **Error Messages:** Check for common errors

**Access Monitoring:**
```
Dashboard > Functions > change-package > Metrics
```

---

## 🐛 **TROUBLESHOOTING**

### **Issue 1: "Function not found"**

**Cause:** Function not deployed or wrong URL

**Solution:**
```bash
# Verify function exists
curl https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package

# Should NOT return 404
```

---

### **Issue 2: "UNAUTHORIZED: Invalid token"**

**Cause:** JWT token expired or invalid

**Solution:**
```bash
# Get fresh token
# Option 1: From mobile app (after login)
# Option 2: From Supabase dashboard SQL:

SELECT
  auth.sign(
    json_build_object(
      'role', 'authenticated',
      'sub', auth_user_id::text,
      'exp', extract(epoch from now() + interval '1 hour')
    ),
    'your-jwt-secret'
  );
```

---

### **Issue 3: CORS errors (web clients)**

**Cause:** Missing CORS headers

**Solution:**
Already handled in code! Check `corsHeaders` object includes:
```typescript
'Access-Control-Allow-Origin': '*'
```

---

### **Issue 4: "Cannot read property 'rpc' of undefined"**

**Cause:** Supabase client creation failed

**Solution:**
Check environment variables are available:
```typescript
console.log('SUPABASE_URL:', Deno.env.get('SUPABASE_URL'))
console.log('SUPABASE_ANON_KEY:', Deno.env.get('SUPABASE_ANON_KEY'))
```

---

## 📈 **PERFORMANCE OPTIMIZATION**

### **Current Performance:**
- Cold start: ~500ms (first request)
- Warm requests: ~100-200ms
- Total (including RPC): ~300-500ms

### **Tips:**
1. **Keep functions warm:** Regular health checks
2. **Minimize dependencies:** Only import what you need
3. **Use connection pooling:** Supabase client handles this
4. **Cache static data:** Not needed for this function

---

## 🔗 **ENDPOINT DETAILS**

### **Production Endpoint:**
```
POST https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package
```

### **Request Format:**
```json
{
  "package_id": 2,
  "notes": "Optional customer notes"
}
```

### **Response Format (Success):**
```json
{
  "success": true,
  "data": {
    "success": true,
    "ticket_id": 123,
    "status": "pending",
    "current_package": "Basic 20 Mbps",
    "requested_package": "Super 50 Mbps",
    "notes": "...",
    "message": "..."
  }
}
```

### **Response Format (Error):**
```json
{
  "success": false,
  "error_code": "ERROR_CODE",
  "message": "Human-readable error message"
}
```

### **HTTP Status Codes:**
- `200` - Success
- `400` - Validation error or business logic error
- `401` - Unauthorized (missing/invalid token)
- `404` - Resource not found (customer, package)
- `500` - Internal server error

---

## ✅ **DEPLOYMENT CHECKLIST**

Before deploying:
- [ ] RPC functions deployed & tested (from Step 3)
- [ ] Code copied correctly (no syntax errors)
- [ ] Function name is `change-package` (lowercase, hyphen)
- [ ] CORS headers included

After deploying:
- [ ] Function appears in dashboard
- [ ] Endpoint URL accessible
- [ ] Test with curl (success case)
- [ ] Test with curl (error case)
- [ ] Check logs for any errors

---

## 📚 **REFERENCES**

- **Edge Functions Docs:** https://supabase.com/docs/guides/functions
- **Deno Runtime:** https://deno.land/manual
- **Supabase JS Client:** https://supabase.com/docs/reference/javascript/

---

**Document Version:** 1.0
**Last Updated:** 2025-12-03
**Next:** [05-MobileIntegration.md](./05-MobileIntegration.md)
