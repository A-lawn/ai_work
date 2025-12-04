#!/bin/bash

# 智能运维问答助手 - 日志查看脚本

set -e

# 颜色定义
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 解析命令行参数
SERVICE=""
FOLLOW=false
TAIL=100

while [[ $# -gt 0 ]]; do
    case $1 in
        -f|--follow)
            FOLLOW=true
            shift
            ;;
        -n|--tail)
            TAIL="$2"
            shift 2
            ;;
        --help)
            echo "使用方法: $0 [选项] [服务名]"
            echo ""
            echo "选项:"
            echo "  -f, --follow     实时跟踪日志"
            echo "  -n, --tail NUM   显示最后 NUM 行日志（默认: 100）"
            echo "  --help           显示此帮助信息"
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
            echo "  nacos                        Nacos 服务注册中心"
            echo "  sentinel-dashboard           Sentinel 控制台"
            echo "  rabbitmq                     RabbitMQ 消息队列"
            echo "  postgres                     PostgreSQL 数据库"
            echo "  redis                        Redis 缓存"
            echo "  chroma                       ChromaDB 向量数据库"
            echo ""
            echo "示例:"
            echo "  $0                           # 查看所有服务日志"
            echo "  $0 gateway-service           # 查看网关服务日志"
            echo "  $0 -f gateway-service        # 实时跟踪网关服务日志"
            echo "  $0 -n 50 document-service    # 查看文档服务最后 50 行日志"
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

# 查看日志
if [ -z "$SERVICE" ]; then
    print_info "查看所有服务日志..."
    if [ "$FOLLOW" = true ]; then
        $COMPOSE_CMD logs -f --tail=$TAIL
    else
        $COMPOSE_CMD logs --tail=$TAIL
    fi
else
    print_info "查看 $SERVICE 日志..."
    if [ "$FOLLOW" = true ]; then
        $COMPOSE_CMD logs -f --tail=$TAIL $SERVICE
    else
        $COMPOSE_CMD logs --tail=$TAIL $SERVICE
    fi
fi
