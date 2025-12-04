#!/bin/bash

# 日志聚合验证脚本

set -e

ES_HOST="${ES_HOST:-localhost:9200}"
KIBANA_HOST="${KIBANA_HOST:-localhost:5601}"

echo "========================================="
echo "日志聚合验证"
echo "========================================="

# 检查 Elasticsearch 状态
echo ""
echo "1. 检查 Elasticsearch 状态..."
CLUSTER_HEALTH=$(curl -s "http://$ES_HOST/_cluster/health")
CLUSTER_STATUS=$(echo "$CLUSTER_HEALTH" | jq -r '.status')

if [ "$CLUSTER_STATUS" == "green" ] || [ "$CLUSTER_STATUS" == "yellow" ]; then
    echo "✓ Elasticsearch 集群状态: $CLUSTER_STATUS"
else
    echo "✗ Elasticsearch 集群状态异常: $CLUSTER_STATUS"
    exit 1
fi

# 检查索引
echo ""
echo "2. 检查日志索引..."
INDICES=$(curl -s "http://$ES_HOST/_cat/indices/logs-*?h=index,docs.count,store.size")

if [ -n "$INDICES" ]; then
    echo "✓ 发现日志索引:"
    echo "$INDICES" | while read line; do
        echo "  $line"
    done
else
    echo "⚠ 未发现日志索引"
fi

# 检查 ILM 策略
echo ""
echo "3. 检查索引生命周期管理策略..."
ILM_POLICY=$(curl -s "http://$ES_HOST/_ilm/policy/logs-policy")

if echo "$ILM_POLICY" | jq -e '.logs-policy' > /dev/null 2>&1; then
    echo "✓ ILM 策略已配置"
    echo "  保留时间: 7 天"
    echo "  滚动条件: 50GB 或 1 天"
else
    echo "⚠ ILM 策略未配置"
fi

# 检查索引模板
echo ""
echo "4. 检查索引模板..."
TEMPLATE=$(curl -s "http://$ES_HOST/_index_template/logs-template")

if echo "$TEMPLATE" | jq -e '.index_templates[0]' > /dev/null 2>&1; then
    echo "✓ 索引模板已配置"
else
    echo "⚠ 索引模板未配置"
fi

# 查询日志数量
echo ""
echo "5. 查询日志数量..."
TOTAL_LOGS=$(curl -s "http://$ES_HOST/logs-*/_count" | jq -r '.count')

if [ "$TOTAL_LOGS" -gt 0 ]; then
    echo "✓ 总日志数: $TOTAL_LOGS"
    
    # 按服务统计
    echo ""
    echo "  按服务统计:"
    curl -s "http://$ES_HOST/logs-*/_search" \
      -H 'Content-Type: application/json' \
      -d '{
        "size": 0,
        "aggs": {
          "services": {
            "terms": {
              "field": "service.keyword",
              "size": 20
            }
          }
        }
      }' | jq -r '.aggregations.services.buckets[] | "    \(.key): \(.doc_count)"'
    
    # 按日志级别统计
    echo ""
    echo "  按日志级别统计:"
    curl -s "http://$ES_HOST/logs-*/_search" \
      -H 'Content-Type: application/json' \
      -d '{
        "size": 0,
        "aggs": {
          "levels": {
            "terms": {
              "field": "level.keyword",
              "size": 10
            }
          }
        }
      }' | jq -r '.aggregations.levels.buckets[] | "    \(.key): \(.doc_count)"'
else
    echo "⚠ 未发现日志数据"
    echo "  可能原因:"
    echo "  1. Filebeat 未启动或未正确配置"
    echo "  2. 服务还未产生日志"
    echo "  3. 日志文件路径配置错误"
fi

# 查询最近的错误日志
echo ""
echo "6. 查询最近的错误日志..."
ERROR_LOGS=$(curl -s "http://$ES_HOST/logs-*/_search" \
  -H 'Content-Type: application/json' \
  -d '{
    "query": {
      "term": {"level.keyword": "ERROR"}
    },
    "sort": [{"@timestamp": "desc"}],
    "size": 5
  }')

ERROR_COUNT=$(echo "$ERROR_LOGS" | jq -r '.hits.total.value')

if [ "$ERROR_COUNT" -gt 0 ]; then
    echo "⚠ 发现 $ERROR_COUNT 条错误日志"
    echo "  最近 5 条错误:"
    echo "$ERROR_LOGS" | jq -r '.hits.hits[] | "    [\(._source["@timestamp"] // ._source.timestamp)] \(._source.service) - \(._source.message)"'
else
    echo "✓ 未发现错误日志"
fi

# 检查 Kibana 状态
echo ""
echo "7. 检查 Kibana 状态..."
if curl -s "http://$KIBANA_HOST/api/status" > /dev/null 2>&1; then
    KIBANA_STATUS=$(curl -s "http://$KIBANA_HOST/api/status" | jq -r '.status.overall.state')
    echo "✓ Kibana 状态: $KIBANA_STATUS"
else
    echo "⚠ Kibana 未运行或无法访问"
fi

# 检查 Filebeat 状态
echo ""
echo "8. 检查 Filebeat 状态..."
if docker ps | grep -q filebeat; then
    echo "✓ Filebeat 容器运行中"
    
    # 检查 Filebeat 日志
    echo "  最近的 Filebeat 日志:"
    docker logs filebeat --tail 5 2>&1 | sed 's/^/    /'
else
    echo "⚠ Filebeat 容器未运行"
fi

echo ""
echo "========================================="
echo "验证完成"
echo "========================================="
echo ""
echo "访问 Elasticsearch: http://$ES_HOST"
echo "访问 Kibana: http://$KIBANA_HOST"
echo ""
echo "常用操作:"
echo "1. 查看所有索引: curl http://$ES_HOST/_cat/indices?v"
echo "2. 查询日志: curl http://$ES_HOST/logs-*/_search"
echo "3. 删除旧索引: curl -X DELETE http://$ES_HOST/logs-YYYY.MM.DD"
echo "========================================="
