#!/bin/bash

# RAG Query Service 验证脚本

echo "=========================================="
echo "RAG Query Service 验证脚本"
echo "=========================================="
echo ""

# 服务地址
SERVICE_URL="http://localhost:9002"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "测试 $TOTAL_TESTS: $name ... "
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$SERVICE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$SERVICE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ 通过${NC} (状态码: $status_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        if [ ! -z "$body" ]; then
            echo "   响应: $(echo $body | head -c 100)..."
        fi
    else
        echo -e "${RED}✗ 失败${NC} (期望: $expected_status, 实际: $status_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        if [ ! -z "$body" ]; then
            echo "   响应: $body"
        fi
    fi
    echo ""
}

# 1. 测试根路径
echo "1. 基础接口测试"
echo "----------------------------------------"
test_endpoint "根路径" "GET" "/" "" 200

# 2. 测试健康检查
test_endpoint "健康检查" "GET" "/api/health" "" 200

# 3. 测试 Prometheus 指标
test_endpoint "Prometheus 指标" "GET" "/metrics" "" 200

# 4. 测试 API 文档
test_endpoint "API 文档" "GET" "/docs" "" 200

echo ""
echo "2. RAG 查询接口测试"
echo "----------------------------------------"

# 5. 测试查询接口（同步模式）
query_data='{
  "question": "如何重启 Nginx 服务？",
  "top_k": 5,
  "similarity_threshold": 0.7
}'
test_endpoint "RAG 查询（同步）" "POST" "/api/query" "$query_data" 200

# 6. 测试查询接口（流式模式）
stream_query_data='{
  "question": "如何重启 Nginx 服务？",
  "stream": true
}'
test_endpoint "RAG 查询（流式）" "POST" "/api/query/stream" "$stream_query_data" 200

# 7. 测试带会话历史的查询
history_query_data='{
  "question": "具体步骤是什么？",
  "session_history": [
    {"role": "user", "content": "如何重启 Nginx？"},
    {"role": "assistant", "content": "可以使用 systemctl restart nginx 命令"}
  ],
  "top_k": 3
}'
test_endpoint "带会话历史的查询" "POST" "/api/query" "$history_query_data" 200

echo ""
echo "3. 错误处理测试"
echo "----------------------------------------"

# 8. 测试空问题
empty_query_data='{
  "question": ""
}'
test_endpoint "空问题" "POST" "/api/query" "$empty_query_data" 422

# 9. 测试无效的 top_k
invalid_topk_data='{
  "question": "测试问题",
  "top_k": -1
}'
test_endpoint "无效的 top_k" "POST" "/api/query" "$invalid_topk_data" 422

echo ""
echo "=========================================="
echo "测试总结"
echo "=========================================="
echo "总测试数: $TOTAL_TESTS"
echo -e "通过: ${GREEN}$PASSED_TESTS${NC}"
echo -e "失败: ${RED}$FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}所有测试通过！✓${NC}"
    exit 0
else
    echo -e "${RED}部分测试失败！✗${NC}"
    exit 1
fi
