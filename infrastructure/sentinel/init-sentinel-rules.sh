#!/bin/bash

# Sentinel 规则初始化脚本
# 将 Sentinel 规则推送到 Nacos 配置中心

set -e

NACOS_SERVER="${NACOS_SERVER:-localhost:8848}"
NAMESPACE="${NAMESPACE:-rag-system}"
GROUP="${GROUP:-SENTINEL_GROUP}"
RULES_DIR="$(dirname "$0")/rules"

echo "========================================="
echo "Sentinel 规则初始化"
echo "========================================="
echo "Nacos Server: $NACOS_SERVER"
echo "Namespace: $NAMESPACE"
echo "Group: $GROUP"
echo "========================================="

# 等待 Nacos 启动
echo "等待 Nacos 服务启动..."
for i in {1..30}; do
    if curl -s "http://$NACOS_SERVER/nacos/v1/console/health/readiness" > /dev/null 2>&1; then
        echo "Nacos 服务已就绪"
        break
    fi
    echo "等待 Nacos 启动... ($i/30)"
    sleep 2
done

# 推送规则到 Nacos
push_rule() {
    local service=$1
    local rule_type=$2
    local file=$3
    
    if [ ! -f "$file" ]; then
        echo "警告: 规则文件不存在: $file"
        return
    fi
    
    local data_id="${service}-${rule_type}"
    local content=$(cat "$file" | jq -c .)
    
    echo "推送规则: $data_id"
    
    curl -X POST "http://$NACOS_SERVER/nacos/v1/cs/configs" \
        -d "dataId=$data_id" \
        -d "group=$GROUP" \
        -d "content=$content" \
        -d "type=json" \
        -d "tenant=$NAMESPACE" \
        > /dev/null 2>&1
    
    if [ $? -eq 0 ]; then
        echo "✓ 成功推送: $data_id"
    else
        echo "✗ 推送失败: $data_id"
    fi
}

# 推送 Gateway Service 规则
echo ""
echo "推送 Gateway Service 规则..."
push_rule "gateway-service" "flow-rules" "$RULES_DIR/gateway-service-flow-rules.json"
push_rule "gateway-service" "degrade-rules" "$RULES_DIR/gateway-service-degrade-rules.json"

# 推送 Document Service 规则
echo ""
echo "推送 Document Service 规则..."
push_rule "document-service" "flow-rules" "$RULES_DIR/document-service-flow-rules.json"
push_rule "document-service" "degrade-rules" "$RULES_DIR/document-service-degrade-rules.json"

# 推送 Session Service 规则
echo ""
echo "推送 Session Service 规则..."
push_rule "session-service" "flow-rules" "$RULES_DIR/session-service-flow-rules.json"
push_rule "session-service" "degrade-rules" "$RULES_DIR/session-service-degrade-rules.json"

# 推送 Auth Service 规则
echo ""
echo "推送 Auth Service 规则..."
push_rule "auth-service" "flow-rules" "$RULES_DIR/auth-service-flow-rules.json"

# 推送系统规则
echo ""
echo "推送系统保护规则..."
push_rule "system" "rules" "$RULES_DIR/system-rules.json"

# 推送热点参数规则
echo ""
echo "推送热点参数规则..."
push_rule "hot-param" "rules" "$RULES_DIR/hot-param-rules.json"

echo ""
echo "========================================="
echo "Sentinel 规则初始化完成"
echo "========================================="
echo ""
echo "访问 Sentinel Dashboard: http://localhost:8858"
echo "用户名: sentinel"
echo "密码: sentinel"
echo ""
echo "访问 Nacos 控制台查看规则: http://localhost:8848/nacos"
echo "命名空间: $NAMESPACE"
echo "分组: $GROUP"
echo "========================================="
