#!/bin/bash

# Document Service 验证脚本

echo "=========================================="
echo "Document Service 验证脚本"
echo "=========================================="
echo ""

BASE_URL="http://localhost:8081"

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查服务健康状态
echo "1. 检查服务健康状态..."
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/actuator/health)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ 服务健康检查通过${NC}"
    echo "响应: $body"
else
    echo -e "${RED}✗ 服务健康检查失败 (HTTP $http_code)${NC}"
    exit 1
fi
echo ""

# 检查 Prometheus 指标
echo "2. 检查 Prometheus 指标..."
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/actuator/prometheus)
http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Prometheus 指标端点正常${NC}"
else
    echo -e "${RED}✗ Prometheus 指标端点异常 (HTTP $http_code)${NC}"
fi
echo ""

# 测试文档列表查询
echo "3. 测试文档列表查询..."
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/v1/documents?page=1&pageSize=10)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ 文档列表查询成功${NC}"
    echo "响应: $body"
else
    echo -e "${RED}✗ 文档列表查询失败 (HTTP $http_code)${NC}"
    echo "响应: $body"
fi
echo ""

# 测试文档上传（需要准备测试文件）
echo "4. 测试文档上传..."
if [ -f "test.txt" ]; then
    response=$(curl -s -w "\n%{http_code}" -X POST ${BASE_URL}/api/v1/documents/upload \
        -F "file=@test.txt")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "200" ]; then
        echo -e "${GREEN}✓ 文档上传成功${NC}"
        echo "响应: $body"
        
        # 提取 documentId
        document_id=$(echo "$body" | grep -o '"documentId":"[^"]*"' | cut -d'"' -f4)
        
        if [ -n "$document_id" ]; then
            echo ""
            echo "5. 测试文档详情查询..."
            response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/v1/documents/${document_id})
            http_code=$(echo "$response" | tail -n1)
            body=$(echo "$response" | sed '$d')
            
            if [ "$http_code" = "200" ]; then
                echo -e "${GREEN}✓ 文档详情查询成功${NC}"
                echo "响应: $body"
            else
                echo -e "${RED}✗ 文档详情查询失败 (HTTP $http_code)${NC}"
            fi
            
            echo ""
            echo "6. 测试文档删除..."
            response=$(curl -s -w "\n%{http_code}" -X DELETE ${BASE_URL}/api/v1/documents/${document_id})
            http_code=$(echo "$response" | tail -n1)
            
            if [ "$http_code" = "204" ]; then
                echo -e "${GREEN}✓ 文档删除成功${NC}"
            else
                echo -e "${RED}✗ 文档删除失败 (HTTP $http_code)${NC}"
            fi
        fi
    else
        echo -e "${RED}✗ 文档上传失败 (HTTP $http_code)${NC}"
        echo "响应: $body"
    fi
else
    echo -e "${YELLOW}⚠ 跳过文档上传测试（未找到 test.txt 文件）${NC}"
    echo "提示: 创建一个 test.txt 文件来测试文档上传功能"
fi
echo ""

echo "=========================================="
echo "验证完成"
echo "=========================================="
