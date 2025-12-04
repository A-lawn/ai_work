# Session Service 快速开始指南

## 前置条件

确保以下服务已启动：
- PostgreSQL (端口 5432)
- Redis (端口 6379)
- Nacos (端口 8848)
- Sentinel Dashboard (端口 8858，可选)
- Zipkin (端口 9411，可选)

## 快速启动

### 1. 使用 Maven 启动

```bash
cd java-services/session-service
mvn spring-boot:run
```

### 2. 使用 Docker 启动

```bash
# 构建镜像
docker build -t session-service:latest -f java-services/session-service/Dockerfile .

# 运行容器
docker run -d \
  --name session-service \
  -p 8082:8082 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/rag_db \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=postgres \
  -e REDIS_HOST=redis \
  -e NACOS_SERVER=nacos:8848 \
  session-service:latest
```

## 验证服务

### 健康检查

```bash
curl http://localhost:8082/api/health
```

预期响应：
```json
{
  "status": "UP",
  "service": "session-service",
  "database": "UP",
  "redis": "UP"
}
```

### 运行验证脚本

```bash
bash java-services/session-service/verify-session-service.sh
```

## API 测试示例

### 1. 创建会话

```bash
curl -X POST http://localhost:8082/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "metadata": "{}"
  }'
```

### 2. 添加消息

```bash
curl -X POST http://localhost:8082/api/v1/sessions/{session_id}/messages \
  -H "Content-Type: application/json" \
  -d '{
    "role": "USER",
    "content": "什么是微服务架构？"
  }'
```

### 3. 获取会话历史

```bash
curl http://localhost:8082/api/v1/sessions/{session_id}/history
```

### 4. 删除会话

```bash
curl -X DELETE http://localhost:8082/api/v1/sessions/{session_id}
```

## 配置说明

### 环境变量

| 变量名 | 默认值 | 说明 |
|--------|--------|------|
| DATABASE_URL | jdbc:postgresql://localhost:5432/rag_db | 数据库连接URL |
| DATABASE_USERNAME | postgres | 数据库用户名 |
| DATABASE_PASSWORD | postgres | 数据库密码 |
| REDIS_HOST | localhost | Redis主机地址 |
| REDIS_PORT | 6379 | Redis端口 |
| NACOS_SERVER | localhost:8848 | Nacos服务器地址 |
| SENTINEL_DASHBOARD | localhost:8858 | Sentinel控制台地址 |
| ZIPKIN_URL | http://localhost:9411 | Zipkin服务器地址 |

### 应用配置

主要配置在 `application.yml` 中，可以通过环境变量覆盖。

## 监控和管理

### Actuator 端点

- 健康检查: http://localhost:8082/api/health
- Prometheus指标: http://localhost:8082/actuator/prometheus
- 所有端点: http://localhost:8082/actuator

### Sentinel 控制台

访问 http://localhost:8858 查看流控和熔断规则。

### Nacos 控制台

访问 http://localhost:8848/nacos 查看服务注册和配置信息。

## 故障排查

### 服务无法启动

1. 检查PostgreSQL是否启动：
```bash
psql -h localhost -U postgres -d rag_db -c "SELECT 1"
```

2. 检查Redis是否启动：
```bash
redis-cli ping
```

3. 检查Nacos是否可访问：
```bash
curl http://localhost:8848/nacos/v1/console/health/readiness
```

### 查看日志

```bash
# Docker容器日志
docker logs -f session-service

# 本地日志文件
tail -f /tmp/session-service.log
```

## 下一步

- 查看 [README.md](README.md) 了解完整功能
- 查看 [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) 了解实现细节
- 集成到 Gateway Service 进行路由配置
