#!/bin/bash

# Test Edge Function: change-package
# User: leon@gmail.com
# Auth User ID: 28833d1c-c016-4721-86e6-ffa56b9a6801

EDGE_FUNCTION_URL="https://rqmzvonjytyjdfhpqwvc.supabase.co/functions/v1/change-package"
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsImtpZCI6Ik83My9VMDVQclVqYS9HaDQiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL3JxbXp2b25qeXR5amRmaHBxd3ZjLnN1cGFiYXNlLmNvL2F1dGgvdjEiLCJzdWIiOiIyODgzM2QxYy1jMDE2LTQ3MjEtODZlNi1mZmE1NmI5YTY4MDEiLCJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNzY0NzU2NzQ3LCJpYXQiOjE3NjQ3NTMxNDcsImVtYWlsIjoibGVvbkBnbWFpbC5jb20iLCJwaG9uZSI6IiIsImFwcF9tZXRhZGF0YSI6eyJwcm92aWRlciI6ImVtYWlsIiwicHJvdmlkZXJzIjpbImVtYWlsIl0sInJvbGUiOiJjdXN0b21lciJ9LCJ1c2VyX21ldGFkYXRhIjp7ImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwicm9sZSI6ImF1dGhlbnRpY2F0ZWQiLCJhYWwiOiJhYWwxIiwiYW1yIjpbeyJtZXRob2QiOiJwYXNzd29yZCIsInRpbWVzdGFtcCI6MTc2NDc1MzE0N31dLCJzZXNzaW9uX2lkIjoiNWYxNTU0YjktMzc4Ny00NzYzLTllMjYtZjcxOThlODY2ZTRkIiwiaXNfYW5vbnltb3VzIjpmYWxzZX0.NoXR2kAkHvYZesLGd2XHiSByKmuhyqIXkHFUIi7_imE"

echo "=========================================="
echo "TEST EDGE FUNCTION: change-package"
echo "User: leon@gmail.com"
echo "=========================================="
echo ""

# TEST 1: Missing Authorization Header
echo "TEST 1: Missing Authorization Header (Should FAIL)"
echo "--------------------------------------------------"
curl -X POST "$EDGE_FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -d '{"package_id": 2, "notes": "Test without auth"}' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

# TEST 2: Invalid Token
echo "TEST 2: Invalid Token (Should FAIL)"
echo "------------------------------------"
curl -X POST "$EDGE_FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer invalid-fake-token-123" \
  -d '{"package_id": 2, "notes": "Test with fake token"}' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

# TEST 3: Missing package_id
echo "TEST 3: Missing package_id (Should FAIL)"
echo "-----------------------------------------"
curl -X POST "$EDGE_FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"notes": "Test without package_id"}' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

# TEST 4: Valid Request - Change to Package ID 2
echo "TEST 4: Valid Request - Change to Package ID 2"
echo "-----------------------------------------------"
echo "NOTE: Ganti package_id sesuai dengan package yang BERBEDA dari current package Anda"
echo ""
curl -X POST "$EDGE_FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"package_id": 2, "notes": "Test dari curl - upgrade paket"}' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

# TEST 5: Same Package (Should FAIL if package_id = current)
echo "TEST 5: Try Same Package - Package ID 1 (Might FAIL if this is current package)"
echo "---------------------------------------------------------------------------------"
curl -X POST "$EDGE_FUNCTION_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"package_id": 1, "notes": "Test same package"}' \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

# TEST 6: CORS Preflight
echo "TEST 6: CORS Preflight (OPTIONS)"
echo "---------------------------------"
curl -X OPTIONS "$EDGE_FUNCTION_URL" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: authorization, content-type" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s
echo ""
echo ""

echo "=========================================="
echo "TESTING COMPLETE!"
echo "=========================================="
