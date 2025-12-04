# Zipkin 链路追踪配置指南

## 概述

Zipkin 是系统的分布式链路追踪组件，用于追踪请求在微服务间的调用链路，帮助分析性能瓶颈和故障定位。

## 访问 Zipkin

- **URL**: http://localhost:9411
- **无需认证**

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      微服务应用层                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Java 微服务 (Spring Cloud Sleuth)                  │   │
│  │  - Gateway Service                                   │   │
│  │  - Document Service                                  │   │
│  │  - Session Service                                   │   │
│  │  - Auth Service                                      │   │
│  │  - Monitor Service                                   │   │
│  │  - Config Service                                    │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Python 微服务 (py_zipkin)                          │   │
│  │  - Document Processing Service                       │   │
│  │  - RAG Query Service                                 │   │
│  │  - Embedding Service                                 │   │
│  │  - LLM Service                                       │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ HTTP POST /api/v2/spans
                            ▼
                    ┌───────────────┐
                    │ Zipkin Server │
                    │  (Collector)  │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ Elasticsearch │
                    │   (Storage)   │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  Zipkin UI    │
                    │  (Query)      │
                    └───────────────┘
```

## 链路追踪配置

### Java 服务配置

Java 服务使用 Spring Cloud Sleuth 自动集成 Zipkin：

#### 依赖配置 (pom.xml)
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

#### 应用配置 (application.yml)
```yaml
spring:
  sleuth:
    sampler:
      probability: 1.0  # 采样率 (0.0-1.0)
  zipkin:
    base-url: http://zipkin:9411
    sender:
      type: web  # 使用 HTTP 发送
```

### Python 服务配置

Python 服务需要手动集成 Zipkin 追踪：

#### 安装依赖
```bash
pip install py-zipkin requests
```

#### 代码集成示例
```python
from py_zipkin.zipkin import zipkin_span, ZipkinAttrs
import requests

def http_transport(encoded_span):
    """发送 span 到 Zipkin"""
    requests.post(
        'http://zipkin:9411/api/v2/spans',
        data=encoded_span,
        headers={'Content-Type': 'application/json'}
    )

@zipkin_span(
    service_name='document-processing-service',
    span_name='process_document',
    transport_handler=http_transport,
    sample_rate=100.0  # 采样率 100%
)
def process_document(document_id):
    # 业务逻辑
    pass
```

## 采样策略

### 采样率配置

采样率决定了多少比例的请求会被追踪：

- **开发环境**: 100% (1.0) - 追踪所有请求
- **测试环境**: 50% (0.5) - 追踪一半请求
- **生产环境**: 10% (0.1) - 追踪 10% 请求

### 动态采样

可以根据请求特征动态调整采样率：

```java
@Bean
public Sampler defaultSampler() {
    return new Sampler() {
        @Override
        public boolean isSampled(long traceId) {
            // 自定义采样逻辑
            // 例如：错误请求 100% 采样，正常请求 10% 采样
            return true;
        }
    };
}
```

## Trace 数据结构

### Trace
一次完整的请求调用链，包含多个 Span。

### Span
调用链中的一个操作单元，包含以下信息：
- **Trace ID**: 全局唯一的追踪 ID
- **Span ID**: 当前 Span 的唯一 ID
- **Parent Span ID**: 父 Span 的 ID
- **Service Name**: 服务名称
- **Span Name**: 操作名称
- **Timestamp**: 开始时间戳
- **Duration**: 持续时间
- **Tags**: 标签 (key-value)
- **Annotations**: 事件标记

### 示例 Trace
```
Trace ID: 1234567890abcdef
├─ Span: gateway-service [GET /api/v1/query]
│  ├─ Span: session-service [POST /api/v1/sessions/query]
│  │  ├─ Span: rag-query-service [POST /api/query]
│  │  │  ├─ Span: embedding-service [POST /api/embeddings]
│  │  │  └─ Span: llm-service [POST /api/generate]
│  │  └─ Span: postgres [INSERT INTO messages]
│  └─ Span: redis [GET session:xxx]
```

## 使用指南

### 1. 查看 Trace 列表

1. 访问 http://localhost:9411
2. 在搜索框中输入查询条件：
   - Service Name: 服务名称
   - Span Name: 操作名称
   - Tags: 标签过滤
   - Duration: 持续时间范围
3. 点击 "Run Query" 查询
4. 查看 Trace 列表

### 2. 查看 Trace 详情

1. 点击 Trace 列表中的某个 Trace
2. 查看完整的调用链路图
3. 查看每个 Span 的详细信息：
   - 服务名称和操作名称
   - 开始时间和持续时间
   - 标签和注解
   - 错误信息（如果有）

### 3. 分析性能瓶颈

1. 在 Trace 详情页面查看时间线
2. 识别耗时最长的 Span
3. 查看 Span 的标签和注解
4. 分析性能瓶颈原因

### 4. 依赖关系分析

1. 点击顶部菜单 "Dependencies"
2. 查看服务间的依赖关系图
3. 分析服务调用频率和错误率

## 常见追踪场景

### 1. 文档上传流程追踪

```
Gateway Service
  └─> Document Service
      ├─> MinIO (文件存储)
      ├─> PostgreSQL (元数据保存)
      ├─> RabbitMQ (发送消息)
      └─> Document Processing Service
          ├─> 文档解析
          ├─> 文本分块
          ├─> Embedding Service (向量生成)
          └─> ChromaDB (向量存储)
```

### 2. RAG 查询流程追踪

```
Gateway Service
  └─> Session Service
      ├─> PostgreSQL (获取会话历史)
      └─> RAG Query Service
          ├─> Embedding Service (查询向量化)
          ├─> ChromaDB (向量检索)
          ├─> LLM Service (答案生成)
          │   └��> OpenAI API
          └─> Redis (结果缓存)
```

### 3. 认证流程追踪

```
Gateway Service
  └─> Auth Service
      ├─> PostgreSQL (用户查询)
      ├─> Redis (Token 缓存)
      └─> JWT 生成
```

## 标签和注解

### 标准标签

- `http.method`: HTTP 方法
- `http.path`: 请求路径
- `http.status_code`: HTTP 状态码
- `error`: 错误标记
- `component`: 组件类型 (database, cache, http)

### 自定义标签

```java
// Java
Span span = tracer.currentSpan();
span.tag("user.id", userId);
span.tag("document.id", documentId);
span.tag("query.text", queryText);
```

```python
# Python
from py_zipkin.zipkin import zipkin_span

with zipkin_span(
    service_name='rag-query-service',
    span_name='query',
    binary_annotations={
        'user.id': user_id,
        'query.text': query_text,
        'top_k': str(top_k)
    }
):
    # 业务逻辑
    pass
```

### 注解

```java
// Java
Span span = tracer.currentSpan();
span.annotate("cache.hit");
span.annotate("db.query.start");
span.annotate("db.query.end");
```

## 性能优化

### 1. 采样率优化

- 生产环境使用较低的采样率 (5-10%)
- 对关键路径使用更高的采样率
- 对错误请求使用 100% 采样

### 2. 异步发送

使用异步方式发送 Span 数据，避免阻塞业务逻辑：

```yaml
spring:
  zipkin:
    sender:
      type: web
    service:
      name: ${spring.application.name}
    base-url: http://zipkin:9411
```

### 3. 批量发送

配置批量发送 Span 数据，减少网络开销：

```yaml
spring:
  sleuth:
    async:
      enabled: true
    scheduled:
      enabled: true
```

### 4. 存储优化

使用 Elasticsearch 作为存储后端，提供更好的查询性能：

```yaml
# Zipkin Server 配置
STORAGE_TYPE: elasticsearch
ES_HOSTS: http://elasticsearch:9200
ES_INDEX: zipkin
```

## 故障排查

### 问题：Trace 数据未显示

**可能原因**:
- Zipkin Server 未启动
- 服务未正确配置 Zipkin URL
- 采样率设置为 0
- 网络连接问题

**排查步骤**:
1. 检查 Zipkin Server 状态: `curl http://localhost:9411/health`
2. 检查服务日志中的 Zipkin 相关信息
3. 检查采样率配置
4. 测试网络连接: `curl http://zipkin:9411/api/v2/services`

### 问题：Trace 数据不完整

**可能原因**:
- 部分服务未集成 Zipkin
- Trace ID 传递中断
- 异步调用未正确传递上下文

**排查步骤**:
1. 检查所有服务是否都集成了 Zipkin
2. 检查 HTTP Header 中的 Trace ID 传递
3. 检查异步调用的上下文传递

### 问题：Zipkin 性能问题

**可能原因**:
- 采样率过高
- Elasticsearch 性能不足
- 数据量过大

**解决方案**:
1. 降低采样率
2. 优化 Elasticsearch 配置
3. 配置数据保留策略
4. 使用 Zipkin 集群

## 数据保留策略

### Elasticsearch 索引管理

配置自动删除旧数据：

```bash
# 删除 7 天前的索引
curl -X DELETE "http://localhost:9200/zipkin-span-$(date -d '7 days ago' +%Y-%m-%d)"
```

### 定期清理脚本

```bash
#!/bin/bash
# cleanup-zipkin-data.sh

# 保留最近 7 天的数据
RETENTION_DAYS=7

# 获取要删除的索引
INDICES=$(curl -s "http://localhost:9200/_cat/indices/zipkin-*?h=index" | \
  awk -v date="$(date -d "$RETENTION_DAYS days ago" +%Y-%m-%d)" '$0 < "zipkin-span-"date')

# 删除旧索引
for index in $INDICES; do
  echo "Deleting index: $index"
  curl -X DELETE "http://localhost:9200/$index"
done
```

## 最佳实践

### 1. Span 命名

- 使用清晰、一致的命名规范
- 包含操作类型和资源名称
- 例如: `GET /api/v1/documents`, `process_document`, `query_vectors`

### 2. 标签使用

- 添加有意义的标签帮助过滤和分析
- 避免高基数标签 (如 UUID)
- 使用标准标签名称

### 3. 错误追踪

- 在 Span 中标记错误信息
- 添加错误堆栈跟踪
- 使用 `error` 标签

```java
try {
    // 业务逻辑
} catch (Exception e) {
    Span span = tracer.currentSpan();
    span.tag("error", "true");
    span.tag("error.message", e.getMessage());
    span.tag("error.stack", Arrays.toString(e.getStackTrace()));
    throw e;
}
```

### 4. 性能监控

- 定期分析慢 Trace
- 识别性能瓶颈
- 优化关键路径

### 5. 采样策略

- 根据环境调整采样率
- 对关键业务使用更高采样率
- 对错误请求使用 100% 采样

## 集成其他工具

### 与 Prometheus 集成

通过 Zipkin 的 metrics 端点暴露指标：

```yaml
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus,health,info
```

### 与 Grafana 集成

在 Grafana 中添加 Zipkin 数据源，可视化 Trace 数据。

### 与告警系统集成

配置告警规则，当检测到异常 Trace 时发送告警。

## 参考资料

- [Zipkin 官方文档](https://zipkin.io/)
- [Spring Cloud Sleuth 文档](https://spring.io/projects/spring-cloud-sleuth)
- [py-zipkin 文档](https://github.com/Yelp/py_zipkin)
- [分布式追踪最佳实践](https://opentracing.io/docs/best-practices/)
