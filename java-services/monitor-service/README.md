# Monitor Service

监控日志服务 - 负责收集、存储和查询系统日志和性能指标

## 功能特性

- **日志收集**: 接收来自其他服务的操作日志
- **日志存储**: 双存储架构（PostgreSQL + Elasticsearch）
- **日志查询**: 支持多维度过滤和全文搜索
- **性能指标**: 收集和统计系统性能指标
- **系统统计**: 提供系统运行状态统计信息
- **链路追踪**: 支持分布式链路追踪
- **Prometheus 集成**: 暴露 Prometheus 指标端点

## 技术栈

- Spring Boot 3.x
- Spring Data JPA (PostgreSQL)
- Spring Data Elasticsearch
- Nacos Discovery & Config
- Sentinel (流控、熔断)
- Zipkin (链路追踪)
- Prometheus (指标采集)

## API 端点

### 日志管理

#### 收集日志
```bash
POST /api/v1/logs
Content-Type: application/json

{
  "operationType": "DOCUMENT_UPLOAD",
  "serviceName": "document-service",
  "userId": "user123",
  "resourceId": "doc-456",
  "resourceType": "document",
  "action": "upload",
  "status": "SUCCESS",
  "ipAddress": "192.168.1.100",
  "durationMs": 1500,
  "traceId": "trace-123",
  "details": {
    "fileName": "test.pdf",
    "fileSize": 1024000
  }
}
```

#### 查询日志
```bash
GET /api/v1/logs?operationType=DOCUMENT_UPLOAD&serviceName=document-service&page=0&size=20
GET /api/v1/logs?startTime=2024-01-01T00:00:00&endTime=2024-01-31T23:59:59
GET /api/v1/logs?userId=user123&status=ERROR
```

#### 搜索日志（全文搜索）
```bash
GET /api/v1/logs/search?keyword=error&page=0&size=20
```

#### 根据 Trace ID 查询日志
```bash
GET /api/v1/logs/trace/{traceId}
```

### 性能指标

#### 收集指标
```bash
POST /api/v1/metrics
Content-Type: application/json

{
  "metricType": "response_time",
  "serviceName": "document-service",
  "metricName": "api_response_time",
  "metricValue": 150.5,
  "unit": "ms",
  "tags": "endpoint=/api/v1/documents",
  "metadata": {
    "method": "POST",
    "statusCode": 200
  }
}
```

#### 查询指标
```bash
GET /api/v1/metrics?metricType=response_time&serviceName=document-service&page=0&size=20
GET /api/v1/metrics?metricName=api_response_time&startTime=2024-01-01T00:00:00
```

### 系统统计

#### 获取系统统计信息
```bash
GET /api/v1/stats
GET /api/v1/stats?startTime=2024-01-01T00:00:00&endTime=2024-01-31T23:59:59
```

响应示例：
```json
{
  "totalLogs": 10000,
  "totalMetrics": 50000,
  "logsByType": {
    "DOCUMENT_UPLOAD": 3000,
    "QUERY": 5000,
    "AUTH": 2000
  },
  "logsByStatus": {
    "SUCCESS": 9500,
    "ERROR": 500
  },
  "avgResponseTime": 150.5,
  "maxResponseTime": 3000.0,
  "errorCount": 500,
  "errorRate": 5.0
}
```

### 健康检查

```bash
GET /api/health
```

### Prometheus 指标

```bash
GET /actuator/prometheus
```

## 配置说明

### 环境变量

```bash
# 服务端口
SERVER_PORT=8084

# 数据库配置
DATABASE_URL=jdbc:postgresql://localhost:5432/rag_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# Elasticsearch 配置
ELASTICSEARCH_URL=http://localhost:9200
ELASTICSEARCH_USERNAME=
ELASTICSEARCH_PASSWORD=

# Nacos 配置
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=rag-system

# Sentinel 配置
SENTINEL_DASHBOARD=localhost:8858

# Zipkin 配置
ZIPKIN_URL=http://localhost:9411

# 采样率
SLEUTH_SAMPLE_RATE=1.0
```

### Nacos 配置

在 Nacos 中创建配置文件 `monitor-service.yaml`:

```yaml
app:
  log:
    retention-days: 30  # 日志保留天数
  metric:
    retention-days: 7   # 指标保留天数
```

### Sentinel 规则

在 Nacos 中创建 Sentinel 规则配置：

**流控规则** (`monitor-service-flow-rules`):
```json
[
  {
    "resource": "collectLog",
    "limitApp": "default",
    "grade": 1,
    "count": 500,
    "strategy": 0,
    "controlBehavior": 0
  },
  {
    "resource": "collectMetric",
    "limitApp": "default",
    "grade": 1,
    "count": 1000,
    "strategy": 0,
    "controlBehavior": 0
  }
]
```

## 本地开发

### 前置条件

- JDK 17+
- Maven 3.6+
- PostgreSQL 15+
- Elasticsearch 8.x
- Nacos Server
- Sentinel Dashboard

### 启动步骤

1. 启动基础设施服务（PostgreSQL, Elasticsearch, Nacos）

2. 创建数据库
```sql
CREATE DATABASE rag_db;
```

3. 启动服务
```bash
cd java-services/monitor-service
mvn spring-boot:run
```

4. 验证服务
```bash
curl http://localhost:8084/api/health
```

## Docker 部署

### 构建镜像

```bash
docker build -t monitor-service:latest -f java-services/monitor-service/Dockerfile .
```

### 运行容器

```bash
docker run -d \
  --name monitor-service \
  -p 8084:8084 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/rag_db \
  -e ELASTICSEARCH_URL=http://elasticsearch:9200 \
  -e NACOS_SERVER=nacos:8848 \
  -e SENTINEL_DASHBOARD=sentinel-dashboard:8858 \
  -e ZIPKIN_URL=http://zipkin:9411 \
  monitor-service:latest
```

## 监控和运维

### 查看日志

```bash
# Docker 容器日志
docker logs -f monitor-service

# 应用日志文件
tail -f logs/monitor-service.log
```

### Prometheus 指标

访问 `http://localhost:8084/actuator/prometheus` 查看所有可用指标

关键指标：
- `http_server_requests_seconds`: HTTP 请求响应时间
- `jvm_memory_used_bytes`: JVM 内存使用
- `system_cpu_usage`: CPU 使用率
- `logback_events_total`: 日志事件总数

### Sentinel 监控

访问 Sentinel Dashboard: `http://localhost:8858`

查看：
- 实时流量监控
- 流控规则配置
- 熔断降级状态

### Zipkin 链路追踪

访问 Zipkin UI: `http://localhost:9411`

查看分布式调用链路

## 数据清理

服务会自动清理过期数据：
- 操作日志保留 30 天
- 性能指标保留 7 天

可以通过配置调整保留时间：
```yaml
app:
  log:
    retention-days: 30
  metric:
    retention-days: 7
```

## 故障排查

### Elasticsearch 连接失败

检查 Elasticsearch 是否运行：
```bash
curl http://localhost:9200
```

检查配置：
```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
```

### 数据库连接失败

检查 PostgreSQL 连接：
```bash
psql -h localhost -U postgres -d rag_db
```

检查配置：
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/rag_db
```

### Nacos 注册失败

检查 Nacos 服务：
```bash
curl http://localhost:8848/nacos/
```

检查配置：
```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
```

## 性能优化

### 数据库优化

- 为常用查询字段添加索引
- 使用连接池管理数据库连接
- 定期清理过期数据

### Elasticsearch 优化

- 配置合适的分片数量
- 使用批量索引提高写入性能
- 定期优化索引

### 异步处理

- 日志和指标收集使用异步处理
- 配置合适的线程池大小

## 许可证

Copyright © 2024 RAG Ops Team
