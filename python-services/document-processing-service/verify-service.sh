#!/bin/bash

# Document Processing Service 验证脚本

echo "=========================================="
echo "Document Processing Service 验证"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 服务 URL
SERVICE_URL="http://localhost:9001"

# 1. 健康检查
echo "1. 检查服务健康状态..."
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" ${SERVICE_URL}/health)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ 服务健康检查通过${NC}"
    echo "响应: $RESPONSE_BODY"
else
    echo -e "${RED}✗ 服务健康检查失败 (HTTP $HTTP_CODE)${NC}"
    exit 1
fi
echo ""

# 2. 检查根路径
echo "2. 检查根路径..."
ROOT_RESPONSE=$(curl -s -w "\n%{http_code}" ${SERVICE_URL}/)
HTTP_CODE=$(echo "$ROOT_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$ROOT_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ 根路径访问成功${NC}"
    echo "响应: $RESPONSE_BODY"
else
    echo -e "${RED}✗ 根路径访问失败 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# 3. 检查 API 文档
echo "3. 检查 API 文档..."
DOCS_RESPONSE=$(curl -s -w "\n%{http_code}" ${SERVICE_URL}/docs)
HTTP_CODE=$(echo "$DOCS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ API 文档可访问${NC}"
    echo "访问地址: ${SERVICE_URL}/docs"
else
    echo -e "${RED}✗ API 文档访问失败 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# 4. 检查 Prometheus 指标
echo "4. 检查 Prometheus 指标..."
METRICS_RESPONSE=$(curl -s -w "\n%{http_code}" ${SERVICE_URL}/metrics)
HTTP_CODE=$(echo "$METRICS_RESPONSE" | tail -n1)

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Prometheus 指标可访问${NC}"
    echo "访问地址: ${SERVICE_URL}/metrics"
else
    echo -e "${RED}✗ Prometheus 指标访问失败 (HTTP $HTTP_CODE)${NC}"
fi
echo ""

# 5. 测试文档处理 API（需要准备测试文件）
echo "5. 测试文档处理 API..."
echo -e "${YELLOW}注意: 需要准备测试文件和配置才能完整测试${NC}"
echo "示例请求:"
echo 'curl -X POST http://localhost:9001/api/process-document \'
echo '  -H "Content-Type: application/json" \'
echo '  -d "{\"document_id\":\"test-doc\",\"file_path\":\"/path/to/test.pdf\",\"file_type\":\"pdf\"}"'
echo ""

# 6. 检查支持的文档格式
echo "6. 支持的文档格式:"
echo "  - PDF (.pdf)"
echo "  - Word (.docx, .doc)"
echo "  - 文本 (.txt, .log, .csv)"
echo "  - Markdown (.md, .markdown)"
echo ""

echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "服务端点:"
echo "  - 健康检查: ${SERVICE_URL}/health"
echo "  - API 文档: ${SERVICE_URL}/docs"
echo "  - Prometheus: ${SERVICE_URL}/metrics"
echo "  - 处理文档: POST ${SERVICE_URL}/api/process-document"
echo "  - 删除向量: DELETE ${SERVICE_URL}/api/vectors/{document_id}"
echo ""
