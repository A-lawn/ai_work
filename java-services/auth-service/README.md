# Auth Service - 认证授权服务

## 概述

Auth Service 是 RAG 运维问答助手系统的认证授权服务，负责用户认证、JWT Token 管理和 API Key 管理。

## 功能特性

- **用户认证**: 基于用户名和密码的登录认证
- **JWT Token**: 生成和验证 JWT 访问令牌和刷新令牌
- **API Key 管理**: 创建、验证、撤销 API Keys
- **角色权限**: 基于角色的访问控制（RBAC）
- **服务注册**: 自动注册到 Nacos 服务注册中心
- **熔断降级**: 集成 Sentinel 实现流量控制和熔断降级
- **链路追踪**: 集成 Sleuth 和 Zipkin 实现分布式链路追踪

## 技术栈

- Spring Boot 3.x
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- JWT (jjwt)
- Nacos Discovery & Config
- Sentinel
- Sleuth & Zipkin

## API 接口

### 认证接口

#### 1. 用户登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

响应:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": "uuid",
  "username": "admin",
  "roles": ["ROLE_ADMIN"]
}
```

#### 2. 刷新 Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### 3. 验证 JWT Token
```http
POST /api/auth/validate-token
Authorization: Bearer <token>
```

响应:
```json
{
  "valid": true,
  "userId": "uuid",
  "username": "admin",
  "message": "Token is valid"
}
```

#### 4. 验证 API Key
```http
POST /api/auth/validate-api-key
X-API-Key: rak_xxxxxxxxxxxxx
```

响应:
```json
{
  "valid": true,
  "message": "API Key is valid"
}
```

### API Key 管理接口

#### 1. 创建 API Key
```http
POST /api/api-keys
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "My API Key",
  "description": "用于测试的 API Key",
  "expirationDays": 365
}
```

响应:
```json
{
  "id": "uuid",
  "name": "My API Key",
  "description": "用于测试的 API Key",
  "apiKey": "rak_xxxxxxxxxxxxx",
  "isActive": true,
  "expiresAt": "2025-12-04T00:00:00"
}
```

**注意**: `apiKey` 字段仅在创建时返回一次，请妥善保存。

#### 2. 获取所有 API Keys
```http
GET /api/api-keys
Authorization: Bearer <token>
```

#### 3. 撤销 API Key
```http
DELETE /api/api-keys/{apiKeyId}
Authorization: Bearer <token>
```

### 健康检查
```http
GET /api/health
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DATABASE_URL` | PostgreSQL 数据库连接 URL | `jdbc:postgresql://localhost:5432/rag_db` |
| `DATABASE_USERNAME` | 数据库用户名 | `postgres` |
| `DATABASE_PASSWORD` | 数据库密码 | `postgres` |
| `REDIS_HOST` | Redis 主机地址 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `NACOS_SERVER` | Nacos 服务器地址 | `localhost:8848` |
| `SENTINEL_DASHBOARD` | Sentinel 控制台地址 | `localhost:8858` |
| `ZIPKIN_URL` | Zipkin 服务器地址 | `http://localhost:9411` |
| `JWT_SECRET` | JWT 密钥（生产环境必须修改） | - |

### JWT 配置

在 `application.yml` 或 Nacos 配置中心配置:

```yaml
jwt:
  secret: your-secret-key-change-in-production-min-256-bits-long
  expiration: 86400000  # 24小时
  refresh-expiration: 604800000  # 7天
```

**安全提示**: 生产环境必须使用强随机密钥，长度至少 256 位。

### API Key 配置

```yaml
api-key:
  default-expiration-days: 365
  hash-algorithm: SHA-256
```

## 数据库表结构

### users 表
- `id`: UUID 主键
- `username`: 用户名（唯一）
- `password`: 加密后的密码
- `email`: 邮箱
- `full_name`: 全名
- `enabled`: 是否启用
- `account_non_expired`: 账户是否未过期
- `account_non_locked`: 账户是否未锁定
- `credentials_non_expired`: 凭证是否未过期
- `created_at`: 创建时间
- `updated_at`: 更新时间
- `last_login_at`: 最后登录时间

### roles 表
- `id`: UUID 主键
- `name`: 角色名（唯一）
- `description`: 描述
- `created_at`: 创建时间

### user_roles 表
- `user_id`: 用户 ID（外键）
- `role_id`: 角色 ID（外键）

### api_keys 表
- `id`: UUID 主键
- `key_hash`: API Key 哈希值（唯一）
- `name`: 名称
- `description`: 描述
- `user_id`: 所属用户 ID（外键）
- `is_active`: 是否激活
- `created_at`: 创建时间
- `expires_at`: 过期时间
- `last_used_at`: 最后使用时间

## 默认用户

系统启动时会自动创建默认管理员用户:

- **用户名**: `admin`
- **密码**: `admin123`
- **角色**: `ROLE_ADMIN`

**重要**: 生产环境部署后请立即修改默认密码！

## 本地开发

### 前置条件
- JDK 17+
- Maven 3.6+
- PostgreSQL 15+
- Redis 7+
- Nacos 2.2+

### 启动步骤

1. 启动 PostgreSQL 和 Redis
2. 启动 Nacos 服务器
3. 配置环境变量或修改 `application.yml`
4. 运行应用:
```bash
cd java-services/auth-service
mvn spring-boot:run
```

服务将在 `http://localhost:8083` 启动。

## Docker 部署

### 构建镜像
```bash
docker build -t auth-service:latest -f java-services/auth-service/Dockerfile .
```

### 运行容器
```bash
docker run -d \
  --name auth-service \
  -p 8083:8083 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/rag_db \
  -e REDIS_HOST=redis \
  -e NACOS_SERVER=nacos:8848 \
  auth-service:latest
```

## 监控和运维

### Actuator 端点

- 健康检查: `GET /actuator/health`
- 指标: `GET /actuator/metrics`
- Prometheus: `GET /actuator/prometheus`

### Sentinel 控制台

访问 Sentinel Dashboard 查看和配置流控规则:
- URL: `http://localhost:8858`
- 用户名: `sentinel`
- 密码: `sentinel`

### 日志

日志级别可在 `application.yml` 中配置:
```yaml
logging:
  level:
    com.rag.ops.auth: DEBUG
```

## 安全建议

1. **修改默认密码**: 部署后立即修改 admin 用户密码
2. **使用强密钥**: JWT_SECRET 必须使用强随机密钥
3. **HTTPS**: 生产环境必须使用 HTTPS
4. **密码策略**: 配置强密码策略
5. **API Key 管理**: 定期轮换 API Keys
6. **监控告警**: 配置异常登录告警

## 故障排查

### 常见问题

1. **无法连接数据库**
   - 检查 DATABASE_URL 配置
   - 确认 PostgreSQL 服务运行正常
   - 检查网络连接

2. **JWT Token 验证失败**
   - 检查 JWT_SECRET 配置是否一致
   - 确认 Token 未过期
   - 检查时钟同步

3. **无法注册到 Nacos**
   - 检查 NACOS_SERVER 配置
   - 确认 Nacos 服务运行正常
   - 检查网络连接

## 许可证

Copyright © 2024 RAG Ops Team
