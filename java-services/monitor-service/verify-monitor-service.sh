#!/bin/bash

# Monitor Service Verification Script
# 验证 Monitor Service 的功能

set -e

BASE_URL="http://localhost:8084"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Monitor Service Verification"
echo "=========================================="
echo ""

# Function to print colored output
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# 1. Health Check
echo "1. Testing Health Check..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/health)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Health check passed"
    echo "$RESPONSE_BODY" | jq '.'
else
    print_error "Health check failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

# 2. Collect Log
echo "2. Testing Log Collection..."
LOG_DATA='{
  "operationType": "TEST_OPERATION",
  "serviceName": "test-service",
  "userId": "test-user",
  "resourceId": "test-resource-123",
  "resourceType": "test",
  "action": "create",
  "status": "SUCCESS",
  "ipAddress": "127.0.0.1",
  "userAgent": "curl/test",
  "durationMs": 100,
  "traceId": "test-trace-123",
  "spanId": "test-span-456",
  "details": {
    "testKey": "testValue"
  }
}'

LOG_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/v1/logs \
  -H "Content-Type: application/json" \
  -d "$LOG_DATA")
HTTP_CODE=$(echo "$LOG_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$LOG_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Log collection successful"
    echo "$RESPONSE_BODY" | jq '.'
else
    print_error "Log collection failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

# Wait for async processing
sleep 2

# 3. Query Logs
echo "3. Testing Log Query..."
QUERY_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/logs?operationType=TEST_OPERATION&page=0&size=10")
HTTP_CODE=$(echo "$QUERY_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$QUERY_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Log query successful"
    echo "$RESPONSE_BODY" | jq '.content | length' | xargs -I {} echo "Found {} logs"
else
    print_error "Log query failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

# 4. Collect Metric
echo "4. Testing Metric Collection..."
METRIC_DATA='{
  "metricType": "response_time",
  "serviceName": "test-service",
  "metricName": "api_response_time",
  "metricValue": 150.5,
  "unit": "ms",
  "tags": "endpoint=/api/test",
  "metadata": {
    "method": "GET",
    "statusCode": 200
  }
}'

METRIC_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/v1/metrics \
  -H "Content-Type: application/json" \
  -d "$METRIC_DATA")
HTTP_CODE=$(echo "$METRIC_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$METRIC_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Metric collection successful"
    echo "$RESPONSE_BODY" | jq '.'
else
    print_error "Metric collection failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

# Wait for async processing
sleep 2

# 5. Query Metrics
echo "5. Testing Metric Query..."
METRIC_QUERY_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/metrics?metricType=response_time&page=0&size=10")
HTTP_CODE=$(echo "$METRIC_QUERY_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$METRIC_QUERY_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Metric query successful"
    echo "$RESPONSE_BODY" | jq '.content | length' | xargs -I {} echo "Found {} metrics"
else
    print_error "Metric query failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

# 6. Get System Stats
echo "6. Testing System Stats..."
STATS_RESPONSE=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/v1/stats)
HTTP_CODE=$(echo "$STATS_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$STATS_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "System stats retrieval successful"
    echo "$RESPONSE_BODY" | jq '.'
else
    print_error "System stats retrieval failed (HTTP $HTTP_CODE)"
    echo "$RESPONSE_BODY"
fi
echo ""

# 7. Test Prometheus Endpoint
echo "7. Testing Prometheus Endpoint..."
PROMETHEUS_RESPONSE=$(curl -s -w "\n%{http_code}" ${BASE_URL}/actuator/prometheus)
HTTP_CODE=$(echo "$PROMETHEUS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Prometheus endpoint accessible"
    echo "Sample metrics:"
    echo "$PROMETHEUS_RESPONSE" | sed '$d' | grep "jvm_memory_used_bytes" | head -n 3
else
    print_error "Prometheus endpoint failed (HTTP $HTTP_CODE)"
fi
echo ""

# 8. Test Search (if Elasticsearch is available)
echo "8. Testing Log Search..."
SEARCH_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/logs/search?keyword=TEST&page=0&size=10")
HTTP_CODE=$(echo "$SEARCH_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$SEARCH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Log search successful"
    echo "$RESPONSE_BODY" | jq '.content | length' | xargs -I {} echo "Found {} logs"
elif [ "$HTTP_CODE" -eq 500 ]; then
    print_info "Log search failed - Elasticsearch may not be configured"
else
    print_error "Log search failed (HTTP $HTTP_CODE)"
fi
echo ""

# 9. Test Trace ID Query
echo "9. Testing Trace ID Query..."
TRACE_RESPONSE=$(curl -s -w "\n%{http_code}" "${BASE_URL}/api/v1/logs/trace/test-trace-123")
HTTP_CODE=$(echo "$TRACE_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$TRACE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    print_success "Trace ID query successful"
    echo "$RESPONSE_BODY" | jq 'length' | xargs -I {} echo "Found {} logs with trace ID"
elif [ "$HTTP_CODE" -eq 500 ]; then
    print_info "Trace ID query failed - Elasticsearch may not be configured"
else
    print_error "Trace ID query failed (HTTP $HTTP_CODE)"
fi
echo ""

echo "=========================================="
echo "Verification Complete!"
echo "=========================================="
echo ""
print_info "Note: Some tests may fail if Elasticsearch is not configured."
print_info "The service will still function with PostgreSQL for basic operations."
