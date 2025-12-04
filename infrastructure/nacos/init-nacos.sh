#!/bin/bash

# Nacos 初始化脚本
# 用于创建命名空间、配置文件和 Sentinel 规则

NACOS_SERVER="http://localhost:8848"
NAMESPACE_ID="rag-system"
NAMESPACE_NAME="RAG系统命名空间"

echo "等待 Nacos 服务启动..."
until curl -f "${NACOS_SERVER}/nacos/v1/console/health/readiness" > /dev/null 2>&1; do
    echo "Nacos 尚未就绪，等待 5 秒..."
    sleep 5
done

echo "Nacos 服务已就绪，开始初始化..."

# 1. 创建命名空间
echo "创建命名空间: ${NAMESPACE_NAME}"
curl -X POST "${NACOS_SERVER}/nacos/v1/console/namespaces" \
    -d "customNamespaceId=${NAMESPACE_ID}" \
    -d "namespaceName=${NAMESPACE_NAME}" \
    -d "namespaceDesc=智能运维问答助手系统命名空间"

# 2. 上传公共配置文件
echo "上传公共配置文件: common-config.yaml"
curl -X POST "${NACOS_SERVER}/nacos/v1/cs/configs" \
    -d "tenant=${NAMESPACE_ID}" \
    -d "dataId=common-config.yaml" \
    -d "group=DEFAULT_GROUP" \
    -d "content=$(cat /nacos-config/common-config.yaml)" \
    -d "type=yaml"

# 3. 上传各服务配置文件
for service in gateway-service document-service session-service auth-service monitor-service config-service; do
    echo "上传服务配置: ${service}.yaml"
    if [ -f "/nacos-config/${service}.yaml" ]; then
        curl -X POST "${NACOS_SERVER}/nacos/v1/cs/configs" \
            -d "tenant=${NAMESPACE_ID}" \
            -d "dataId=${service}.yaml" \
            -d "group=DEFAULT_GROUP" \
            -d "content=$(cat /nacos-config/${service}.yaml)" \
            -d "type=yaml"
    fi
done

# 4. 上传 Sentinel 规则配置
echo "上传 Sentinel 流控规则"
for service in gateway-service document-service session-service auth-service monitor-service config-service; do
    if [ -f "/nacos-config/sentinel/${service}-flow-rules.json" ]; then
        curl -X POST "${NACOS_SERVER}/nacos/v1/cs/configs" \
            -d "tenant=${NAMESPACE_ID}" \
            -d "dataId=${service}-flow-rules" \
            -d "group=SENTINEL_GROUP" \
            -d "content=$(cat /nacos-config/sentinel/${service}-flow-rules.json)" \
            -d "type=json"
    fi
done

echo "上传 Sentinel 熔断规则"
for service in gateway-service document-service session-service auth-service monitor-service config-service; do
    if [ -f "/nacos-config/sentinel/${service}-degrade-rules.json" ]; then
        curl -X POST "${NACOS_SERVER}/nacos/v1/cs/configs" \
            -d "tenant=${NAMESPACE_ID}" \
            -d "dataId=${service}-degrade-rules" \
            -d "group=SENTINEL_GROUP" \
            -d "content=$(cat /nacos-config/sentinel/${service}-degrade-rules.json)" \
            -d "type=json"
    fi
done

echo "上传 Sentinel 系统保护规则"
for service in gateway-service document-service session-service auth-service monitor-service config-service; do
    if [ -f "/nacos-config/sentinel/${service}-system-rules.json" ]; then
        curl -X POST "${NACOS_SERVER}/nacos/v1/cs/configs" \
            -d "tenant=${NAMESPACE_ID}" \
            -d "dataId=${service}-system-rules" \
            -d "group=SENTINEL_GROUP" \
            -d "content=$(cat /nacos-config/sentinel/${service}-system-rules.json)" \
            -d "type=json"
    fi
done

echo "Nacos 初始化完成！"
