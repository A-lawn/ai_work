#!/bin/bash

# Sentinel 配置验证脚本

set -e

SENTINEL_DASHBOARD="${SENTINEL_DASHBOARD:-localhost:8858}"
NACOS_SERVER="${NACOS_SERVER:-localhost:8848}"
NAMESPACE="${NAMESPACE:-rag-system}"
GROUP="${GROUP:-SENTINEL_GROUP}"

echo "========================================="
echo "Sentinel 配置验证"
echo "========================================="

# 检查 Sentinel Dashboard 是否运行
echo ""
echo "1. 检查 Sentinel Dashboard 状态..."
if curl -s "http://$SENTINEL_DASHBOARD/" > /dev/null 2>&1; then
    echo "✓ Sentinel Dashboard 运行正常"
else
    echo "✗ Sentinel Dashboard 无法访问"
    exit 1
fi

# 检查 Nacos 连接
echo ""
echo "2. 检查 Nacos 连接..."
if curl -s "http://$NACOS_SERVER/nacos/v1/console/health/readiness" > /dev/null 2>&1; then
    echo "✓ Nacos 服务运行正常"
else
    echo "✗ Nacos 服务无法访问"
    exit 1
fi

# 检查规则是否已推送到 Nacos
echo ""
echo "3. 检查 Sentinel 规则持久化..."

check_rule() {
    local data_id=$1
    local result=$(curl -s "http://$NACOS_SERVER/nacos/v1/cs/configs?dataId=$data_id&group=$GROUP&tenant=$NAMESPACE")
    
    if [ -n "$result" ] && [ "$result" != "config data not exist" ]; then
        echo "✓ 规则存在: $data_id"
        return 0
    else
        echo "✗ 规则不存在: $data_id"
        return 1
    fi
}

check_rule "gateway-service-flow-rules"
check_rule "gateway-service-degrade-rules"
check_rule "document-service-flow-rules"
check_rule "document-service-degrade-rules"
check_rule "session-service-flow-rules"
check_rule "session-service-degrade-rules"
check_rule "auth-service-flow-rules"
check_rule "system-rules"

# 检查服务是否连接到 Sentinel Dashboard
echo ""
echo "4. 检查服务连接状态..."
echo "请在 Sentinel Dashboard 中查看服务列表"
echo "访问: http://$SENTINEL_DASHBOARD"
echo "用户名: sentinel"
echo "密码: sentinel"

echo ""
echo "========================================="
echo "验证完成"
echo "========================================="
echo ""
echo "下一步操作:"
echo "1. 访问 Sentinel Dashboard 查看实时监控"
echo "2. 在 Dashboard 中配置或调整规则"
echo "3. 规则会自动持久化到 Nacos"
echo "4. 使用压测工具验证限流和熔断功能"
echo "========================================="
