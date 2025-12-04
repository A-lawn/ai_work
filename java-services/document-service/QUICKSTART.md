# Document Service 快速开始指南

## 前置条件

- Java 17+
- Maven 3.9+
- Docker（用于运行依赖服务）

## 快速启动

### 1. 启动依赖服务

使用 Docker Compose 启动所有依赖服务：

```bash
# 在项目根目录
docker-compose up -d postgres redis minio rabbitmq nacos sentinel-dashboard zipkin
```

或者单独启动每个服务：

```bash
# PostgreSQL
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_DB=rag_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres123 \
  postgres:15

# Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# MinIO
docker run -d --name minio -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=admin \
  -e MINIO_ROOT_PASSWORD=admin123456 \
  minio/minio server /data --console-address ":9001"

# RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3.12-management

# Nacos
docker run -d --name nacos -p 8848:8848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.2.3

# Sentinel Dashboard
docker run -d --name sentinel -p 8858:8858 \
  bladex/sentinel-dashboard:latest

# Zipkin
docker run -d --name zipkin -p 9411:9411 \
  openzipkin/zipkin:latest
```

### 2. 验证依赖服务

```bash
# PostgreSQL
psql -h localhost -U postgres -d rag_db -c "SELECT 1"

# Redis
redis-cli ping

# MinIO
curl http://localhost:9000/minio/health/live

# RabbitMQ
curl http://localhost:15672/api/overview -u admin:admin

# Nacos
curl http://localhost:8848/nacos/v1/console/health/readiness

# Sentinel Dashboard
curl http://localhost:8858/

# Zipkin
curl http://localhost:9411/health
```

### 3. 配置环境变量（可选）

创建 `.env` 文件或设置环境变量：

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/rag_db
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres123
export REDIS_HOST=localhost
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=admin
export MINIO_SECRET_KEY=admin123456
export RABBITMQ_HOST=localhost
export NACOS_SERVER=localhost:8848
export SENTINEL_DASHBOARD=localhost:8858
export ZIPKIN_URL=http://localhost:9411
```

### 4. 启动 Document Service

#### 方式 1: 使用 Maven

```bash
cd java-services/document-service
mvn spring-boot:run
```

#### 方式 2: 构建并运行 JAR

```bash
cd java-services/document-service
mvn clean package -DskipTests
java -jar target/document-service-1.0.0.jar
```

#### 方式 3: 使用 Docker

```bash
# 在项目根目录
docker build -t document-service:latest -f java-services/document-service/Dockerfile .
docker run -d --name document-service -p 8081:8081 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/rag_db \
  -e REDIS_HOST=host.docker.internal \
  -e MINIO_ENDPOINT=http://host.docker.internal:9000 \
  -e RABBITMQ_HOST=host.docker.internal \
  -e NACOS_SERVER=host.docker.internal:8848 \
  document-service:latest
```

### 5. 验证服务启动

```bash
# 健康检查
curl http://localhost:8081/actuator/health

# 自定义健康检查
curl http://localhost:8081/api/v1/health

# Prometheus 指标
curl http://localhost:8081/actuator/prometheus
```

### 6. 测试 API

#### 创建测试文件

```bash
echo "这是一个测试文档" > test.txt
```

#### 上传文档

```bash
curl -X POST http://localhost:8081/api/v1/documents/upload \
  -F "file=@test.txt"
```

响应示例：
```json
{
  "documentId": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "test.txt",
  "status": "PROCESSING",
  "message": "文档上传成功，正在处理中",
  "fileSize": 24
}
```

#### 查询文档列表

```bash
curl http://localhost:8081/api/v1/documents?page=1&pageSize=10
```

#### 查询文档详情

```bash
curl http://localhost:8081/api/v1/documents/{documentId}
```

#### 删除文档

```bash
curl -X DELETE http://localhost:8081/api/v1/documents/{documentId}
```

### 7. 运行验证脚本

```bash
cd java-services/document-service
chmod +x verify-document-service.sh
./verify-document-service.sh
```

## 访问管理界面

- **MinIO Console**: http://localhost:9001 (admin / admin123456)
- **RabbitMQ Management**: http://localhost:15672 (admin / admin)
- **Nacos Console**: http://localhost:8848/nacos (nacos / nacos)
- **Sentinel Dashboard**: http://localhost:8858 (sentinel / sentinel)
- **Zipkin UI**: http://localhost:9411

## 查看日志

```bash
# 实时查看日志
tail -f logs/document-service.log

# 查看错误日志
tail -f logs/document-service-error.log

# Docker 日志
docker logs -f document-service
```

## 常见问题

### 1. 服务启动失败

**问题**: 无法连接到数据库

**解决方案**:
```bash
# 检查 PostgreSQL 是否运行
docker ps | grep postgres

# 检查数据库连接
psql -h localhost -U postgres -d rag_db
```

### 2. 文件上传失败

**问题**: MinIO 连接失败

**解决方案**:
```bash
# 检查 MinIO 是否运行
docker ps | grep minio

# 检查 MinIO 健康状态
curl http://localhost:9000/minio/health/live
```

### 3. 消息发送失败

**问题**: RabbitMQ 连接失败

**解决方案**:
```bash
# 检查 RabbitMQ 是否运行
docker ps | grep rabbitmq

# 检查 RabbitMQ 状态
curl http://localhost:15672/api/overview -u admin:admin
```

### 4. 服务注册失败

**问题**: 无法连接到 Nacos

**解决方案**:
```bash
# 检查 Nacos 是否运行
docker ps | grep nacos

# 检查 Nacos 健康状态
curl http://localhost:8848/nacos/v1/console/health/readiness
```

## 停止服务

```bash
# 停止 Document Service
# Maven 方式: Ctrl+C

# Docker 方式
docker stop document-service

# 停止所有依赖服务
docker-compose down

# 或单独停止
docker stop postgres redis minio rabbitmq nacos sentinel-dashboard zipkin
```

## 清理数据

```bash
# 删除 Docker 容器和数据卷
docker-compose down -v

# 或单独删除
docker rm -f postgres redis minio rabbitmq nacos sentinel-dashboard zipkin
docker volume prune
```

## 下一步

1. 启动 Python 文档处理服务（document-processing-service）
2. 测试完整的文档处理流程
3. 查看 Sentinel Dashboard 中的流控规则
4. 查看 Zipkin 中的链路追踪信息
5. 查看 Prometheus 指标

## 获取帮助

- 查看 [README.md](README.md) 了解详细功能说明
- 查看 [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) 了解实现细节
- 查看日志文件排查问题
- 检查 Nacos 配置中心的配置项
