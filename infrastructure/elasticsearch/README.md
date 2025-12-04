# Elasticsearch 日志聚合配置指南

## 概述

Elasticsearch 是系统的日志存储和搜索引擎，配合 Filebeat 或 Fluentd 实现日志聚合，提供强大的日志查询和分析能力。

## 访问 Elasticsearch

- **URL**: http://localhost:9200
- **Kibana URL**: http://localhost:5601 (如果部署了 Kibana)
- **无需认证** (开发环境)

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      微服务应用层                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Java 微服务                                         │   │
│  │  - 日志输出到文件 (logback)                         │   │
│  │  - 日志格式: JSON                                    │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Python 微服务                                       │   │
│  │  - 日志输出到文件 (logging)                         │   │
│  │  - 日志格式: JSON                                    │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ 日志文件
                            ▼
                    ┌───────────────┐
                    │   Filebeat    │
                    │ (日志收集器)  │
                    └───────────────┘
                            │
                            │ Beats Protocol
                            ▼
                    ┌───────────────┐
                    │ Elasticsearch │
                    │  (日志存储)   │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │    Kibana     │
                    │  (日志查询)   │
                    └───────────────┘
```

## Elasticsearch 配置

### 索引模板

为日志数据创建索引模板，定义字段映射和索引设置：

```json
{
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
        "@timestamp": {"type": "date"},
        "level": {"type": "keyword"},
        "logger": {"type": "keyword"},
        "thread": {"type": "keyword"},
        "message": {"type": "text"},
        "service": {"type": "keyword"},
        "trace_id": {"type": "keyword"},
        "span_id": {"type": "keyword"},
        "user_id": {"type": "keyword"},
        "request_id": {"type": "keyword"},
        "exception": {
          "properties": {
            "class": {"type": "keyword"},
            "message": {"type": "text"},
            "stacktrace": {"type": "text"}
          }
        }
      }
    }
  }
}
```

### 索引生命周期管理 (ILM)

配置自动删除旧日志：

```json
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
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
}
```

## Filebeat 配置

### 安装 Filebeat

```bash
# Docker 方式
docker run -d \
  --name filebeat \
  --user=root \
  --volume="$(pwd)/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro" \
  --volume="/var/lib/docker/containers:/var/lib/docker/containers:ro" \
  --volume="/var/run/docker.sock:/var/run/docker.sock:ro" \
  docker.elastic.co/beats/filebeat:8.11.0
```

### filebeat.yml 配置

```yaml
filebeat.inputs:
  # Java 服务日志
  - type: log
    enabled: true
    paths:
      - /var/log/java-services/*/*.log
    json.keys_under_root: true
    json.add_error_key: true
    fields:
      type: java-service
    fields_under_root: true
    multiline.pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}'
    multiline.negate: true
    multiline.match: after

  # Python 服务日志
  - type: log
    enabled: true
    paths:
      - /var/log/python-services/*/*.log
    json.keys_under_root: true
    json.add_error_key: true
    fields:
      type: python-service
    fields_under_root: true

  # Docker 容器日志
  - type: container
    enabled: true
    paths:
      - '/var/lib/docker/containers/*/*.log'
    processors:
      - add_docker_metadata:
          host: "unix:///var/run/docker.sock"

# 输出到 Elasticsearch
output.elasticsearch:
  hosts: ["elasticsearch:9200"]
  index: "logs-%{[agent.version]}-%{+yyyy.MM.dd}"
  
# Kibana 配置
setup.kibana:
  host: "kibana:5601"

# 日志级别
logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
  permissions: 0644

# 处理器
processors:
  - add_host_metadata: ~
  - add_cloud_metadata: ~
  - add_docker_metadata: ~
```

## Fluentd 配置 (可选)

作为 Filebeat 的替代方案，Fluentd 提供更灵活的日志处理能力。

### fluent.conf 配置

```conf
<source>
  @type tail
  path /var/log/java-services/*/*.log
  pos_file /var/log/fluentd/java-services.pos
  tag java.service
  <parse>
    @type json
    time_key timestamp
    time_format %Y-%m-%dT%H:%M:%S.%L%z
  </parse>
</source>

<source>
  @type tail
  path /var/log/python-services/*/*.log
  pos_file /var/log/fluentd/python-services.pos
  tag python.service
  <parse>
    @type json
    time_key timestamp
    time_format %Y-%m-%dT%H:%M:%S.%L%z
  </parse>
</source>

<filter **>
  @type record_transformer
  <record>
    hostname "#{Socket.gethostname}"
    environment "#{ENV['ENVIRONMENT'] || 'development'}"
  </record>
</filter>

<match **>
  @type elasticsearch
  host elasticsearch
  port 9200
  logstash_format true
  logstash_prefix logs
  include_tag_key true
  type_name _doc
  <buffer>
    @type file
    path /var/log/fluentd/buffer
    flush_interval 5s
    retry_max_times 3
  </buffer>
</match>
```

## 日志格式规范

### Java 服务日志格式 (Logback JSON)

```json
{
  "@timestamp": "2024-12-04T10:30:45.123+08:00",
  "level": "INFO",
  "logger": "com.rag.ops.document.service.DocumentService",
  "thread": "http-nio-8081-exec-1",
  "message": "Document uploaded successfully",
  "service": "document-service",
  "trace_id": "1234567890abcdef",
  "span_id": "abcdef1234567890",
  "user_id": "user123",
  "request_id": "req-uuid-123",
  "document_id": "doc-uuid-456",
  "file_name": "manual.pdf",
  "file_size": 1048576
}
```

### Python 服务日志格式

```json
{
  "timestamp": "2024-12-04T10:30:45.123+08:00",
  "level": "INFO",
  "logger": "document_processing_service",
  "message": "Document processed successfully",
  "service": "document-processing-service",
  "trace_id": "1234567890abcdef",
  "span_id": "abcdef1234567890",
  "document_id": "doc-uuid-456",
  "chunk_count": 25,
  "processing_time": 3.45
}
```

### 错误日志格式

```json
{
  "@timestamp": "2024-12-04T10:30:45.123+08:00",
  "level": "ERROR",
  "logger": "com.rag.ops.document.service.DocumentService",
  "thread": "http-nio-8081-exec-1",
  "message": "Failed to process document",
  "service": "document-service",
  "trace_id": "1234567890abcdef",
  "span_id": "abcdef1234567890",
  "exception": {
    "class": "java.io.IOException",
    "message": "File not found",
    "stacktrace": "java.io.IOException: File not found\n\tat ..."
  },
  "document_id": "doc-uuid-456"
}
```

## Kibana 配置

### 安装 Kibana

```yaml
# docker-compose.yml
kibana:
  image: docker.elastic.co/kibana/kibana:8.11.0
  container_name: kibana
  ports:
    - "5601:5601"
  environment:
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
  networks:
    - rag-network
  depends_on:
    - elasticsearch
```

### 创建索引模式

1. 访问 http://localhost:5601
2. 进入 "Stack Management" -> "Index Patterns"
3. 点击 "Create index pattern"
4. 输入索引模式: `logs-*`
5. 选择时间字段: `@timestamp`
6. 点击 "Create index pattern"

### 创建日志查询视图

#### 1. Discover 页面

在 Discover 页面可以：
- 搜索和过滤日志
- 查看日志详情
- 保存搜索查询
- 创建可视化

#### 2. 常用查询

**按服务查询**:
```
service: "document-service"
```

**按日志级别查询**:
```
level: "ERROR"
```

**按 Trace ID 查询**:
```
trace_id: "1234567890abcdef"
```

**按时间范围查询**:
```
@timestamp: [now-1h TO now]
```

**组合查询**:
```
service: "document-service" AND level: "ERROR" AND @timestamp: [now-1h TO now]
```

#### 3. 创建可视化

**错误日志趋势图**:
- 类型: Line Chart
- Y 轴: Count
- X 轴: @timestamp
- 过滤: level: "ERROR"

**服务日志分布饼图**:
- 类型: Pie Chart
- Slice by: service.keyword
- Metric: Count

**Top 错误日志表格**:
- 类型: Data Table
- Rows: exception.class.keyword
- Metric: Count
- 排序: Count DESC

#### 4. 创建仪表盘

1. 进入 "Dashboard"
2. 点击 "Create dashboard"
3. 添加已创建的可视化
4. 调整布局
5. 保存仪表盘

## 日志查询示例

### Elasticsearch Query DSL

**查询最近 1 小时的错误日志**:
```json
GET /logs-*/_search
{
  "query": {
    "bool": {
      "must": [
        {"term": {"level": "ERROR"}},
        {"range": {
          "@timestamp": {
            "gte": "now-1h",
            "lte": "now"
          }
        }}
      ]
    }
  },
  "sort": [{"@timestamp": "desc"}],
  "size": 100
}
```

**按服务聚合日志数量**:
```json
GET /logs-*/_search
{
  "size": 0,
  "aggs": {
    "services": {
      "terms": {
        "field": "service.keyword",
        "size": 20
      }
    }
  }
}
```

**查询特定 Trace 的所有日志**:
```json
GET /logs-*/_search
{
  "query": {
    "term": {"trace_id": "1234567890abcdef"}
  },
  "sort": [{"@timestamp": "asc"}]
}
```

## 日志保留策略

### 索引生命周期管理

```bash
# 创建 ILM 策略
PUT _ilm/policy/logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "1d"
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
}
```

### 手动删除旧索引

```bash
# 删除 7 天前的索引
curl -X DELETE "http://localhost:9200/logs-$(date -d '7 days ago' +%Y.%m.%d)"
```

## 性能优化

### 1. 索引优化

- 使用合适的分片数量 (单个索引 1-2 个分片)
- 禁用不需要的副本 (开发环境)
- 配置合适的刷新间隔 (5-30 秒)

### 2. 查询优化

- 使用时间范围过滤
- 使用 term 查询而不是 match 查询
- 限制返回结果数量
- 使用聚合而不是大量查询

### 3. 存储优化

- 配置索引生命周期管理
- 定期删除旧数据
- 使用压缩
- 优化字段映射

## 故障排查

### 问题：日志未收集

**可能原因**:
- Filebeat 未启动
- 日志文件路径配置错误
- 文件权限问题
- Elasticsearch 连接失败

**排查步骤**:
1. 检查 Filebeat 状态
2. 检查 Filebeat 日志
3. 检查文件路径和权限
4. 测试 Elasticsearch 连接

### 问题：日志查询慢

**可能原因**:
- 查询范围过大
- 索引数量过多
- 字段映射不合理
- 资源不足

**解决方案**:
1. 缩小查询时间范围
2. 使用索引模式过滤
3. 优化字段映射
4. 增加 Elasticsearch 资源

### 问题：磁盘空间不足

**解决方案**:
1. 配置索引生命周期管理
2. 减少日志保留时间
3. 删除旧索引
4. 增加磁盘空间

## 最佳实践

### 1. 日志格式

- 使用结构化日志 (JSON)
- 包含关键字段 (timestamp, level, service, trace_id)
- 统一时间格式 (ISO 8601)
- 避免敏感信息

### 2. 日志级别

- ERROR: 错误和异常
- WARN: 警告信息
- INFO: 重要业务事件
- DEBUG: 调试信息 (生产环境禁用)

### 3. 日志内容

- 包含足够的上下文信息
- 使用有意义的消息
- 记录关键业务指标
- 记录性能数据

### 4. 日志管理

- 定期审查日志
- 配置告警规则
- 优化日志查询
- 控制日志量

## 参考资料

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Filebeat 官方文档](https://www.elastic.co/guide/en/beats/filebeat/current/index.html)
- [Kibana 官方文档](https://www.elastic.co/guide/en/kibana/current/index.html)
- [Fluentd 官方文档](https://docs.fluentd.org/)
