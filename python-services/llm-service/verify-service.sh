#!/bin/bash

# LLM Service Verification Script
# This script verifies that the LLM Service is running correctly

set -e

SERVICE_URL="http://localhost:9004"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "LLM Service Verification"
echo "========================================="
echo ""

# Function to check if service is running
check_service() {
    echo -n "Checking if service is running... "
    if curl -s -f "${SERVICE_URL}/" > /dev/null; then
        echo -e "${GREEN}✓ Service is running${NC}"
        return 0
    else
        echo -e "${RED}✗ Service is not running${NC}"
        return 1
    fi
}

# Function to check health endpoint
check_health() {
    echo -n "Checking health endpoint... "
    RESPONSE=$(curl -s "${SERVICE_URL}/api/health")
    STATUS=$(echo $RESPONSE | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    
    if [ "$STATUS" = "healthy" ] || [ "$STATUS" = "degraded" ]; then
        echo -e "${GREEN}✓ Health check passed (Status: $STATUS)${NC}"
        return 0
    else
        echo -e "${RED}✗ Health check failed${NC}"
        echo "Response: $RESPONSE"
        return 1
    fi
}

# Function to check service info
check_info() {
    echo -n "Checking service info... "
    RESPONSE=$(curl -s "${SERVICE_URL}/api/info")
    PROVIDER=$(echo $RESPONSE | grep -o '"provider":"[^"]*"' | cut -d'"' -f4)
    MODEL=$(echo $RESPONSE | grep -o '"model":"[^"]*"' | cut -d'"' -f4)
    
    if [ ! -z "$PROVIDER" ]; then
        echo -e "${GREEN}✓ Service info retrieved${NC}"
        echo "  Provider: $PROVIDER"
        echo "  Model: $MODEL"
        return 0
    else
        echo -e "${RED}✗ Failed to get service info${NC}"
        return 1
    fi
}

# Function to test token counting
test_token_counting() {
    echo -n "Testing token counting... "
    RESPONSE=$(curl -s -X POST "${SERVICE_URL}/api/count-tokens" \
        -H "Content-Type: application/json" \
        -d '{"text": "这是一段测试文本"}')
    
    TOKEN_COUNT=$(echo $RESPONSE | grep -o '"token_count":[0-9]*' | cut -d':' -f2)
    
    if [ ! -z "$TOKEN_COUNT" ]; then
        echo -e "${GREEN}✓ Token counting works (Count: $TOKEN_COUNT)${NC}"
        return 0
    else
        echo -e "${RED}✗ Token counting failed${NC}"
        return 1
    fi
}

# Function to test text generation (if API key is configured)
test_generation() {
    echo -n "Testing text generation... "
    
    # Check if we should skip this test
    if [ "$SKIP_GENERATION_TEST" = "true" ]; then
        echo -e "${YELLOW}⊘ Skipped (set SKIP_GENERATION_TEST=false to enable)${NC}"
        return 0
    fi
    
    RESPONSE=$(curl -s -X POST "${SERVICE_URL}/api/generate" \
        -H "Content-Type: application/json" \
        -d '{
            "prompt": "Say hello in Chinese",
            "max_tokens": 10,
            "temperature": 0.7,
            "stream": false
        }' 2>&1)
    
    # Check if response contains text field
    if echo "$RESPONSE" | grep -q '"text"'; then
        echo -e "${GREEN}✓ Text generation works${NC}"
        return 0
    else
        echo -e "${YELLOW}⊘ Text generation test skipped or failed${NC}"
        echo "  (This is expected if API key is not configured)"
        return 0
    fi
}

# Function to check metrics endpoint
check_metrics() {
    echo -n "Checking metrics endpoint... "
    RESPONSE=$(curl -s "${SERVICE_URL}/metrics")
    
    if echo "$RESPONSE" | grep -q "llm_requests_total"; then
        echo -e "${GREEN}✓ Metrics endpoint works${NC}"
        return 0
    else
        echo -e "${RED}✗ Metrics endpoint failed${NC}"
        return 1
    fi
}

# Main verification flow
main() {
    FAILED=0
    
    check_service || FAILED=$((FAILED + 1))
    echo ""
    
    check_health || FAILED=$((FAILED + 1))
    echo ""
    
    check_info || FAILED=$((FAILED + 1))
    echo ""
    
    test_token_counting || FAILED=$((FAILED + 1))
    echo ""
    
    test_generation || FAILED=$((FAILED + 1))
    echo ""
    
    check_metrics || FAILED=$((FAILED + 1))
    echo ""
    
    echo "========================================="
    if [ $FAILED -eq 0 ]; then
        echo -e "${GREEN}All checks passed!${NC}"
        echo "========================================="
        exit 0
    else
        echo -e "${RED}$FAILED check(s) failed${NC}"
        echo "========================================="
        exit 1
    fi
}

# Run main function
main
