# Document Service（文档管理服务）

## 概述

文档管理服务负责处理文档的上传、存储、查询和删除操作。它是 RAG 系统的核心服务之一，负责管理文档元数据并协调文档处理流程。

## 主要功能

- **文档上传**: 接收用户上传的文档，验证格式和大小
- **文件存储**: 将文档存储到 MinIO 对象存储
- **元数据管理**: 在 PostgreSQL 中管理文档元数据
- **异步处理**: 通过 RabbitMQ 触发文档处理流程
- **文档查询**: 提供分页查询和详情查询接口
- **文档删除**: 删除文档及其关联的文件和向量数据
- **服务间调用**: 使用 OpenFeign 调用 Python 文档处理服务
- **熔断降级**: 使用 Sentinel 保护服务调用

## 技术栈

- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Redis
- MinIO
- RabbitMQ
- Nacos Discovery & Config
- Sentinel
- OpenFeign
- Spring Cloud Sleuth + Zipkin

## API 接口

### 1. 上传文档

```http
POST /api/v1/documents/upload
Content-Type: multipart/form-data

参数:
- file: 文档文件

响应:
{
  "documentId": "uuid",
  "filename": "example.pdf",
  "status": "PROCESSING",
  "message": "文档上传成功，正在处理中",
  "fileSize": 1024000
}
```

### 2. 获取文档列表

```http
GET /api/v1/documents?page=1&pageSize=20&status=COMPLETED

响应:
{
  "content": [
    {
      "id": "uuid",
      "filename": "example.pdf",
      "fileType": "pdf",
      "fileSize": 1024000,
      "uploadTime": "2024-01-01T10:00:00",
      "chunkCount": 10,
      "status": "COMPLETED",
      "createdAt": "2024-01-01T10:00:00",
      "updatedAt": "2024-01-01T10:05:00"
    }
  ],
  "page": 1,
  "pageSize": 20,
  "total": 100,
  "totalPages": 5
}
```

### 3. 获取文档详情

```http
GET /api/v1/documents/{id}

响应:
{
  "id": "uuid",
  "filename": "example.pdf",
  "fileType": "pdf",
  "fileSize": 1024000,
  "uploadTime": "2024-01-01T10:00:00",
  "chunkCount": 10,
  "status": "COMPLETED",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:05:00"
}
```

### 4. 删除文档

```http
DELETE /api/v1/documents/{id}

响应: 204 No Content
```

## 配置说明

### 环境变量

```bash
# 数据库配置
DATABASE_URL=jdbc:postgresql://localhost:5432/rag_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres123

# Redis 配置
REDIS_HOST=localhost
REDIS_PORT=6379

# MinIO 配置
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=admin123456
MINIO_BUCKET_NAME=rag-documents

# RabbitMQ 配置
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=admin

# Nacos 配置
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=rag-system

# Sentinel 配置
SENTINEL_DASHBOARD=localhost:8858

# Zipkin 配置
ZIPKIN_URL=http://localhost:9411
```

## 文档状态

- **PROCESSING**: 处理中
- **COMPLETED**: 已完成
- **FAILED**: 失败

## 支持的文档格式

- PDF (.pdf)
- Word (.docx)
- 文本 (.txt)
- Markdown (.md)

## 文件大小限制

最大文件大小: 50MB

## Sentinel 规则

### 流控规则

- **uploadDocument**: QPS 限制为 10
- **processDocument**: QPS 限制为 20

### 熔断规则

- **异常比例熔断**: 50% 异常比例，熔断 10 秒
- **慢调用熔断**: 响应时间超过 3 秒，熔断 10 秒

## 本地开发

### 启动依赖服务

```bash
# 启动 PostgreSQL
docker run -d --name postgres -p 5432:5432 \
  -e POSTGRES_DB=rag_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres123 \
  postgres:15

# 启动 Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 启动 MinIO
docker run -d --name minio -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=admin \
  -e MINIO_ROOT_PASSWORD=admin123456 \
  minio/minio server /data --console-address ":9001"

# 启动 RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3.12-management

# 启动 Nacos
docker run -d --name nacos -p 8848:8848 \
  -e MODE=standalone \
  nacos/nacos-server:v2.2.3
```

### 启动服务

```bash
cd java-services/document-service
mvn spring-boot:run
```

### 构建 Docker 镜像

```bash
docker build -t document-service:latest -f java-services/document-service/Dockerfile .
```

## 监控和日志

### 健康检查

```http
GET /actuator/health
```

### Prometheus 指标

```http
GET /actuator/prometheus
```

### 日志级别

- root: INFO
- com.rag.ops: DEBUG

## 故障排查

### 文档上传失败

1. 检查文件格式是否支持
2. 检查文件大小是否超过限制
3. 检查 MinIO 连接是否正常
4. 查看日志中的详细错误信息

### 文档处理超时

1. 检查 RabbitMQ 连接是否正常
2. 检查 Python 文档处理服务是否运行
3. 查看 Sentinel Dashboard 是否有熔断记录

### 服务调用失败

1. 检查 Nacos 服务注册是否正常
2. 检查网络连接是否正常
3. 查看 Sentinel 熔断降级日志
4. 检查 OpenFeign 配置是否正确
