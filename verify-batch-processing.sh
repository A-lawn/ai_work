#!/bin/bash

# 批量文档处理功能验证脚本

set -e

echo "=========================================="
echo "批量文档处理功能验证"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查函数
check_service() {
    local service_name=$1
    local url=$2
    
    echo -n "检查 ${service_name}... "
    if curl -s -f "${url}" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        return 1
    fi
}

# 1. 检查服务状态
echo "1. 检查服务状态"
echo "-------------------"
check_service "Document Service" "http://localhost:8081/health"
check_service "Document Processing Service" "http://localhost:9001/health"
check_service "RabbitMQ" "http://localhost:15672"
check_service "Redis" "http://localhost:6379" || echo -e "${YELLOW}Redis 需要使用 redis-cli 检查${NC}"
echo ""

# 2. 检查 Docker 容器
echo "2. 检查 Docker 容器"
echo "-------------------"
echo "Celery Worker:"
docker ps | grep celery-worker || echo -e "${RED}Celery Worker 未运行${NC}"
echo ""
echo "Batch Consumer:"
docker ps | grep batch-consumer || echo -e "${RED}Batch Consumer 未运行${NC}"
echo ""

# 3. 创建测试文档
echo "3. 创建测试文档"
echo "-------------------"
TEST_DIR="test-batch-docs"
rm -rf ${TEST_DIR}
mkdir -p ${TEST_DIR}

cat > ${TEST_DIR}/doc1.txt << EOF
测试文档 1
这是一个用于验证批量处理功能的测试文档。
EOF

cat > ${TEST_DIR}/doc2.md << EOF
# 测试文档 2

## 内容
这是 Markdown 格式的测试文档。
EOF

cat > ${TEST_DIR}/doc3.txt << EOF
测试文档 3
包含一些示例内容。
EOF

echo "创建了 3 个测试文档"
ls -lh ${TEST_DIR}/
echo ""

# 4. 创建 ZIP 文件
echo "4. 创建 ZIP 文件"
echo "-------------------"
ZIP_FILE="test-batch-docs.zip"
rm -f ${ZIP_FILE}
cd ${TEST_DIR}
zip ../${ZIP_FILE} *.txt *.md
cd ..
echo "ZIP 文件创建完成: ${ZIP_FILE}"
ls -lh ${ZIP_FILE}
echo ""

# 5. 上传 ZIP 文件
echo "5. 上传 ZIP 文件"
echo "-------------------"
UPLOAD_RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/documents/batch-upload \
  -F "file=@${ZIP_FILE}")

echo "上传响应:"
echo ${UPLOAD_RESPONSE} | jq '.'

# 提取 taskId
TASK_ID=$(echo ${UPLOAD_RESPONSE} | jq -r '.taskId')
if [ "$TASK_ID" = "null" ] || [ -z "$TASK_ID" ]; then
    echo -e "${RED}上传失败，未获取到 taskId${NC}"
    exit 1
fi

echo -e "${GREEN}上传成功，任务 ID: ${TASK_ID}${NC}"
echo ""

# 6. 监控任务进度
echo "6. 监控任务进度"
echo "-------------------"
MAX_ATTEMPTS=30
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    STATUS_RESPONSE=$(curl -s http://localhost:8081/api/v1/documents/batch-upload/${TASK_ID})
    STATUS=$(echo ${STATUS_RESPONSE} | jq -r '.status')
    PROGRESS=$(echo ${STATUS_RESPONSE} | jq -r '.progress')
    PROCESSED=$(echo ${STATUS_RESPONSE} | jq -r '.processed')
    TOTAL=$(echo ${STATUS_RESPONSE} | jq -r '.total')
    
    echo -n "状态: ${STATUS}, 进度: ${PROGRESS}%, 已处理: ${PROCESSED}/${TOTAL}"
    
    # 检查是否完成
    if [ "$STATUS" = "COMPLETED" ]; then
        echo -e " ${GREEN}✓${NC}"
        echo ""
        echo "任务完成！"
        echo ${STATUS_RESPONSE} | jq '.'
        break
    elif [ "$STATUS" = "COMPLETED_WITH_ERRORS" ]; then
        echo -e " ${YELLOW}⚠${NC}"
        echo ""
        echo "任务完成但有错误："
        echo ${STATUS_RESPONSE} | jq '.'
        break
    elif [ "$STATUS" = "FAILED" ]; then
        echo -e " ${RED}✗${NC}"
        echo ""
        echo "任务失败："
        echo ${STATUS_RESPONSE} | jq '.'
        exit 1
    elif [ "$STATUS" = "NOT_FOUND" ]; then
        echo -e " ${RED}✗${NC}"
        echo ""
        echo "任务不存在或已过期"
        exit 1
    else
        echo ""
    fi
    
    ATTEMPT=$((ATTEMPT + 1))
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo -e "${RED}超时：任务未在预期时间内完成${NC}"
    exit 1
fi

echo ""

# 7. 验证文档
echo "7. 验证文档"
echo "-------------------"
DOCUMENT_IDS=$(echo ${UPLOAD_RESPONSE} | jq -r '.documentIds[]')

for DOC_ID in ${DOCUMENT_IDS}; do
    echo -n "检查文档 ${DOC_ID}... "
    DOC_RESPONSE=$(curl -s http://localhost:8081/api/v1/documents/${DOC_ID})
    DOC_STATUS=$(echo ${DOC_RESPONSE} | jq -r '.status')
    
    if [ "$DOC_STATUS" = "COMPLETED" ]; then
        echo -e "${GREEN}✓${NC} (状态: ${DOC_STATUS})"
    else
        echo -e "${YELLOW}⚠${NC} (状态: ${DOC_STATUS})"
    fi
done

echo ""

# 8. 清理测试数据
echo "8. 清理测试数据"
echo "-------------------"
echo -n "是否清理测试数据？(y/n) "
read -r CLEANUP

if [ "$CLEANUP" = "y" ] || [ "$CLEANUP" = "Y" ]; then
    echo "清理测试文件..."
    rm -rf ${TEST_DIR}
    rm -f ${ZIP_FILE}
    
    echo "删除测试文档..."
    for DOC_ID in ${DOCUMENT_IDS}; do
        curl -s -X DELETE http://localhost:8081/api/v1/documents/${DOC_ID} > /dev/null
        echo "已删除文档: ${DOC_ID}"
    done
    
    echo -e "${GREEN}清理完成${NC}"
else
    echo "保留测试数据"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}批量文档处理功能验证完成！${NC}"
echo "=========================================="
