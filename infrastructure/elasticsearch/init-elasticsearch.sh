#!/bin/bash

# Elasticsearch 初始化脚本
# 创建索引模板和 ILM 策略

set -e

ES_HOST="${ES_HOST:-localhost:9200}"

echo "========================================="
echo "Elasticsearch 初始化"
echo "========================================="
echo "Elasticsearch Host: $ES_HOST"
echo "========================================="

# 等待 Elasticsearch 启动
echo "等待 Elasticsearch 服务启动..."
for i in {1..30}; do
    if curl -s "http://$ES_HOST/_cluster/health" > /dev/null 2>&1; then
        echo "Elasticsearch 服务已就绪"
        break
    fi
    echo "等待 Elasticsearch 启动... ($i/30)"
    sleep 2
done

# 创建 ILM 策略
echo ""
echo "创建索引生命周期管理策略..."
curl -X PUT "http://$ES_HOST/_ilm/policy/logs-policy" \
  -H 'Content-Type: application/json' \
  -d '{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
          },
          "set_priority": {
            "priority": 100
          }
        }
      },
      "warm": {
        "min_age": "3d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          },
          "set_priority": {
            "priority": 50
          }
        }
      },
      "delete": {
        "min_age": "7d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}'

echo ""
echo "✓ ILM 策略创建成功"

# 创建索引模板
echo ""
echo "创建日志索引模板..."
curl -X PUT "http://$ES_HOST/_index_template/logs-template" \
  -H 'Content-Type: application/json' \
  -d '{
  "index_patterns": ["logs-*"],
  "template": {
    "settings": {
      "number_of_shards": 1,
      "number_of_replicas": 0,
      "index.refresh_interval": "5s",
      "index.lifecycle.name": "logs-policy",
      "index.lifecycle.rollover_alias": "logs"
    },
    "mappings": {
      "properties": {
        "@timestamp": {
          "type": "date"
        },
        "timestamp": {
          "type": "date"
        },
        "level": {
          "type": "keyword"
        },
        "logger": {
          "type": "keyword"
        },
        "thread": {
          "type": "keyword"
        },
        "message": {
          "type": "text",
          "fields": {
            "keyword": {
              "type": "keyword",
              "ignore_above": 256
            }
          }
        },
        "service": {
          "type": "keyword"
        },
        "type": {
          "type": "keyword"
        },
        "trace_id": {
          "type": "keyword"
        },
        "span_id": {
          "type": "keyword"
        },
        "user_id": {
          "type": "keyword"
        },
        "request_id": {
          "type": "keyword"
        },
        "document_id": {
          "type": "keyword"
        },
        "session_id": {
          "type": "keyword"
        },
        "exception": {
          "properties": {
            "class": {
              "type": "keyword"
            },
            "message": {
              "type": "text"
            },
            "stacktrace": {
              "type": "text"
            }
          }
        },
        "host": {
          "properties": {
            "name": {
              "type": "keyword"
            },
            "hostname": {
              "type": "keyword"
            }
          }
        },
        "container": {
          "properties": {
            "id": {
              "type": "keyword"
            },
            "name": {
              "type": "keyword"
            },
            "image": {
              "type": "keyword"
            }
          }
        }
      }
    }
  },
  "priority": 500,
  "version": 1
}'

echo ""
echo "✓ 索引模板创建成功"

# 创建初始索引
echo ""
echo "创建初始索引..."
curl -X PUT "http://$ES_HOST/logs-000001" \
  -H 'Content-Type: application/json' \
  -d '{
  "aliases": {
    "logs": {
      "is_write_index": true
    }
  }
}'

echo ""
echo "✓ 初始索引创建成功"

# 验证配置
echo ""
echo "验证配置..."
echo "ILM 策略:"
curl -s "http://$ES_HOST/_ilm/policy/logs-policy" | jq .

echo ""
echo "索引模板:"
curl -s "http://$ES_HOST/_index_template/logs-template" | jq .

echo ""
echo "========================================="
echo "Elasticsearch 初始化完成"
echo "========================================="
echo ""
echo "访问 Elasticsearch: http://$ES_HOST"
echo "查看索引: http://$ES_HOST/_cat/indices?v"
echo "查看 ILM 策略: http://$ES_HOST/_ilm/policy"
echo "========================================="
