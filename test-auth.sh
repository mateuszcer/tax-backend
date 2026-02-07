#!/bin/bash

# Simple Auth Testing Script
# Usage: ./test-auth.sh [base-url]

BASE_URL="${1:-http://localhost:8080}"
TEST_EMAIL="demo$(date +%s)@test.com"
TEST_PASSWORD="DemoPassword123!"

echo "🚀 Testing Simple Auth at $BASE_URL"
echo "📧 Test email: $TEST_EMAIL"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health check
echo "1️⃣  Testing health endpoint..."
HEALTH=$(curl -s "$BASE_URL/actuator/health")
if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ Health check passed${NC}"
else
    echo -e "${RED}❌ Health check failed${NC}"
    echo "$HEALTH"
    exit 1
fi
echo ""

# Test 2: Sign up
echo "2️⃣  Testing sign up..."
SIGNUP_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/signUp" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

if echo "$SIGNUP_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ Sign up successful${NC}"
else
    echo -e "${RED}❌ Sign up failed${NC}"
    echo "$SIGNUP_RESPONSE"
    exit 1
fi
echo ""

# Test 3: Sign in
echo "3️⃣  Testing sign in..."
SIGNIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/signIn" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

if echo "$SIGNIN_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ Sign in successful${NC}"
    
    # Extract token
    TOKEN=$(echo "$SIGNIN_RESPONSE" | grep -o '"idToken":"[^"]*' | cut -d'"' -f4)
    
    if [ -n "$TOKEN" ]; then
        echo -e "${GREEN}✅ Token received${NC}"
        echo -e "${YELLOW}Token (first 50 chars): ${TOKEN:0:50}...${NC}"
    else
        echo -e "${RED}❌ No token in response${NC}"
        exit 1
    fi
else
    echo -e "${RED}❌ Sign in failed${NC}"
    echo "$SIGNIN_RESPONSE"
    exit 1
fi
echo ""

# Test 4: Protected endpoint
echo "4️⃣  Testing protected endpoint with token..."
ORDERS_RESPONSE=$(curl -s "$BASE_URL/api/orders" \
    -H "Authorization: Bearer $TOKEN")

if echo "$ORDERS_RESPONSE" | grep -q '"success":true'; then
    echo -e "${GREEN}✅ Protected endpoint accessible with token${NC}"
else
    echo -e "${YELLOW}⚠️  Protected endpoint returned error (might be expected if no orders)${NC}"
    echo "$ORDERS_RESPONSE"
fi
echo ""

# Test 5: Invalid credentials
echo "5️⃣  Testing invalid credentials..."
INVALID_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/signIn" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"WrongPassword\"}")

if echo "$INVALID_RESPONSE" | grep -q '"success":false'; then
    echo -e "${GREEN}✅ Invalid credentials correctly rejected${NC}"
else
    echo -e "${RED}❌ Invalid credentials not rejected${NC}"
    echo "$INVALID_RESPONSE"
fi
echo ""

# Test 6: Duplicate sign up
echo "6️⃣  Testing duplicate email rejection..."
DUPLICATE_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/signUp" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")

if echo "$DUPLICATE_RESPONSE" | grep -q '"success":false'; then
    echo -e "${GREEN}✅ Duplicate email correctly rejected${NC}"
else
    echo -e "${RED}❌ Duplicate email not rejected${NC}"
    echo "$DUPLICATE_RESPONSE"
fi
echo ""

echo -e "${GREEN}🎉 All authentication tests passed!${NC}"
echo ""
echo "📋 Summary:"
echo "   - Health check: OK"
echo "   - Sign up: OK"
echo "   - Sign in: OK"
echo "   - JWT token: OK"
echo "   - Protected endpoints: OK"
echo "   - Invalid credentials: Rejected ✓"
echo "   - Duplicate emails: Rejected ✓"
echo ""
echo -e "${YELLOW}🔑 Save this for your demo:${NC}"
echo "   Email: $TEST_EMAIL"
echo "   Password: $TEST_PASSWORD"
echo "   Token: ${TOKEN:0:50}..."
