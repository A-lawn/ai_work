#!/bin/bash

# 智能运维问答助手 - 重启脚本

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
    echo "使用方法: $0 [服务名]"
    echo ""
    echo "不指定服务名时，重启所有服务"
    echo ""
    echo "可用服务:"
    echo "  gateway-service              API 网关"
    echo "  document-service             文档管理服务"
    echo "  session-service              会话管理服务"
    echo "  auth-service                 认证授权服务"
    echo "  monitor-service              监控日志服务"
    echo "  config-service               配置管理服务"
    echo "  document-processing-service  文档处理服务"
    echo "  rag-query-service            RAG 查询服务"
    echo "  embedding-service            嵌入模型服务"
    echo "  llm-service                  大模型服务"
    echo "  celery-worker                Celery 异步任务"
    echo "  batch-consumer               批量处理消费者"
    echo "  frontend                     前端应用"
    echo "  nacos                        Nacos 服务注册中心"
    echo "  sentinel-dashboard           Sentinel 控制台"
    echo "  rabbitmq                     RabbitMQ 消息队列"
    echo "  postgres                     PostgreSQL 数据库"
    echo "  redis                        Redis 缓存"
    echo "  chroma                       ChromaDB 向量数据库"
    echo ""
    echo "示例:"
    echo "  $0                           # 重启所有服务"
    echo "  $0 gateway-service           # 重启网关服务"
    echo "  $0 rag-query-service         # 重启 RAG 查询服务"
}

# 解析命令行参数
SERVICE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --help)
            show_help
            exit 0
            ;;
        *)
            SERVICE="$1"
            shift
            ;;
    esac
done

# 构建 Docker Compose 命令
COMPOSE_CMD="docker-compose -f docker-compose.yml"
if [ -f docker-compose.nginx.yml ]; then
    COMPOSE_CMD="$COMPOSE_CMD -f docker-compose.nginx.yml"
fi

echo "=========================================="
echo "智能运维问答助手 - 重启服务"
echo "=========================================="
echo ""

# 重启服务
if [ -z "$SERVICE" ]; then
    print_info "重启所有服务..."
    $COMPOSE_CMD restart
    print_success "所有服务已重启"
else
    # 检查服务是否存在
    if ! $COMPOSE_CMD ps --services | grep -q "^${SERVICE}$"; then
        print_error "服务 '$SERVICE' 不存在"
        echo ""
        show_help
        exit 1
    fi
    
    print_info "重启 $SERVICE..."
    $COMPOSE_CMD restart $SERVICE
    print_success "$SERVICE 已重启"
fi

# 等待服务启动
print_info "等待服务启动..."
sleep 5

# 显示服务状态
echo ""
echo "=========================================="
echo "服务状态:"
echo "=========================================="
if [ -z "$SERVICE" ]; then
    $COMPOSE_CMD ps
else
    $COMPOSE_CMD ps $SERVICE
fi

echo ""
print_success "重启完成！"
echo ""
echo "提示:"
if [ -n "$SERVICE" ]; then
    echo "  查看日志: ./logs.sh -f $SERVICE"
else
    echo "  查看日志: ./logs.sh -f"
fi
echo "  检查状态: ./status.sh"
echo ""
