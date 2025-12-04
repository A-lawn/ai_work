# Config Service（配置管理服务）

## 概述

配置管理服务负责管理系统的所有配置参数，包括文档处理、检索、LLM、嵌入模型等配置。支持配置的CRUD操作、配置验证、配置同步到Nacos配置中心，以及LLM连接测试功能。

## 功能特性

- ✅ 系统配置管理（CRUD）
- ✅ 配置类型验证（INTEGER、FLOAT、BOOLEAN、JSON、STRING）
- ✅ 配置同步到Nacos配置中心
- ✅ LLM连接测试
- ✅ 批量配置更新
- ✅ 配置分类管理
- ✅ Sentinel流控和熔断
- ✅ Nacos服务注册与发现
- ✅ 链路追踪（Zipkin）
- ✅ Prometheus指标暴露

## 技术栈

- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Nacos Discovery & Config
- Sentinel
- OpenFeign
- Spring Cloud Sleuth + Zipkin
- Prometheus

## 端口

- 服务端口: 8085
- Sentinel端口: 8729

## API接口

### 1. 获取所有配置

```http
GET /api/v1/config
```

响应示例：
```json
{
  "document.chunk_size": 512,
  "document.chunk_overlap": 50,
  "retrieval.top_k": 5,
  "llm.provider": "openai",
  "llm.model": "gpt-4"
}
```

### 2. 获取所有配置详情

```http
GET /api/v1/config/details
```

响应示例：
```json
[
  {
    "id": 1,
    "configKey": "document.chunk_size",
    "configValue": "512",
    "configType": "INTEGER",
    "description": "文档分块大小（tokens）",
    "isActive": true,
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
]
```

### 3. 根据配置键获取配置

```http
GET /api/v1/config/{configKey}
```

### 4. 更新配置

```http
PUT /api/v1/config/{configKey}
Content-Type: application/json

{
  "configValue": "1024",
  "description": "更新后的描述"
}
```

### 5. 创建配置

```http
POST /api/v1/config
Content-Type: application/json

{
  "configKey": "custom.config",
  "configValue": "value",
  "configType": "STRING",
  "description": "自定义配置",
  "isActive": true
}
```

### 6. 批量更新配置

```http
PUT /api/v1/config
Content-Type: application/json

{
  "document.chunk_size": "1024",
  "retrieval.top_k": "10",
  "llm.temperature": "0.8"
}
```

响应示例：
```json
{
  "success": 3,
  "failed": 0,
  "total": 3
}
```

### 7. 测试LLM连接

```http
POST /api/v1/config/test-llm
Content-Type: application/json

{
  "provider": "openai",
  "apiKey": "sk-xxx",
  "endpoint": "https://api.openai.com/v1",
  "model": "gpt-4",
  "testPrompt": "Hello, this is a test."
}
```

响应示例：
```json
{
  "success": true,
  "message": "LLM连接测试成功",
  "latency": 1234,
  "response": "Hello! I'm working correctly."
}
```

### 8. 健康检查

```http
GET /api/health
```

响应示例：
```json
{
  "status": "UP",
  "service": "config-service",
  "timestamp": 1704067200000
}
```

## 配置类型

支持以下配置类型：

- `INTEGER`: 整数类型
- `FLOAT`: 浮点数类型
- `BOOLEAN`: 布尔类型（true/false）
- `JSON`: JSON格式
- `STRING`: 字符串类型（默认）

## 默认配置

服务启动时会自动初始化以下默认配置：

### 文档处理配置
- `document.chunk_size`: 512
- `document.chunk_overlap`: 50
- `document.supported_formats`: pdf,docx,txt,md
- `document.max_file_size`: 52428800 (50MB)

### 检索配置
- `retrieval.top_k`: 5
- `retrieval.similarity_threshold`: 0.7

### LLM配置
- `llm.provider`: openai
- `llm.model`: gpt-4
- `llm.temperature`: 0.7
- `llm.max_tokens`: 1000
- `llm.api_base`: https://api.openai.com/v1

### 嵌入模型配置
- `embedding.provider`: openai
- `embedding.model`: text-embedding-ada-002

### 会话配置
- `session.max_history`: 10
- `session.max_tokens`: 4000

### 系统配置
- `system.language`: zh-CN

## 环境变量

```bash
# 数据库配置
DATABASE_HOST=localhost
DATABASE_PORT=5432
DATABASE_NAME=rag_db
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres

# Nacos配置
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=rag-system
NACOS_GROUP=DEFAULT_GROUP

# Sentinel配置
SENTINEL_DASHBOARD=localhost:8858

# Zipkin配置
ZIPKIN_URL=http://localhost:9411/api/v2/spans
```

## 本地开发

### 前置条件

- JDK 17+
- Maven 3.9+
- PostgreSQL 15+
- Nacos Server
- Sentinel Dashboard

### 启动步骤

1. 启动PostgreSQL数据库
2. 启动Nacos Server
3. 启动Sentinel Dashboard
4. 配置环境变量
5. 运行服务：

```bash
cd java-services/config-service
mvn spring-boot:run
```

## Docker部署

### 构建镜像

```bash
docker build -t config-service:latest -f java-services/config-service/Dockerfile .
```

### 运行容器

```bash
docker run -d \
  --name config-service \
  -p 8085:8085 \
  -e DATABASE_HOST=postgres \
  -e NACOS_SERVER=nacos:8848 \
  -e SENTINEL_DASHBOARD=sentinel-dashboard:8858 \
  config-service:latest
```

## 监控指标

服务暴露以下Prometheus指标：

- JVM内存使用
- HTTP请求统计
- 数据库连接池状态
- Sentinel流控指标
- 自定义业务指标

访问指标端点：
```
http://localhost:8085/actuator/prometheus
```

## Sentinel规则配置

### 流控规则示例

```json
[
  {
    "resource": "getAllConfigs",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0
  }
]
```

### 熔断规则示例

```json
[
  {
    "resource": "testLlmConnection",
    "grade": 0,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5
  }
]
```

## 故障排查

### 常见问题

1. **服务无法启动**
   - 检查数据库连接
   - 检查Nacos连接
   - 查看日志文件

2. **配置同步失败**
   - 检查Nacos Config配置
   - 验证命名空间和分组
   - 查看Nacos日志

3. **LLM测试失败**
   - 验证API Key
   - 检查网络连接
   - 确认LLM Service可用

## 日志

日志文件位置：`/tmp/config-service.log`

日志级别：
- root: INFO
- com.rag.ops.config: DEBUG
- org.springframework.cloud: DEBUG

## 相关服务

- Gateway Service: http://localhost:8080
- LLM Service: http://localhost:9004
- Nacos Console: http://localhost:8848/nacos
- Sentinel Dashboard: http://localhost:8858

## 开发团队

RAG Ops QA Assistant Team
