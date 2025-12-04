#!/bin/bash

# Embedding Service 验证脚本

echo "=========================================="
echo "Embedding Service 验证脚本"
echo "=========================================="
echo ""

SERVICE_URL="http://localhost:9003"
TIMEOUT=5

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查服务是否运行
echo "1. 检查服务健康状态..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" --max-time $TIMEOUT "${SERVICE_URL}/health" 2>/dev/null)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
HEALTH_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓${NC} 服务健康检查通过"
    echo "响应: $HEALTH_BODY" | head -c 200
    echo ""
else
    echo -e "${RED}✗${NC} 服务健康检查失败 (HTTP $HTTP_CODE)"
    echo "请确保服务已启动: uvicorn app.main:app --port 9003"
    exit 1
fi

echo ""

# 测试嵌入向量生成
echo "2. 测试嵌入向量生成..."
EMBEDDING_RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 30 \
    -X POST "${SERVICE_URL}/api/embeddings" \
    -H "Content-Type: application/json" \
    -d '{
        "texts": ["如何重启 Nginx 服务？", "MySQL 数据库连接失败"],
        "use_cache": true
    }' 2>/dev/null)

HTTP_CODE=$(echo "$EMBEDDING_RESPONSE" | tail -n1)
EMBEDDING_BODY=$(echo "$EMBEDDING_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓${NC} 嵌入向量生成成功"
    
    # 提取关键信息
    MODEL=$(echo "$EMBEDDING_BODY" | grep -o '"model":"[^"]*"' | cut -d'"' -f4)
    DIMENSION=$(echo "$EMBEDDING_BODY" | grep -o '"dimension":[0-9]*' | cut -d':' -f2)
    TOTAL=$(echo "$EMBEDDING_BODY" | grep -o '"total_count":[0-9]*' | cut -d':' -f2)
    CACHED=$(echo "$EMBEDDING_BODY" | grep -o '"cached_count":[0-9]*' | cut -d':' -f2)
    
    echo "  - 模型: $MODEL"
    echo "  - 维度: $DIMENSION"
    echo "  - 总数: $TOTAL"
    echo "  - 缓存命中: $CACHED"
else
    echo -e "${RED}✗${NC} 嵌入向量生成失败 (HTTP $HTTP_CODE)"
    echo "响应: $EMBEDDING_BODY"
fi

echo ""

# 测试缓存功能
echo "3. 测试缓存功能（再次请求相同文本）..."
CACHE_RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 30 \
    -X POST "${SERVICE_URL}/api/embeddings" \
    -H "Content-Type: application/json" \
    -d '{
        "texts": ["如何重启 Nginx 服务？"],
        "use_cache": true
    }' 2>/dev/null)

HTTP_CODE=$(echo "$CACHE_RESPONSE" | tail -n1)
CACHE_BODY=$(echo "$CACHE_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    CACHED=$(echo "$CACHE_BODY" | grep -o '"cached_count":[0-9]*' | cut -d':' -f2)
    
    if [ "$CACHED" = "1" ]; then
        echo -e "${GREEN}✓${NC} 缓存功能正常（缓存命中: $CACHED）"
    else
        echo -e "${YELLOW}⚠${NC} 缓存未命中（可能 Redis 未连接）"
    fi
else
    echo -e "${RED}✗${NC} 缓存测试失败"
fi

echo ""

# 检查 Prometheus 指标
echo "4. 检查 Prometheus 指标..."
METRICS_RESPONSE=$(curl -s -w "\n%{http_code}" --max-time $TIMEOUT "${SERVICE_URL}/metrics" 2>/dev/null)
HTTP_CODE=$(echo "$METRICS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Prometheus 指标端点正常"
    
    # 检查关键指标
    METRICS_BODY=$(echo "$METRICS_RESPONSE" | sed '$d')
    
    if echo "$METRICS_BODY" | grep -q "embedding_requests_total"; then
        echo "  - embedding_requests_total: 已暴露"
    fi
    
    if echo "$METRICS_BODY" | grep -q "embedding_duration_seconds"; then
        echo "  - embedding_duration_seconds: 已暴露"
    fi
    
    if echo "$METRICS_BODY" | grep -q "redis_connection_status"; then
        REDIS_STATUS=$(echo "$METRICS_BODY" | grep "redis_connection_status" | grep -v "#" | awk '{print $2}')
        if [ "$REDIS_STATUS" = "1.0" ]; then
            echo "  - Redis 连接: 已连接"
        else
            echo "  - Redis 连接: 未连接"
        fi
    fi
else
    echo -e "${RED}✗${NC} Prometheus 指标端点失败"
fi

echo ""

# 检查 API 文档
echo "5. 检查 API 文档..."
DOCS_RESPONSE=$(curl -s -w "\n%{http_code}" --max-time $TIMEOUT "${SERVICE_URL}/docs" 2>/dev/null)
HTTP_CODE=$(echo "$DOCS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓${NC} API 文档可访问"
    echo "  访问地址: ${SERVICE_URL}/docs"
else
    echo -e "${RED}✗${NC} API 文档不可访问"
fi

echo ""
echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "服务端点:"
echo "  - 健康检查: ${SERVICE_URL}/health"
echo "  - 嵌入向量: ${SERVICE_URL}/api/embeddings"
echo "  - 指标监控: ${SERVICE_URL}/metrics"
echo "  - API 文档: ${SERVICE_URL}/docs"
echo ""
