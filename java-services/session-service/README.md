# Session Service (会话管理服务)

会话管理服务负责管理用户的多轮对话会话，包括会话创建、历史管理、消息存储和Token数量控制。

## 功能特性

- **会话管理**: 创建、查询、删除会话
- **消息管理**: 添加消息到会话，维护对话历史
- **滑动窗口**: 自动保留最近10轮对话（20条消息）
- **Token控制**: 限制会话总Token数不超过4000
- **Redis缓存**: 会话历史缓存，提高查询性能
- **服务治理**: 集成Nacos服务注册、Sentinel限流熔断
- **链路追踪**: 集成Sleuth和Zipkin

## 技术栈

- Spring Boot 3.x
- Spring Data JPA + PostgreSQL
- Spring Data Redis
- Spring Cloud Alibaba (Nacos, Sentinel)
- Spring Cloud Sleuth + Zipkin
- Lombok

## API 端点

### 1. 创建会话
```http
POST /api/v1/sessions
Content-Type: application/json

{
  "userId": "user123",
  "metadata": "{}"
}

Response:
{
  "sessionId": "uuid",
  "userId": "user123",
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00",
  "messageCount": 0,
  "metadata": "{}"
}
```

### 2. 获取会话历史
```http
GET /api/v1/sessions/{id}/history

Response:
{
  "sessionId": "uuid",
  "userId": "user123",
  "messages": [
    {
      "id": "msg-uuid",
      "role": "USER",
      "content": "问题内容",
      "timestamp": "2024-01-01T10:00:00",
      "metadata": null
    },
    {
      "id": "msg-uuid-2",
      "role": "ASSISTANT",
      "content": "回答内容",
      "timestamp": "2024-01-01T10:00:05",
      "metadata": null
    }
  ],
  "totalMessages": 2,
  "totalTokens": 150
}
```

### 3. 添加消息到会话
```http
POST /api/v1/sessions/{id}/messages
Content-Type: application/json

{
  "role": "USER",
  "content": "这是一个问题",
  "metadata": null
}

Response:
{
  "id": "msg-uuid",
  "role": "USER",
  "content": "这是一个问题",
  "timestamp": "2024-01-01T10:00:00",
  "metadata": null
}
```

### 4. 删除会话
```http
DELETE /api/v1/sessions/{id}

Response:
{
  "success": true,
  "message": "会话删除成功"
}
```

### 5. 获取用户会话列表
```http
GET /api/v1/sessions?userId=user123

Response:
[
  {
    "sessionId": "uuid-1",
    "userId": "user123",
    "createdAt": "2024-01-01T10:00:00",
    "updatedAt": "2024-01-01T10:30:00",
    "messageCount": 10,
    "metadata": null
  }
]
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DATABASE_URL | PostgreSQL连接URL | jdbc:postgresql://localhost:5432/rag_db |
| DATABASE_USERNAME | 数据库用户名 | postgres |
| DATABASE_PASSWORD | 数据库密码 | postgres |
| REDIS_HOST | Redis主机地址 | localhost |
| REDIS_PORT | Redis端口 | 6379 |
| NACOS_SERVER | Nacos服务器地址 | localhost:8848 |
| NACOS_NAMESPACE | Nacos命名空间 | rag-system |
| SENTINEL_DASHBOARD | Sentinel控制台地址 | localhost:8858 |
| ZIPKIN_URL | Zipkin服务器地址 | http://localhost:9411 |

### 应用配置

主要配置项在 `application.yml` 中：

```yaml
server:
  port: 8082

spring:
  application:
    name: session-service
  
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
```

## 数据模型

### Session (会话)
- id: 会话ID (UUID)
- userId: 用户ID
- messages: 消息列表
- createdAt: 创建时间
- updatedAt: 更新时间
- metadata: 元数据 (JSON)

### Message (消息)
- id: 消息ID (UUID)
- sessionId: 所属会话ID
- role: 角色 (USER/ASSISTANT)
- content: 消息内容
- timestamp: 时间戳
- metadata: 元数据 (JSON)

## 核心功能

### 滑动窗口策略

系统自动维护最近10轮对话（20条消息）：
- 当消息数超过20条时，自动删除最旧的消息
- 保证会话历史不会无限增长
- 提高查询和处理性能

### Token数量控制

系统限制每个会话的总Token数不超过4000：
- 添加消息时自动计算Token数量
- 超过限制时抛出异常
- 简单估算：中文按字符数，英文按单词数×1.3

### Redis缓存

会话历史查询结果缓存到Redis：
- 缓存时间：1小时
- 添加/删除消息时自动清除缓存
- 提高查询性能

## 本地开发

### 前置条件

- JDK 17+
- Maven 3.6+
- PostgreSQL 15+
- Redis 7+
- Nacos 2.2+

### 启动步骤

1. 启动依赖服务（PostgreSQL, Redis, Nacos）

2. 配置环境变量或修改 `application.yml`

3. 启动服务：
```bash
cd java-services/session-service
mvn spring-boot:run
```

4. 验证服务：
```bash
curl http://localhost:8082/api/health
```

## Docker部署

### 构建镜像
```bash
docker build -t session-service:latest -f java-services/session-service/Dockerfile .
```

### 运行容器
```bash
docker run -d \
  --name session-service \
  -p 8082:8082 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/rag_db \
  -e REDIS_HOST=redis \
  -e NACOS_SERVER=nacos:8848 \
  session-service:latest
```

## 监控和运维

### 健康检查
```bash
curl http://localhost:8082/api/health
```

### Prometheus指标
```bash
curl http://localhost:8082/actuator/prometheus
```

### 日志查看
```bash
# Docker容器日志
docker logs -f session-service

# 本地日志文件
tail -f /tmp/session-service.log
```

## 故障排查

### 常见问题

1. **数据库连接失败**
   - 检查PostgreSQL是否启动
   - 验证连接URL和凭据
   - 检查网络连接

2. **Redis连接失败**
   - 检查Redis是否启动
   - 验证主机地址和端口
   - 检查防火墙设置

3. **Nacos注册失败**
   - 检查Nacos服务器是否可访问
   - 验证命名空间配置
   - 查看服务日志

## 性能优化

- 使用Redis缓存减少数据库查询
- 滑动窗口策略控制数据量
- 数据库连接池优化
- 异步日志提高性能

## 安全考虑

- 输入验证防止注入攻击
- Token限制防止资源滥用
- 会话隔离保护用户数据
- 敏感信息不记录日志
