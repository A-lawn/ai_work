#!/bin/bash

# 智能运维问答助手 - 停止脚本

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo "=========================================="
echo "智能运维问答助手 - 停止中..."
echo "=========================================="

# 解析命令行参数
REMOVE_VOLUMES=false
REMOVE_IMAGES=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--volumes)
            REMOVE_VOLUMES=true
            shift
            ;;
        --images)
            REMOVE_IMAGES=true
            shift
            ;;
        --help)
            echo "使用方法: $0 [选项]"
            echo ""
            echo "选项:"
            echo "  -v, --volumes    同时删除数据卷（警告：会删除所有数据）"
            echo "  --images         同时删除 Docker 镜像"
            echo "  --help           显示此帮助信息"
            echo ""
            echo "示例:"
            echo "  $0                # 停止所有服务"
            echo "  $0 -v             # 停止服务并删除数据卷"
            echo "  $0 --images       # 停止服务并删除镜像"
            exit 0
            ;;
        *)
            print_warning "未知选项: $1"
            echo "使用 --help 查看帮助"
            exit 1
            ;;
    esac
done

# 构建 Docker Compose 命令
COMPOSE_CMD="docker-compose -f docker-compose.yml"
if [ -f docker-compose.nginx.yml ]; then
    COMPOSE_CMD="$COMPOSE_CMD -f docker-compose.nginx.yml"
fi

# 停止所有服务
if [ "$REMOVE_VOLUMES" = true ]; then
    print_warning "停止服务并删除数据卷..."
    read -p "确认删除所有数据？这将无法恢复！(yes/no): " confirm
    if [ "$confirm" = "yes" ]; then
        $COMPOSE_CMD down -v
        print_success "服务已停止，数据卷已删除"
    else
        print_info "取消操作"
        exit 0
    fi
else
    print_info "停止所有服务..."
    $COMPOSE_CMD down
    print_success "所有服务已停止"
fi

# 删除镜像（如果需要）
if [ "$REMOVE_IMAGES" = true ]; then
    print_info "删除 Docker 镜像..."
    docker images | grep "rag-ops" | awk '{print $3}' | xargs -r docker rmi -f
    print_success "镜像已删除"
fi

echo ""
echo "=========================================="
echo "清理完成"
echo "=========================================="
if [ "$REMOVE_VOLUMES" = false ]; then
    echo "数据卷已保留，下次启动时数据将恢复"
    echo "如需删除数据卷，请运行: $0 -v"
fi
echo ""
