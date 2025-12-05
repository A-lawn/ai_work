#!/bin/bash

# Quick Verification Script - Tests Critical Paths Only
# Use this for rapid health checks

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

GATEWAY_URL="http://localhost:8080"
NACOS_URL="http://localhost:8848"

echo -e "${YELLOW}RAG Ops QA Assistant - Quick Verification${NC}\n"

# 1. Check Gateway
echo -n "1. Gateway health... "
if curl -s -f "$GATEWAY_URL/actuator/health" | grep -q "UP"; then
    echo -e "${GREEN}‚ú?{NC}"
else
    echo -e "${RED}‚ú?{NC}"
    exit 1
fi

# 2. Check Nacos
echo -n "2. Nacos health... "
if curl -s -f "$NACOS_URL/nacos/v1/console/health/liveness" > /dev/null 2>&1; then
    echo -e "${GREEN}‚ú?{NC}"
else
    echo -e "${RED}‚ú?{NC}"
    exit 1
fi

# 3. Check Service Registration
echo -n "3. Services registered... "
SERVICES=$(curl -s "$NACOS_URL/nacos/v1/ns/service/list?pageNo=1&pageSize=100" | grep -o "gateway-service\|document-service\|session-service" | wc -l)
if [ "$SERVICES" -ge 3 ]; then
    echo -e "${GREEN}‚ú?($SERVICES services)${NC}"
else
    echo -e "${RED}‚ú?(only $SERVICES services)${NC}"
fi

# 4. Test Document Upload
echo -n "4. Document upload... "
echo "Test document" > /tmp/quick_test.txt
UPLOAD_RESULT=$(curl -s -X POST "$GATEWAY_URL/api/v1/documents/upload" -F "file=@/tmp/quick_test.txt")
if echo "$UPLOAD_RESULT" | grep -q "id\|documentId"; then
    echo -e "${GREEN}‚ú?{NC}"
else
    echo -e "${RED}‚ú?{NC}"
fi
rm -f /tmp/quick_test.txt

# 5. Test Session Creation
echo -n "5. Session creation... "
SESSION_RESULT=$(curl -s -X POST "$GATEWAY_URL/api/v1/sessions" -H "Content-Type: application/json" -d '{"userId":"quick_test"}')
if echo "$SESSION_RESULT" | grep -q "id\|sessionId"; then
    echo -e "${GREEN}‚ú?{NC}"
else
    echo -e "${RED}‚ú?{NC}"
fi

# 6. Test Query
echo -n "6. Query processing... "
SESSION_ID=$(echo "$SESSION_RESULT" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
if [ -n "$SESSION_ID" ]; then
    QUERY_RESULT=$(curl -s -X POST "$GATEWAY_URL/api/v1/query" \
        -H "Content-Type: application/json" \
        -d "{\"question\":\"test\",\"session_id\":\"$SESSION_ID\"}")
    if echo "$QUERY_RESULT" | grep -q "answer"; then
        echo -e "${GREEN}‚ú?{NC}"
    else
        echo -e "${RED}‚ú?{NC}"
    fi
else
    echo -e "${YELLOW}‚ä?(skipped - no session)${NC}"
fi

# 7. Check Monitoring
echo -n "7. Monitoring stack... "
PROM_UP=$(curl -s -f "http://localhost:9090/-/healthy" > /dev/null 2>&1 && echo "1" || echo "0")
GRAFANA_UP=$(curl -s -f "http://localhost:3001/api/health" > /dev/null 2>&1 && echo "1" || echo "0")
if [ "$PROM_UP" = "1" ] && [ "$GRAFANA_UP" = "1" ]; then
    echo -e "${GREEN}‚ú?{NC}"
else
    echo -e "${YELLOW}‚ä?(Prometheus: $PROM_UP, Grafana: $GRAFANA_UP)${NC}"
fi

# 8. Check Tracing
echo -n "8. Distributed tracing... "
if curl -s -f "http://localhost:9411/health" > /dev/null 2>&1; then
    echo -e "${GREEN}‚ú?{NC}"
else
    echo -e "${YELLOW}‚ä?{NC}"
fi

echo -e "\n${GREEN}Quick verification complete!${NC}"
echo -e "For comprehensive testing, run: ${YELLOW}./verify-e2e.sh${NC}\n"
