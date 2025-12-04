# Prometheus 监控配置指南

## 概述

Prometheus 是系统的核心监控组件，负责采集所有服务的指标数据并触发告警。

## 访问 Prometheus

- **URL**: http://localhost:9090
- **无需认证**

## 监控架构

```
┌─────────────────────────────────────────────────────────────┐
│                     Prometheus Server                        │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Scrape Targets (每 15 秒采集一次)                   │   │
│  │  - Java 微服务 (/actuator/prometheus)               │   │
│  │  - Python 微服务 (/metrics)                         │   │
│  │  - 基础设施服务                                      │   │
│  └──────────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Alerting Rules (每 30 秒评估一次)                  │   │
│  │  - 服务健康告警                                      │   │
│  │  - 性能告警                                          │   │
│  │  - Sentinel 告警                                     │   │
│  │  - 业务指标告警                                      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  Alertmanager │
                    │  (可选)       │
                    └───────────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ Monitor Service│
                    │  (告警接收)   │
                    └───────────────┘
```

## 监控指标

### Java 微服务指标

所有 Java 服务通过 Spring Boot Actuator 暴露 Prometheus 格式的指标：

#### JVM 指标
- `jvm_memory_used_bytes`: JVM 内存使用量
- `jvm_memory_max_bytes`: JVM 最大内存
- `jvm_gc_pause_seconds`: GC 暂停时间
- `jvm_threads_live`: 活跃线程数
- `process_cpu_usage`: CPU 使用率

#### HTTP 指标
- `http_server_requests_seconds`: HTTP 请求响应时间
- `http_server_requests_seconds_count`: HTTP 请求总数
- `http_server_requests_seconds_sum`: HTTP 请求总耗时

#### Sentinel 指标
- `sentinel_pass_qps`: 通过的 QPS
- `sentinel_block_qps`: 被限流的 QPS
- `sentinel_success_qps`: 成功的 QPS
- `sentinel_exception_qps`: 异常的 QPS
- `sentinel_rt`: 平均响应时间
- `sentinel_circuit_breaker_state`: 熔断器状态

#### 数据库连接池指标
- `hikaricp_connections_active`: 活跃连接数
- `hikaricp_connections_idle`: 空闲连接数
- `hikaricp_connections_pending`: 等待连接数

### Python 微服务指标

Python 服务使用 prometheus_client 库暴露指标：

#### 业务指标
- `document_processing_total`: 文档处理总数
- `document_processing_duration_seconds`: 文档处理耗时
- `document_processing_failed_total`: 文档处理失败数
- `rag_query_total`: RAG 查询总数
- `rag_query_duration_seconds`: RAG 查询耗时
- `embedding_generation_total`: 向量生成总数
- `embedding_generation_duration_seconds`: 向量生成耗时
- `llm_call_total`: LLM 调用总数
- `llm_call_duration_seconds`: LLM 调用耗时
- `llm_token_usage`: LLM Token 使用量

#### 系统指标
- `process_cpu_seconds_total`: CPU 使用时间
- `process_resident_memory_bytes`: 内存使用量
- `process_open_fds`: 打开的文件描述符数

### 基础设施指标

#### PostgreSQL
- `pg_stat_database_numbackends`: 连接数
- `pg_stat_database_xact_commit`: 事务提交数
- `pg_stat_database_xact_rollback`: 事务回滚数

#### Redis
- `redis_connected_clients`: 连接客户端数
- `redis_memory_used_bytes`: 内存使用量
- `redis_keyspace_hits_total`: 缓存命中数
- `redis_keyspace_misses_total`: 缓存未命中数

#### RabbitMQ
- `rabbitmq_queue_messages`: 队列消息数
- `rabbitmq_queue_messages_ready`: 待消费消息数
- `rabbitmq_queue_consumers`: 消费者数量

## 告警规则

### 服务健康告警

#### ServiceDown
- **触发条件**: 服务无法访问超过 1 分钟
- **严重级别**: critical
- **处理建议**: 立即检查服务状态，查看日志，重启服务

#### HighErrorRate
- **触发条件**: 5xx 错误率超过 5%，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 检查服务日志，分析错误原因

#### SlowResponse
- **触发条件**: P95 响应时间超过 3 秒，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 分析慢查询，优化性能，考虑扩容

### Sentinel 告警

#### SentinelBlocked
- **触发条件**: 限流触发超过 10 次/秒，持续 2 分钟
- **严重级别**: info
- **处理建议**: 检查流量是否正常，考虑调整限流阈值或扩容

#### SentinelCircuitBreakerOpen
- **触发条件**: 熔断器打开超过 1 分钟
- **严重级别**: warning
- **处理建议**: 检查下游服务状态，修复故障后熔断器会自动恢复

#### SentinelHighBlockRate
- **触发条件**: 请求拒绝率超过 10%，持续 3 分钟
- **严重级别**: warning
- **处理建议**: 分析流量模式，调整限流策略

### 资源告警

#### HighCPUUsage
- **触发条件**: CPU 使用率超过 80%，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 分析 CPU 热点，优化代码，考虑扩容

#### HighMemoryUsage
- **触发条件**: 内存使用率超过 90%，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 检查内存泄漏，调整 JVM 参数，考虑扩容

### 业务告警

#### HighDocumentProcessingFailureRate
- **触发条件**: 文档处理失败率超过 10%，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 检查文档格式，查看处理服务日志

#### SlowQueryResponse
- **触发条件**: RAG 查询 P95 响应时间超过 5 秒，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 优化向量检索，检查 LLM 服务性能

#### HighLLMFailureRate
- **触发条件**: LLM 调用失败率超过 5%，持续 5 分钟
- **严重级别**: warning
- **处理建议**: 检查 LLM API 配置，查看 API 限额

## 常用查询

### 服务健康检查
```promql
# 查看所有服务状态
up

# 查看特定服务状态
up{job="gateway-service"}

# 查看下线的服务
up == 0
```

### QPS 查询
```promql
# 查看服务 QPS
rate(http_server_requests_seconds_count[5m])

# 查看 Sentinel 通过的 QPS
rate(sentinel_pass_qps[5m])

# 查看 Sentinel 限流的 QPS
rate(sentinel_block_qps[5m])
```

### 响应时间查询
```promql
# 查看 P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 查看平均响应时间
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

### 错误率查询
```promql
# 查看 5xx 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])

# 查看 Sentinel 异常率
rate(sentinel_exception_qps[5m]) / rate(sentinel_pass_qps[5m])
```

### 资源使用查询
```promql
# 查看 CPU 使用率
process_cpu_usage

# 查看内存使用率
jvm_memory_used_bytes / jvm_memory_max_bytes

# 查看 GC 时间
rate(jvm_gc_pause_seconds_sum[5m])
```

### 业务指标查询
```promql
# 查看文档处理速率
rate(document_processing_total[5m])

# 查看 RAG 查询速率
rate(rag_query_total[5m])

# 查看 LLM Token 消耗
rate(llm_token_usage[5m])
```

## 配置文件说明

### prometheus.yml
主配置文件，定义了：
- 全局配置（采集间隔、评估间隔）
- 告警管理器配置
- 规则文件加载
- 抓取目标配置

### alerting_rules.yml
告警规则定义文件，包含：
- 服务健康告警
- Sentinel 告警
- 数据库告警
- 业务指标告警
- 基础设施告警

### alertmanager.yml
告警管理器配置（可选），定义了：
- 告警路由规则
- 告警接收器
- 告警抑制规则

## 使用指南

### 1. 启动 Prometheus

```bash
docker-compose up -d prometheus
```

### 2. 访问 Web UI

打开浏览器访问 http://localhost:9090

### 3. 查看监控指标

在 Prometheus UI 中：
1. 点击 "Graph" 标签
2. 在查询框中输入 PromQL 查询
3. 点击 "Execute" 执行查询
4. 查看图表或表格结果

### 4. 查看告警规则

1. 点击 "Alerts" 标签
2. 查看所有告警规则的状态
3. 绿色表示正常，红色表示触发告警

### 5. 查看抓取目标

1. 点击 "Status" -> "Targets"
2. 查看所有监控目标的状态
3. UP 表示正常，DOWN 表示无法访问

## 集成 Grafana

Prometheus 数据可以在 Grafana 中可视化：

1. Grafana 已自动配置 Prometheus 数据源
2. 访问 http://localhost:3001
3. 使用预配置的仪表盘查看监控数据

## 故障排查

### 问题：指标未采集
- 检查服务是否正常运行
- 检查服务的 metrics 端点是否可访问
- 查看 Prometheus 日志
- 检查 prometheus.yml 配置

### 问题：告警未触发
- 检查告警规则语法
- 查看 Prometheus UI 中的告警状态
- 检查 Alertmanager 配置
- 查看 Prometheus 日志

### 问题：数据丢失
- 检查 Prometheus 存储空间
- 检查数据保留策略
- 查看 Prometheus 日志

## 性能优化

### 采集优化
- 调整 scrape_interval 减少采集频率
- 使用 metric_relabel_configs 过滤不需要的指标
- 限制高基数标签

### 存储优化
- 配置合适的数据保留时间
- 使用远程存储（如 Thanos、Cortex）
- 定期清理旧数据

## 参考资料

- [Prometheus 官方文档](https://prometheus.io/docs/)
- [PromQL 查询语言](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [告警规则配置](https://prometheus.io/docs/prometheus/latest/configuration/alerting_rules/)
