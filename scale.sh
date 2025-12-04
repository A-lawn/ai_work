#!/bin/bash

# 智能运维问答助手 - 服务扩缩容脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    echo "使用方法: $0 <服务名> <副本数>"
    echo ""
    echo "可扩展的服务:"
    echo "  document-service             文档管理服务（默认: 2）"
    echo "  session-service              会话管理服务（默认: 2）"
    echo "  document-processing-service  文档处理服务（默认: 2）"
    echo "  rag-query-service            RAG 查询服务（默认: 3）"
    echo "  embedding-service            嵌入模型服务（默认: 2）"
    echo "  llm-service                  大模型服务（默认: 2）"
    echo "  celery-worker                Celery 异步任务（默认: 2）"
    echo ""
    echo "示例:"
    echo "  $0 rag-query-service 5       # 扩展 RAG 查询服务到 5 个副本"
    echo "  $0 document-service 3        # 扩展文档服务到 3 个副本"
    echo "  $0 embedding-service 4       # 扩展嵌入服务到 4 个副本"
    echo ""
    echo "查看当前状态:"
    echo "  docker-compose ps"
}

# 检查参数
if [ $# -ne 2 ]; then
    print_error "参数错误"
    echo ""
    show_help
    exit 1
fi

SERVICE=$1
REPLICAS=$2

# 验证副本数
if ! [[ "$REPLICAS" =~ ^[0-9]+$ ]] || [ "$REPLICAS" -lt 1 ]; then
    print_error "副本数必须是大于 0 的整数"
    exit 1
fi

# 可扩展的服务列表
SCALABLE_SERVICES=(
    "document-service"
    "session-service"
    "document-processing-service"
    "rag-query-service"
    "embedding-service"
    "llm-service"
    "celery-worker"
)

# 检查服务是否可扩展
if [[ ! " ${SCALABLE_SERVICES[@]} " =~ " ${SERVICE} " ]]; then
    print_error "服务 '$SERVICE' 不支持扩缩容"
    echo ""
    show_help
    exit 1
fi

# 构建 Docker Compose 命令
COMPOSE_CMD="docker-compose -f docker-compose.yml"
if [ -f docker-compose.nginx.yml ]; then
    COMPOSE_CMD="$COMPOSE_CMD -f docker-compose.nginx.yml"
fi

# 执行扩缩容
print_info "扩展 $SERVICE 到 $REPLICAS 个副本..."
$COMPOSE_CMD up -d --scale $SERVICE=$REPLICAS --no-recreate

# 等待服务启动
print_info "等待服务启动..."
sleep 5

# 显示服务状态
echo ""
echo "=========================================="
echo "服务状态:"
echo "=========================================="
$COMPOSE_CMD ps $SERVICE

print_success "扩缩容完成！"
echo ""
echo "提示:"
echo "  - 查看日志: ./logs.sh -f $SERVICE"
echo "  - 查看所有服务: docker-compose ps"
echo "  - 恢复默认副本数: $0 $SERVICE <默认数量>"
echo ""
