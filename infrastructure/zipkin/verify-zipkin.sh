#!/bin/bash

# Zipkin 链路追踪验证脚本

set -e

ZIPKIN_URL="${ZIPKIN_URL:-localhost:9411}"
GATEWAY_URL="${GATEWAY_URL:-localhost:8080}"

echo "========================================="
echo "Zipkin 链路追踪验证"
echo "========================================="

# 检查 Zipkin 服务状态
echo ""
echo "1. 检查 Zipkin 服务状态..."
if curl -s "http://$ZIPKIN_URL/health" > /dev/null 2>&1; then
    echo "✓ Zipkin 服务运行正常"
else
    echo "✗ Zipkin 服务无法访问"
    exit 1
fi

# 检查 Elasticsearch 连接
echo ""
echo "2. 检查 Elasticsearch 存储..."
STORAGE_TYPE=$(curl -s "http://$ZIPKIN_URL/api/v2/dependencies?endTs=$(date +%s)000&lookback=86400000" | jq -r 'type')
if [ "$STORAGE_TYPE" != "null" ]; then
    echo "✓ Elasticsearch 存储正常"
else
    echo "⚠ Elasticsearch 存储可能未配置"
fi

# 检查服务列表
echo ""
echo "3. 检查已注册的服务..."
SERVICES=$(curl -s "http://$ZIPKIN_URL/api/v2/services")
SERVICE_COUNT=$(echo "$SERVICES" | jq -r 'length')

if [ "$SERVICE_COUNT" -gt 0 ]; then
    echo "✓ 发现 $SERVICE_COUNT 个服务:"
    echo "$SERVICES" | jq -r '.[]' | while read service; do
        echo "  - $service"
    done
else
    echo "⚠ 未发现任何服务，可能还没有 Trace 数据"
fi

# 发送测试请求生成 Trace
echo ""
echo "4. 发送测试请求生成 Trace..."
if curl -s -X GET "http://$GATEWAY_URL/actuator/health" > /dev/null 2>&1; then
    echo "✓ 测试请求发送成功"
    echo "  等待 5 秒让 Trace 数据上报..."
    sleep 5
else
    echo "⚠ 无法发送测试请求到 Gateway"
fi

# 查询最近的 Trace
echo ""
echo "5. 查询最近的 Trace..."
END_TS=$(date +%s)000
START_TS=$((END_TS - 3600000))  # 最近 1 小时

TRACES=$(curl -s "http://$ZIPKIN_URL/api/v2/traces?endTs=$END_TS&lookback=3600000&limit=10")
TRACE_COUNT=$(echo "$TRACES" | jq -r 'length')

if [ "$TRACE_COUNT" -gt 0 ]; then
    echo "✓ 发现 $TRACE_COUNT 条 Trace 记录"
    
    # 显示第一条 Trace 的详细信息
    echo ""
    echo "最新 Trace 详情:"
    FIRST_TRACE=$(echo "$TRACES" | jq -r '.[0]')
    TRACE_ID=$(echo "$FIRST_TRACE" | jq -r '.[0].traceId')
    SPAN_COUNT=$(echo "$FIRST_TRACE" | jq -r 'length')
    
    echo "  Trace ID: $TRACE_ID"
    echo "  Span 数量: $SPAN_COUNT"
    echo "  调用链路:"
    
    echo "$FIRST_TRACE" | jq -r '.[] | "    \(.name) [\(.localEndpoint.serviceName)] - \(.duration/1000)ms"'
else
    echo "⚠ 未发现 Trace 记录"
    echo "  可能原因:"
    echo "  1. 服务还未发送 Trace 数据"
    echo "  2. 采样率设置过低"
    echo "  3. 服务未正确配置 Zipkin"
fi

# 检查依赖关系
echo ""
echo "6. 检查服务依赖关系..."
DEPENDENCIES=$(curl -s "http://$ZIPKIN_URL/api/v2/dependencies?endTs=$END_TS&lookback=86400000")
DEP_COUNT=$(echo "$DEPENDENCIES" | jq -r 'length')

if [ "$DEP_COUNT" -gt 0 ]; then
    echo "✓ 发现 $DEP_COUNT 个服务依赖关系:"
    echo "$DEPENDENCIES" | jq -r '.[] | "  \(.parent) -> \(.child) (调用次数: \(.callCount))"'
else
    echo "⚠ 未发现服务依赖关系"
fi

# 检查采样率配置
echo ""
echo "7. 检查服务采样率配置..."
echo "  请检查各服务的配置文件:"
echo "  - Java 服务: application.yml 中的 spring.sleuth.sampler.probability"
echo "  - Python 服务: 代码中的 sample_rate 参数"
echo ""
echo "  推荐配置:"
echo "  - 开发环境: 100% (1.0)"
echo "  - 测试环境: 50% (0.5)"
echo "  - 生产环境: 10% (0.1)"

echo ""
echo "========================================="
echo "验证完成"
echo "========================================="
echo ""
echo "访问 Zipkin UI 查看详细信息:"
echo "http://$ZIPKIN_URL"
echo ""
echo "常用功能:"
echo "1. 搜索 Trace: 按服务名、操作名、标签等搜索"
echo "2. 查看依赖关系: Dependencies 页面"
echo "3. 分析性能: 查看 Trace 时间线和 Span 耗时"
echo "========================================="
