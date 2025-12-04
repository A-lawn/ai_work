#!/bin/bash

# 智能运维问答助手 - 启动脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo "=========================================="
echo "智能运维问答助手 - 启动中..."
echo "=========================================="

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    print_error "Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    print_error "Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    print_error "Docker 未运行，请先启动 Docker"
    exit 1
fi

# 检查 .env 文件是否存在
if [ ! -f .env ]; then
    print_warning ".env 文件不存在，从 .env.example 复制..."
    cp .env.example .env
    print_warning "请编辑 .env 文件，配置必要的环境变量（特别是 OPENAI_API_KEY）"
    print_warning "配置完成后，重新运行此脚本"
    exit 1
fi

# 解析命令行参数
WITH_NGINX=false
BUILD=false
SCALE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --nginx)
            WITH_NGINX=true
            shift
            ;;
        --build)
            BUILD=true
            shift
            ;;
        --scale)
            SCALE="$2"
            shift 2
            ;;
        --help)
            echo "使用方法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  --nginx       启用 Nginx 负载均衡器"
            echo "  --build       重新构建 Docker 镜像"
            echo "  --scale       扩展服务副本数，格式: service=num (例如: rag-query-service=5)"
            echo "  --help        显示此帮助信息"
            echo ""
            echo "示例:"
            echo "  $0                                    # 启动所有服务"
            echo "  $0 --nginx                            # 启用 Nginx 负载均衡"
            echo "  $0 --build                            # 重新构建并启动"
            echo "  $0 --scale rag-query-service=5        # 扩展 RAG 查询服务到 5 个副本"
            exit 0
            ;;
        *)
            print_error "未知选项: $1"
            echo "使用 --help 查看帮助"
            exit 1
            ;;
    esac
done

# 构建 Docker Compose 命令
COMPOSE_CMD="docker-compose -f docker-compose.yml"
if [ "$WITH_NGINX" = true ]; then
    COMPOSE_CMD="$COMPOSE_CMD -f docker-compose.nginx.yml"
    print_info "启用 Nginx 负载均衡器"
    
    # 检查 SSL 证书是否存在
    if [ ! -f infrastructure/nginx/ssl/cert.pem ] || [ ! -f infrastructure/nginx/ssl/key.pem ]; then
        print_warning "SSL 证书不存在，正在生成自签名证书..."
        if [ -f infrastructure/nginx/generate-ssl-cert.sh ]; then
            bash infrastructure/nginx/generate-ssl-cert.sh
        else
            print_error "SSL 证书生成脚本不存在"
            exit 1
        fi
    fi
fi

# 构建镜像（如果需要）
if [ "$BUILD" = true ]; then
    print_info "重新构建 Docker 镜像..."
    $COMPOSE_CMD build --no-cache
fi

# 启动所有服务
print_info "启动所有服务..."
if [ -n "$SCALE" ]; then
    print_info "扩展服务: $SCALE"
    $COMPOSE_CMD up -d --scale $SCALE
else
    $COMPOSE_CMD up -d
fi

# 等待服务启动
print_info "等待服务启动..."
sleep 15

# 检查关键服务健康状态
print_info "检查服务健康状态..."
check_service() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "$url" > /dev/null 2>&1; then
            print_success "$service 已就绪"
            return 0
        fi
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_warning "$service 启动超时或未就绪"
    return 1
}

check_service "Nacos" "http://localhost:8848/nacos/"
check_service "Gateway" "http://localhost:8080/actuator/health"
check_service "ChromaDB" "http://localhost:8001/api/v1/heartbeat"

# 检查服务状态
echo ""
echo "=========================================="
echo "服务状态:"
echo "=========================================="
$COMPOSE_CMD ps

echo ""
echo "=========================================="
echo "服务访问地址:"
echo "=========================================="
if [ "$WITH_NGINX" = true ]; then
    echo "前端应用: https://localhost (HTTPS)"
    echo "前端应用: http://localhost (HTTP, 自动重定向到 HTTPS)"
else
    echo "前端应用: http://localhost"
fi
echo "API 网关: http://localhost:8080"
echo ""
echo "管理控制台:"
echo "  Nacos: http://localhost:8848/nacos (nacos/nacos)"
echo "  Sentinel: http://localhost:8858 (sentinel/sentinel)"
echo "  RabbitMQ: http://localhost:15672 (admin/admin123)"
echo "  MinIO: http://localhost:9001 (admin/admin123456)"
echo ""
echo "监控系统:"
echo "  Zipkin: http://localhost:9411"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3001 (admin/admin)"
echo "=========================================="

echo ""
print_success "所有服务已启动！"
echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f [service-name]"
echo "  查看所有日志: docker-compose logs -f"
echo "  停止服务: ./stop.sh 或 docker-compose down"
echo "  重启服务: docker-compose restart [service-name]"
echo "  扩展服务: docker-compose up -d --scale service-name=N"
echo ""
