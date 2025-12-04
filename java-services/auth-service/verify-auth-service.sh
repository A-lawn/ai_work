#!/bin/bash

# Auth Service 验证脚本

echo "========================================="
echo "Auth Service 验证脚本"
echo "========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 服务地址
AUTH_SERVICE_URL="http://localhost:8083"

# 测试计数器
PASSED=0
FAILED=0

# 测试函数
test_endpoint() {
    local name=$1
    local method=$2
    local url=$3
    local data=$4
    local expected_status=$5
    local headers=$6
    
    echo -n "测试: $name ... "
    
    if [ -z "$data" ]; then
        if [ -z "$headers" ]; then
            response=$(curl -s -w "\n%{http_code}" -X $method "$url" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X $method "$url" -H "$headers" 2>/dev/null)
        fi
    else
        if [ -z "$headers" ]; then
            response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
                -H "Content-Type: application/json" \
                -d "$data" 2>/dev/null)
        else
            response=$(curl -s -w "\n%{http_code}" -X $method "$url" \
                -H "Content-Type: application/json" \
                -H "$headers" \
                -d "$data" 2>/dev/null)
        fi
    fi
    
    status_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $status_code)"
        PASSED=$((PASSED + 1))
        if [ ! -z "$body" ] && [ "$body" != "null" ]; then
            echo "  响应: $(echo $body | head -c 100)..."
        fi
    else
        echo -e "${RED}✗ FAILED${NC} (Expected: $expected_status, Got: $status_code)"
        FAILED=$((FAILED + 1))
        if [ ! -z "$body" ]; then
            echo "  响应: $body"
        fi
    fi
    echo ""
}

# 等待服务启动
echo "等待 Auth Service 启动..."
for i in {1..30}; do
    if curl -s "$AUTH_SERVICE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}Auth Service 已启动${NC}"
        echo ""
        break
    fi
    if [ $i -eq 30 ]; then
        echo -e "${RED}Auth Service 启动超时${NC}"
        exit 1
    fi
    sleep 2
done

# 1. 健康检查
echo "========================================="
echo "1. 健康检查测试"
echo "========================================="
test_endpoint "健康检查" "GET" "$AUTH_SERVICE_URL/api/health" "" "200"
test_endpoint "Actuator 健康检查" "GET" "$AUTH_SERVICE_URL/actuator/health" "" "200"

# 2. 用户登录测试
echo "========================================="
echo "2. 用户认证测试"
echo "========================================="

# 登录（使用默认管理员账户）
LOGIN_DATA='{"username":"admin","password":"admin123"}'
login_response=$(curl -s -X POST "$AUTH_SERVICE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "$LOGIN_DATA")

if echo "$login_response" | grep -q "accessToken"; then
    echo -e "${GREEN}✓ 登录成功${NC}"
    PASSED=$((PASSED + 1))
    
    # 提取 Token
    ACCESS_TOKEN=$(echo $login_response | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
    REFRESH_TOKEN=$(echo $login_response | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)
    
    echo "  Access Token: ${ACCESS_TOKEN:0:50}..."
    echo "  Refresh Token: ${REFRESH_TOKEN:0:50}..."
else
    echo -e "${RED}✗ 登录失败${NC}"
    echo "  响应: $login_response"
    FAILED=$((FAILED + 1))
    ACCESS_TOKEN=""
    REFRESH_TOKEN=""
fi
echo ""

# 登录失败测试
test_endpoint "登录失败（错误密码）" "POST" "$AUTH_SERVICE_URL/api/auth/login" \
    '{"username":"admin","password":"wrongpassword"}' "400"

# 3. Token 验证测试
echo "========================================="
echo "3. Token 验证测试"
echo "========================================="

if [ ! -z "$ACCESS_TOKEN" ]; then
    test_endpoint "验证有效 Token" "POST" "$AUTH_SERVICE_URL/api/auth/validate-token" \
        "" "200" "Authorization: Bearer $ACCESS_TOKEN"
    
    test_endpoint "验证无效 Token" "POST" "$AUTH_SERVICE_URL/api/auth/validate-token" \
        "" "200" "Authorization: Bearer invalid_token"
else
    echo -e "${YELLOW}跳过 Token 验证测试（未获取到 Token）${NC}"
    echo ""
fi

# 4. Token 刷新测试
echo "========================================="
echo "4. Token 刷新测试"
echo "========================================="

if [ ! -z "$REFRESH_TOKEN" ]; then
    test_endpoint "刷新 Token" "POST" "$AUTH_SERVICE_URL/api/auth/refresh" \
        "{\"refreshToken\":\"$REFRESH_TOKEN\"}" "200"
else
    echo -e "${YELLOW}跳过 Token 刷新测试（未获取到 Refresh Token）${NC}"
    echo ""
fi

# 5. API Key 管理测试
echo "========================================="
echo "5. API Key 管理测试"
echo "========================================="

if [ ! -z "$ACCESS_TOKEN" ]; then
    # 创建 API Key
    echo -n "测试: 创建 API Key ... "
    apikey_response=$(curl -s -X POST "$AUTH_SERVICE_URL/api/api-keys" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d '{"name":"Test API Key","description":"用于测试","expirationDays":365}')
    
    if echo "$apikey_response" | grep -q "apiKey"; then
        echo -e "${GREEN}✓ PASSED${NC}"
        PASSED=$((PASSED + 1))
        
        # 提取 API Key
        API_KEY=$(echo $apikey_response | grep -o '"apiKey":"[^"]*' | cut -d'"' -f4)
        API_KEY_ID=$(echo $apikey_response | grep -o '"id":"[^"]*' | cut -d'"' -f4)
        
        echo "  API Key: ${API_KEY:0:30}..."
        echo "  API Key ID: $API_KEY_ID"
    else
        echo -e "${RED}✗ FAILED${NC}"
        echo "  响应: $apikey_response"
        FAILED=$((FAILED + 1))
        API_KEY=""
        API_KEY_ID=""
    fi
    echo ""
    
    # 验证 API Key
    if [ ! -z "$API_KEY" ]; then
        test_endpoint "验证有效 API Key" "POST" "$AUTH_SERVICE_URL/api/auth/validate-api-key" \
            "" "200" "X-API-Key: $API_KEY"
        
        test_endpoint "验证无效 API Key" "POST" "$AUTH_SERVICE_URL/api/auth/validate-api-key" \
            "" "200" "X-API-Key: invalid_key"
    fi
    
    # 获取 API Keys 列表
    test_endpoint "获取 API Keys 列表" "GET" "$AUTH_SERVICE_URL/api/api-keys" \
        "" "200" "Authorization: Bearer $ACCESS_TOKEN"
    
    # 撤销 API Key
    if [ ! -z "$API_KEY_ID" ]; then
        test_endpoint "撤销 API Key" "DELETE" "$AUTH_SERVICE_URL/api/api-keys/$API_KEY_ID" \
            "" "204" "Authorization: Bearer $ACCESS_TOKEN"
    fi
else
    echo -e "${YELLOW}跳过 API Key 管理测试（未获取到 Access Token）${NC}"
    echo ""
fi

# 6. Prometheus 指标测试
echo "========================================="
echo "6. 监控指标测试"
echo "========================================="
test_endpoint "Prometheus 指标" "GET" "$AUTH_SERVICE_URL/actuator/prometheus" "" "200"

# 总结
echo "========================================="
echo "测试总结"
echo "========================================="
echo -e "通过: ${GREEN}$PASSED${NC}"
echo -e "失败: ${RED}$FAILED${NC}"
echo "总计: $((PASSED + FAILED))"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}所有测试通过！✓${NC}"
    exit 0
else
    echo -e "${RED}部分测试失败！✗${NC}"
    exit 1
fi
