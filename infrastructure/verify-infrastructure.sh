#!/bin/bash

# 基础设施验证脚本
# 用于验证所有基础设施服务是否正常运行

echo "=========================================="
echo "  RAG 系统基础设施验证脚本"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 验证函数
check_service() {
    local service_name=$1
    local url=$2
    local expected_code=${3:-200}
    
    echo -n "检查 ${service_name}... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" ${url} 2>/dev/null)
    
    if [ "$response" -eq "$expected_code" ]; then
        echo -e "${GREEN}✓ 正常${NC} (HTTP ${response})"
        return 0
    else
        echo -e "${RED}✗ 异常${NC} (HTTP ${response})"
        return 1
    fi
}

# 计数器
total=0
passed=0

# 1. 检查 Nacos
echo "1. 服务注册与配置中心"
echo "----------------------------"
((total++))
if check_service "Nacos" "http://localhost:8848/nacos/" 302; then
    ((passed++))
fi

# 检查 Nacos 命名空间
echo -n "检查 Nacos 命名空间... "
((total++))
namespaces=$(curl -s "http://localhost:8848/nacos/v1/console/namespaces" 2>/dev/null)
if echo "$namespaces" | grep -q "rag-system"; then
    echo -e "${GREEN}✓ 正常${NC} (命名空间已创建)"
    ((passed++))
else
    echo -e "${RED}✗ 异常${NC} (命名空间未创建)"
fi

echo ""

# 2. 检查 Sentinel
echo "2. 流量控制"
echo "----------------------------"
((total++))
if check_service "Sentinel Dashboard" "http://localhost:8858/" 200; then
    ((passed++))
fi
echo ""

# 3. 检查 RabbitMQ
echo "3. 消息队列"
echo "----------------------------"
((total++))
if check_service "RabbitMQ Management" "http://localhost:15672/" 200; then
    ((passed++))
fi

# 检查 RabbitMQ 队列
echo -n "检查 RabbitMQ 队列... "
((total++))
queues=$(curl -s -u admin:admin123 "http://localhost:15672/api/queues/rag-system" 2>/dev/null)
if echo "$queues" | grep -q "document.processing.queue"; then
    echo -e "${GREEN}✓ 正常${NC} (队列已创建)"
    ((passed++))
else
    echo -e "${YELLOW}⚠ 警告${NC} (队列未创建，请运行 rabbitmq-init)"
fi
echo ""

# 4. 检查 Zipkin
echo "4. 链路追踪"
echo "----------------------------"
((total++))
if check_service "Zipkin" "http://localhost:9411/health" 200; then
    ((passed++))
fi
echo ""

# 5. 检查 Prometheus
echo "5. 监控系统"
echo "----------------------------"
((total++))
if check_service "Prometheus" "http://localhost:9090/-/healthy" 200; then
    ((passed++))
fi

((total++))
if check_service "Grafana" "http://localhost:3001/api/health" 200; then
    ((passed++))
fi
echo ""

# 6. 检查数据存储
echo "6. 数据存储"
echo "----------------------------"
echo -n "检查 PostgreSQL... "
((total++))
if docker-compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 正常${NC}"
    ((passed++))
else
    echo -e "${RED}✗ 异常${NC}"
fi

echo -n "检查 Redis... "
((total++))
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 正常${NC}"
    ((passed++))
else
    echo -e "${RED}✗ 异常${NC}"
fi

((total++))
if check_service "ChromaDB" "http://localhost:8001/api/v1/heartbeat" 200; then
    ((passed++))
fi

((total++))
if check_service "Elasticsearch" "http://localhost:9200/_cluster/health" 200; then
    ((passed++))
fi

((total++))
if check_service "MinIO" "http://localhost:9000/minio/health/live" 200; then
    ((passed++))
fi
echo ""

# 7. 总结
echo "=========================================="
echo "  验证结果"
echo "=========================================="
echo ""
echo "总计: ${total} 项检查"
echo -e "通过: ${GREEN}${passed}${NC} 项"
echo -e "失败: ${RED}$((total - passed))${NC} 项"
echo ""

if [ $passed -eq $total ]; then
    echo -e "${GREEN}✓ 所有基础设施服务运行正常！${NC}"
    echo ""
    echo "您可以访问以下地址："
    echo "  - Nacos:     http://localhost:8848/nacos (nacos/nacos)"
    echo "  - Sentinel:  http://localhost:8858 (sentinel/sentinel)"
    echo "  - RabbitMQ:  http://localhost:15672 (admin/admin123)"
    echo "  - Zipkin:    http://localhost:9411"
    echo "  - Prometheus: http://localhost:9090"
    echo "  - Grafana:   http://localhost:3001 (admin/admin)"
    echo "  - MinIO:     http://localhost:9001 (admin/admin123456)"
    exit 0
else
    echo -e "${RED}✗ 部分服务异常，请检查日志${NC}"
    echo ""
    echo "查看日志命令："
    echo "  docker-compose logs <service-name>"
    echo ""
    echo "重启服务命令："
    echo "  docker-compose restart <service-name>"
    exit 1
fi
