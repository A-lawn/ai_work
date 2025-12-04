# Config Service 快速开始指南

## 概述

本指南将帮助您快速启动和测试 Config Service（配置管理服务）。

## 前置条件

确保以下服务已启动：

1. **PostgreSQL** (端口 5432)
2. **Nacos Server** (端口 8848)
3. **Sentinel Dashboard** (端口 8858) - 可选
4. **Zipkin** (端口 9411) - 可选

## 快速启动

### 方式一：使用 Maven

```bash
# 进入服务目录
cd java-services/config-service

# 启动服务
mvn spring-boot:run
```

### 方式二：使用 Docker Compose

```bash
# 在项目根目录
docker-compose up -d config-service
```

### 方式三：使用 JAR 包

```bash
# 构建
cd java-services/config-service
mvn clean package -DskipTests

# 运行
java -jar target/config-service-1.0.0.jar
```

## 验证服务

### 1. 检查服务健康状态

```bash
curl http://localhost:8085/api/health
```

预期响应：
```json
{
  "status": "UP",
  "service": "config-service",
  "timestamp": 1704067200000
}
```

### 2. 查看所有配置

```bash
curl http://localhost:8085/api/v1/config
```

### 3. 获取配置详情

```bash
curl http://localhost:8085/api/v1/config/details
```

### 4. 运行验证脚本

```bash
chmod +x java-services/config-service/verify-config-service.sh
./java-services/config-service/verify-config-service.sh
```

## 常用操作

### 创建配置

```bash
curl -X POST http://localhost:8085/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "configKey": "custom.setting",
    "configValue": "my_value",
    "configType": "STRING",
    "description": "自定义配置",
    "isActive": true
  }'
```

### 更新配置

```bash
curl -X PUT http://localhost:8085/api/v1/config/custom.setting \
  -H "Content-Type: application/json" \
  -d '{
    "configValue": "new_value",
    "description": "更新后的配置"
  }'
```

### 批量更新配置

```bash
curl -X PUT http://localhost:8085/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "document.chunk_size": "1024",
    "retrieval.top_k": "10",
    "llm.temperature": "0.8"
  }'
```

### 测试 LLM 连接

```bash
curl -X POST http://localhost:8085/api/v1/config/test-llm \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "apiKey": "sk-your-api-key",
    "endpoint": "https://api.openai.com/v1",
    "model": "gpt-4",
    "testPrompt": "Hello, this is a test."
  }'
```

## 配置说明

### 默认配置项

服务启动时会自动创建以下默认配置：

| 配置键 | 默认值 | 类型 | 说明 |
|--------|--------|------|------|
| document.chunk_size | 512 | INTEGER | 文档分块大小 |
| document.chunk_overlap | 50 | INTEGER | 分块重叠大小 |
| document.supported_formats | pdf,docx,txt,md | STRING | 支持的文档格式 |
| document.max_file_size | 52428800 | INTEGER | 最大文件大小（50MB） |
| retrieval.top_k | 5 | INTEGER | 检索返回数量 |
| retrieval.similarity_threshold | 0.7 | FLOAT | 相似度阈值 |
| llm.provider | openai | STRING | LLM提供商 |
| llm.model | gpt-4 | STRING | LLM模型 |
| llm.temperature | 0.7 | FLOAT | 温度参数 |
| llm.max_tokens | 1000 | INTEGER | 最大tokens |
| embedding.provider | openai | STRING | 嵌入模型提供商 |
| embedding.model | text-embedding-ada-002 | STRING | 嵌入模型 |
| session.max_history | 10 | INTEGER | 最大历史轮数 |
| session.max_tokens | 4000 | INTEGER | 会话最大tokens |

### 环境变量配置

创建 `.env` 文件或设置环境变量：

```bash
# 数据库
export DATABASE_HOST=localhost
export DATABASE_PORT=5432
export DATABASE_NAME=rag_db
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres

# Nacos
export NACOS_SERVER=localhost:8848
export NACOS_NAMESPACE=rag-system
export NACOS_GROUP=DEFAULT_GROUP

# Sentinel
export SENTINEL_DASHBOARD=localhost:8858

# Zipkin
export ZIPKIN_URL=http://localhost:9411/api/v2/spans
```

## 监控和管理

### 查看 Prometheus 指标

```bash
curl http://localhost:8085/actuator/prometheus
```

### 查看健康检查详情

```bash
curl http://localhost:8085/actuator/health
```

### 查看应用信息

```bash
curl http://localhost:8085/actuator/info
```

### Sentinel 控制台

访问 Sentinel Dashboard 查看流控和熔断规则：

```
http://localhost:8858
```

### Nacos 控制台

访问 Nacos Console 查看服务注册和配置：

```
http://localhost:8848/nacos
用户名: nacos
密码: nacos
```

## 日志查看

### 查看实时日志

```bash
# Docker
docker logs -f config-service

# 本地文件
tail -f /tmp/config-service.log
```

### 日志级别

默认日志级别：
- root: INFO
- com.rag.ops.config: DEBUG
- org.springframework.cloud: DEBUG

## 故障排查

### 服务无法启动

1. 检查数据库连接：
```bash
psql -h localhost -U postgres -d rag_db
```

2. 检查 Nacos 连接：
```bash
curl http://localhost:8848/nacos/v1/console/health/readiness
```

3. 查看日志：
```bash
tail -100 /tmp/config-service.log
```

### 配置同步失败

1. 检查 Nacos Config 配置
2. 验证命名空间和分组设置
3. 查看 Nacos 日志

### LLM 测试失败

1. 验证 API Key 是否正确
2. 检查网络连接
3. 确认 LLM Service 是否可用

## 集成测试

运行完整的集成测试：

```bash
# 启动所有依赖服务
docker-compose up -d postgres nacos sentinel-dashboard

# 等待服务就绪
sleep 30

# 启动 Config Service
mvn spring-boot:run &

# 等待服务启动
sleep 20

# 运行验证脚本
./verify-config-service.sh

# 停止服务
pkill -f config-service
```

## 下一步

- 查看 [README.md](README.md) 了解详细功能
- 查看 [API 文档](README.md#api接口) 了解所有接口
- 集成到 Gateway Service 进行统一访问
- 配置 Sentinel 规则进行流控和熔断

## 相关链接

- [Nacos 文档](https://nacos.io/zh-cn/docs/quick-start.html)
- [Sentinel 文档](https://sentinelguard.io/zh-cn/docs/introduction.html)
- [Spring Cloud Alibaba 文档](https://spring-cloud-alibaba-group.github.io/github-pages/2022/zh-cn/index.html)

## 支持

如有问题，请查看：
- 服务日志
- Nacos 控制台
- Sentinel Dashboard
- 项目 README.md
