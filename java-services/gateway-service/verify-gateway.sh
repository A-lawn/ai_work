#!/bin/bash

# Gateway Service 验证脚本

GATEWAY_URL="http://localhost:8080"

echo "=========================================="
echo "Gateway Service 验证脚本"
echo "=========================================="
echo ""

# 1. 健康检查
echo "1. 测试健康检查端点..."
curl -s "${GATEWAY_URL}/actuator/health" | jq '.' || echo "健康检查失败"
echo ""

# 2. 详细健康检查
echo "2. 测试详细健康检查..."
curl -s "${GATEWAY_URL}/actuator/health/detail" | jq '.' || echo "详细健康检查失败"
echo ""

# 3. Prometheus 指标
echo "3. 测试 Prometheus 指标端点..."
curl -s "${GATEWAY_URL}/actuator/prometheus" | head -n 10
echo "..."
echo ""

# 4. 测试认证（应该返回 401）
echo "4. 测试未认证请求（应该返回 401）..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_URL}/api/v1/documents")
if [ "$HTTP_CODE" = "401" ]; then
    echo "✓ 认证保护正常工作（返回 401）"
else
    echo "✗ 认证保护异常（返回 $HTTP_CODE）"
fi
echo ""

# 5. 测试白名单路径（应该可以访问）
echo "5. 测试白名单路径（登录端点）..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${GATEWAY_URL}/api/auth/login" -X POST)
echo "登录端点返回状态码: $HTTP_CODE"
echo ""

# 6. 测试路由（需要认证）
echo "6. 测试路由转发..."
echo "注意：以下测试需要有效的 Token 或 API Key"
echo ""

# 7. 检查日志文件
echo "7. 检查日志文件..."
if [ -f "logs/gateway-service.log" ]; then
    echo "✓ 日志文件存在"
    echo "最近的日志条目："
    tail -n 5 logs/gateway-service.log
else
    echo "✗ 日志文件不存在（服务可能未启动）"
fi
echo ""

echo "=========================================="
echo "验证完成"
echo "=========================================="
echo ""
echo "提示："
echo "1. 确保 Gateway Service 已启动"
echo "2. 确保 Nacos 已启动并可访问"
echo "3. 确保其他依赖服务已启动"
echo "4. 查看 Sentinel Dashboard: http://localhost:8858"
echo "5. 查看 Zipkin UI: http://localhost:9411"
