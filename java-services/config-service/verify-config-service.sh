#!/bin/bash

# Config Service 验证脚本

echo "=========================================="
echo "Config Service 验证脚本"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8085"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数器
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 测试函数
test_endpoint() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "测试 $TOTAL_TESTS: $test_name ... "
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq "$expected_status" ]; then
        echo -e "${GREEN}✓ 通过${NC} (HTTP $http_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        if [ ! -z "$body" ]; then
            echo "   响应: $(echo $body | jq -c '.' 2>/dev/null || echo $body)"
        fi
    else
        echo -e "${RED}✗ 失败${NC} (期望 HTTP $expected_status, 实际 HTTP $http_code)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        if [ ! -z "$body" ]; then
            echo "   响应: $body"
        fi
    fi
    echo ""
}

# 等待服务启动
echo "等待 Config Service 启动..."
for i in {1..30}; do
    if curl -s "$BASE_URL/api/health" > /dev/null 2>&1; then
        echo -e "${GREEN}Config Service 已启动${NC}"
        echo ""
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}Config Service 启动超时${NC}"
        exit 1
    fi
    sleep 2
done

# 1. 健康检查
test_endpoint "健康检查" "GET" "/api/health" "" 200

# 2. 获取所有配置
test_endpoint "获取所有配置" "GET" "/api/v1/config" "" 200

# 3. 获取所有配置详情
test_endpoint "获取所有配置详情" "GET" "/api/v1/config/details" "" 200

# 4. 获取特定配置
test_endpoint "获取文档分块大小配置" "GET" "/api/v1/config/document.chunk_size" "" 200

# 5. 创建新配置
test_endpoint "创建新配置" "POST" "/api/v1/config" \
    '{"configKey":"test.config","configValue":"test_value","configType":"STRING","description":"测试配置","isActive":true}' \
    200

# 6. 更新配置
test_endpoint "更新配置" "PUT" "/api/v1/config/test.config" \
    '{"configValue":"updated_value","description":"更新后的测试配置"}' \
    200

# 7. 批量更新配置
test_endpoint "批量更新配置" "PUT" "/api/v1/config" \
    '{"document.chunk_size":"1024","retrieval.top_k":"10"}' \
    200

# 8. 测试LLM连接（预期失败，因为没有真实的LLM服务）
test_endpoint "测试LLM连接" "POST" "/api/v1/config/test-llm" \
    '{"provider":"openai","apiKey":"test-key","endpoint":"http://localhost:9004","model":"gpt-4"}' \
    200

# 9. 获取不存在的配置（应该返回错误）
test_endpoint "获取不存在的配置" "GET" "/api/v1/config/nonexistent.config" "" 500

# 10. 创建重复配置（应该返回错误）
test_endpoint "创建重复配置" "POST" "/api/v1/config" \
    '{"configKey":"test.config","configValue":"duplicate","configType":"STRING"}' \
    500

# 输出测试结果摘要
echo "=========================================="
echo "测试结果摘要"
echo "=========================================="
echo -e "总测试数: $TOTAL_TESTS"
echo -e "${GREEN}通过: $PASSED_TESTS${NC}"
echo -e "${RED}失败: $FAILED_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}所有测试通过！✓${NC}"
    exit 0
else
    echo -e "${RED}部分测试失败！✗${NC}"
    exit 1
fi
