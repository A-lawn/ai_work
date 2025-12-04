#!/bin/bash

# 智能运维问答助手 - 状态检查脚本

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
    echo -e "${GREEN}[✓]${NC} $1"
}

print_error() {
    echo -e "${RED}[✗]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[!]${NC} $1"
}

echo "=========================================="
echo "智能运维问答助手 - 系统状态检查"
echo "=========================================="
echo ""

# 构建 Docker Compose 命令
COMPOSE_CMD="docker-compose -f docker-compose.yml"
if [ -f docker-compose.nginx.yml ]; then
    COMPOSE_CMD="$COMPOSE_CMD -f docker-compose.nginx.yml"
fi

# 检查服务健康状态
check_service() {
    local service=$1
    local url=$2
    local name=$3
    
    if curl -f -s "$url" > /dev/null 2>&1; then
        print_success "$name 运行正常"
        return 0
    else
        print_error "$name 无法访问"
        return 1
    fi
}

# 检查容器状态
check_container() {
    local container=$1
    local name=$2
    
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        local status=$(docker inspect --format='{{.State.Status}}' $container 2>/dev/null)
        if [ "$status" = "running" ]; then
            print_success "$name 容器运行中"
            return 0
        else
            print_warning "$name 容器状态: $status"
            return 1
        fi
    else
        print_error "$name 容器未运行"
        return 1
    fi
}

print_info "检查基础设施服务..."
check_service "Nacos" "http://localhost:8848/nacos/" "Nacos 服务注册中心"
check_service "Sentinel" "http://localhost:8858/" "Sentinel 控制台"
check_service "RabbitMQ" "http://localhost:15672/" "RabbitMQ 消息队列"
check_service "Zipkin" "http://localhost:9411/health" "Zipkin 链路追踪"
check_service "Prometheus" "http://localhost:9090/-/healthy" "Prometheus 监控"
check_service "Grafana" "http://localhost:3001/api/health" "Grafana 可视化"

echo ""
print_info "检查数据存储服务..."
check_container "postgres" "PostgreSQL 数据库"
check_container "redis" "Redis 缓存"
check_service "ChromaDB" "http://localhost:8001/api/v1/heartbeat" "ChromaDB 向量数据库"
check_service "Elasticsearch" "http://localhost:9200/_cluster/health" "Elasticsearch 日志存储"
check_service "MinIO" "http://localhost:9000/minio/health/live" "MinIO 对象存储"

echo ""
print_info "检查 Java 微服务..."
check_service "Gateway" "http://localhost:8080/actuator/health" "API 网关服务"
check_service "Document" "http://localhost:8081/actuator/health" "文档管理服务"
check_service "Session" "http://localhost:8082/api/health" "会话管理服务"
check_service "Auth" "http://localhost:8083/actuator/health" "认证授权服务"
check_service "Monitor" "http://localhost:8084/api/health" "监控日志服务"
check_service "Config" "http://localhost:8085/api/health" "配置管理服务"

echo ""
print_info "检查 Python 微服务..."
check_service "Document Processing" "http://localhost:9001/health" "文档处理服务"
check_service "RAG Query" "http://localhost:9002/health" "RAG 查询服务"
check_service "Embedding" "http://localhost:9003/health" "嵌入模型服务"
check_service "LLM" "http://localhost:9004/api/health" "大模型服务"

echo ""
print_info "检查前端服务..."
if [ -f docker-compose.nginx.yml ] && docker ps --format '{{.Names}}' | grep -q "^nginx-lb$"; then
    check_service "Nginx LB" "http://localhost/health" "Nginx 负载均衡器"
    check_service "Frontend (HTTPS)" "https://localhost/health" "前端应用 (HTTPS)"
else
    check_service "Frontend" "http://localhost/health" "前端应用"
fi

echo ""
print_info "检查后台任务服务..."
check_container "celery-worker" "Celery 异步任务"
check_container "batch-consumer" "批量处理消费者"

echo ""
echo "=========================================="
echo "容器资源使用情况:"
echo "=========================================="
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}" | head -20

echo ""
echo "=========================================="
echo "服务副本数:"
echo "=========================================="
$COMPOSE_CMD ps --format "table {{.Service}}\t{{.State}}\t{{.Status}}"

echo ""
echo "=========================================="
echo "磁盘使用情况:"
echo "=========================================="
docker system df

echo ""
print_info "状态检查完成"
echo ""
echo "常用命令:"
echo "  查看详细日志: ./logs.sh -f [service-name]"
echo "  重启服务: docker-compose restart [service-name]"
echo "  扩展服务: ./scale.sh <service-name> <replicas>"
echo ""
