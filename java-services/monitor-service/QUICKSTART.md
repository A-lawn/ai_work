# Monitor Service Quick Start Guide

## 快速启动指南

### 前置条件

确保以下服务已启动：
- PostgreSQL (端口 5432)
- Nacos Server (端口 8848)
- Sentinel Dashboard (端口 8858，可选)
- Elasticsearch (端口 9200，可选)
- Zipkin (端口 9411，可选)

### 1. 启动基础设施

使用 Docker Compose 启动基础设施：

```bash
# 从项目根目录
cd infrastructure
docker-compose up -d postgres nacos sentinel-dashboard elasticsearch zipkin
```

等待服务启动完成（约 30 秒）。

### 2. 初始化数据库

数据库表会在服务启动时自动创建（使用 JPA DDL auto-update）。

如果需要手动创建，可以执行：

```sql
-- 连接到 PostgreSQL
psql -h localhost -U postgres -d rag_db

-- 表会自动创建，无需手动执行 SQL
```

### 3. 配置 Nacos

访问 Nacos 控制台: http://localhost:8848/nacos

登录（用户名/密码: nacos/nacos）

创建配置文件 `monitor-service.yaml`:

```yaml
app:
  log:
    retention-days: 30
  metric:
    retention-days: 7
```

### 4. 启动 Monitor Service

#### 方式 1: Maven 启动（开发环境）

```bash
cd java-services/monitor-service
mvn spring-boot:run
```

#### 方式 2: JAR 启动

```bash
cd java-services/monitor-service
mvn clean package -DskipTests
java -jar target/monitor-service-1.0.0.jar
```

#### 方式 3: Docker 启动

```bash
# 从项目根目录
docker build -t monitor-service:latest -f java-services/monitor-service/Dockerfile .
docker run -d \
  --name monitor-service \
  -p 8084:8084 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/rag_db \
  -e NACOS_SERVER=host.docker.internal:8848 \
  -e ELASTICSEARCH_URL=http://host.docker.internal:9200 \
  monitor-service:latest
```

### 5. 验证服务

#### 健康检查

```bash
curl http://localhost:8084/api/health
```

预期响应：
```json
{
  "status": "UP",
  "service": "monitor-service",
  "timestamp": 1234567890,
  "postgresql": "UP",
  "elasticsearch": "UP"
}
```

#### 运行验证脚本

```bash
cd java-services/monitor-service
chmod +x verify-monitor-service.sh
./verify-monitor-service.sh
```

### 6. 测试 API

#### 收集日志

```bash
curl -X POST http://localhost:8084/api/v1/logs \
  -H "Content-Type: application/json" \
  -d '{
    "operationType": "DOCUMENT_UPLOAD",
    "serviceName": "document-service",
    "userId": "user123",
    "resourceId": "doc-456",
    "action": "upload",
    "status": "SUCCESS",
    "durationMs": 1500
  }'
```

#### 查询日志

```bash
# 查询所有日志
curl "http://localhost:8084/api/v1/logs?page=0&size=10"

# 按操作类型查询
curl "http://localhost:8084/api/v1/logs?operationType=DOCUMENT_UPLOAD"

# 按服务名查询
curl "http://localhost:8084/api/v1/logs?serviceName=document-service"

# 按时间范围查询
curl "http://localhost:8084/api/v1/logs?startTime=2024-01-01T00:00:00&endTime=2024-12-31T23:59:59"
```

#### 收集性能指标

```bash
curl -X POST http://localhost:8084/api/v1/metrics \
  -H "Content-Type: application/json" \
  -d '{
    "metricType": "response_time",
    "serviceName": "document-service",
    "metricName": "api_response_time",
    "metricValue": 150.5,
    "unit": "ms"
  }'
```

#### 查询性能指标

```bash
curl "http://localhost:8084/api/v1/metrics?metricType=response_time&page=0&size=10"
```

#### 获取系统统计

```bash
curl "http://localhost:8084/api/v1/stats"
```

### 7. 查看监控指标

#### Prometheus 指标

```bash
curl http://localhost:8084/actuator/prometheus
```

#### Actuator 端点

```bash
# 健康检查
curl http://localhost:8084/actuator/health

# 所有指标
curl http://localhost:8084/actuator/metrics

# 特定指标
curl http://localhost:8084/actuator/metrics/jvm.memory.used
```

### 8. 查看 Sentinel 监控

访问 Sentinel Dashboard: http://localhost:8858

用户名/密码: sentinel/sentinel

在"簇点链路"中可以看到 Monitor Service 的流控规则。

### 9. 查看链路追踪

访问 Zipkin UI: http://localhost:9411

可以查看 Monitor Service 的调用链路。

### 10. 日志查看

#### 应用日志

```bash
# 实时查看
tail -f java-services/monitor-service/logs/monitor-service.log

# 查看最近 100 行
tail -n 100 java-services/monitor-service/logs/monitor-service.log
```

#### Docker 日志

```bash
docker logs -f monitor-service
```

## 常见问题

### Q1: 服务启动失败，提示数据库连接错误

**解决方案**:
1. 检查 PostgreSQL 是否运行: `docker ps | grep postgres`
2. 检查数据库连接配置: `DATABASE_URL`
3. 测试数据库连接: `psql -h localhost -U postgres -d rag_db`

### Q2: Elasticsearch 连接失败

**解决方案**:
1. Elasticsearch 是可选的，服务仍可正常运行
2. 检查 Elasticsearch 是否运行: `curl http://localhost:9200`
3. 如果不需要全文搜索，可以忽略此错误

### Q3: Nacos 注册失败

**解决方案**:
1. 检查 Nacos 是否运行: `curl http://localhost:8848/nacos/`
2. 检查配置: `NACOS_SERVER=localhost:8848`
3. 查看 Nacos 控制台的服务列表

### Q4: 端口冲突

**解决方案**:
修改端口配置:
```bash
export SERVER_PORT=8085
mvn spring-boot:run
```

### Q5: 内存不足

**解决方案**:
调整 JVM 参数:
```bash
java -Xmx512m -Xms256m -jar target/monitor-service-1.0.0.jar
```

## 性能调优

### 数据库连接池

在 `application.yml` 中调整：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### 异步线程池

在 `AsyncConfig` 中调整：

```java
executor.setCorePoolSize(10);
executor.setMaxPoolSize(50);
executor.setQueueCapacity(500);
```

### Sentinel 流控规则

在 Nacos 中调整流控规则：

```json
[
  {
    "resource": "collectLog",
    "count": 500
  }
]
```

## 下一步

1. 集成到 API Gateway
2. 配置其他服务调用 Monitor Service
3. 设置 Grafana 仪表盘
4. 配置告警规则

## 相关文档

- [README.md](README.md) - 完整文档
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - 实现总结
- [verify-monitor-service.sh](verify-monitor-service.sh) - 验证脚本

## 技术支持

如有问题，请查看：
1. 应用日志: `logs/monitor-service.log`
2. Nacos 控制台: http://localhost:8848/nacos
3. Sentinel Dashboard: http://localhost:8858
4. Actuator 健康检查: http://localhost:8084/actuator/health
