# Gateway Service

API 网关服务 - 智能运维问答助手系统的统一入口

## 功能特性

### 1. 路由转发
- 统一入口，路由请求到各个微服务
- 基于 Spring Cloud Gateway 的响应式路由
- 支持负载均衡（通过 Nacos 服务发现）
- 动态路由配置

### 2. 认证鉴权
- JWT Token 验证
- API Key 验证
- 白名单路径配置
- 集成 Auth Service 进行认证

### 3. 限流熔断
- 基于 Sentinel 的流量控制
- QPS 限流
- 异常比例熔断
- 慢调用比例熔断
- 系统保护规则
- 规则持久化到 Nacos

### 4. 日志和链路追踪
- 请求/响应日志记录
- 慢请求告警
- 错误请求记录
- Sleuth + Zipkin 链路追踪
- 分布式追踪 ID

### 5. 监控指标
- Prometheus 指标暴露
- 健康检查端点
- 服务发现状态监控

## 路由配置

### 服务路由

| 路径 | 目标服务 | 说明 |
|------|---------|------|
| `/api/v1/documents/**` | document-service | 文档管理 |
| `/api/v1/sessions/**` | session-service | 会话管理 |
| `/api/auth/**` | auth-service | 认证授权 |
| `/api/v1/config/**` | config-service | 配置管理 |
| `/api/v1/logs/**` | monitor-service | 日志查询 |
| `/api/v1/metrics/**` | monitor-service | 指标查询 |
| `/api/v1/stats/**` | monitor-service | 统计信息 |
| `/api/v1/query/**` | rag-query-service | RAG 查询 |

### 白名单路径

以下路径不需要认证：
- `/api/auth/login` - 用户登录
- `/api/auth/register` - 用户注册
- `/actuator/health` - 健康检查
- `/actuator/prometheus` - Prometheus 指标

## 认证方式

### JWT Token 认证

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/documents
```

### API Key 认证

```bash
curl -H "X-API-Key: <api-key>" http://localhost:8080/api/v1/documents
```

## Sentinel 规则配置

### 流控规则

规则存储在 Nacos：`gateway-service-flow-rules`

```json
[
  {
    "resource": "document-service",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0
  }
]
```

### 熔断规则

规则存储在 Nacos：`gateway-service-degrade-rules`

```json
[
  {
    "resource": "document-service",
    "grade": 0,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5
  }
]
```

### 系统保护规则

规则存储在 Nacos：`gateway-service-system-rules`

```json
[
  {
    "resource": "system",
    "highestSystemLoad": 8.0,
    "avgRt": 3000,
    "maxThread": 100,
    "qps": 1000
  }
]
```

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `NACOS_SERVER` | Nacos 服务地址 | localhost:8848 |
| `NACOS_NAMESPACE` | Nacos 命名空间 | rag-system |
| `SENTINEL_DASHBOARD` | Sentinel 控制台地址 | localhost:8858 |
| `ZIPKIN_URL` | Zipkin 服务地址 | http://localhost:9411 |
| `REDIS_HOST` | Redis 主机地址 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |

### 应用配置

主要配置文件：
- `bootstrap.yml` - Nacos 配置中心连接
- `application.yml` - 应用配置和路由规则
- `logback-spring.xml` - 日志配置

## 监控端点

### 健康检查

```bash
# 基础健康检查
curl http://localhost:8080/actuator/health

# 详细健康检查（包含服务发现信息）
curl http://localhost:8080/actuator/health/detail
```

### Prometheus 指标

```bash
curl http://localhost:8080/actuator/prometheus
```

## 日志

### 日志文件

- `logs/gateway-service.log` - 所有日志
- `logs/gateway-service-error.log` - 错误日志

### 日志格式

```
2024-12-04 10:30:45.123 [http-nio-8080-exec-1] [traceId,spanId] INFO  c.r.o.g.filter.RequestLoggingFilter - ==> [requestId] GET /api/v1/documents from 192.168.1.100 at 2024-12-04 10:30:45.123
2024-12-04 10:30:45.456 [http-nio-8080-exec-1] [traceId,spanId] INFO  c.r.o.g.filter.RequestLoggingFilter - <== [requestId] GET /api/v1/documents - Status: 200 - Duration: 333ms - Completed at 2024-12-04 10:30:45.456
```

## 构建和运行

### 本地开发

```bash
# 编译
mvn clean package

# 运行
java -jar target/gateway-service-1.0.0.jar
```

### Docker 部署

```bash
# 构建镜像
docker build -t gateway-service:1.0.0 .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e NACOS_SERVER=nacos:8848 \
  -e ZIPKIN_URL=http://zipkin:9411 \
  gateway-service:1.0.0
```

### Docker Compose

```bash
docker-compose up gateway-service
```

## 故障排查

### 常见问题

1. **服务无法注册到 Nacos**
   - 检查 Nacos 服务是否启动
   - 检查网络连接
   - 查看日志：`logs/gateway-service.log`

2. **认证失败**
   - 检查 Auth Service 是否正常运行
   - 验证 Token 或 API Key 是否有效
   - 查看认证日志

3. **请求被限流**
   - 检查 Sentinel 规则配置
   - 访问 Sentinel Dashboard 查看实时监控
   - 调整限流阈值

4. **链路追踪数据缺失**
   - 检查 Zipkin 服务是否启动
   - 验证采样率配置
   - 查看 Zipkin UI

## 性能优化

### 建议配置

- JVM 参数：`-Xms512m -Xmx1024m`
- 连接池大小：根据并发量调整
- 采样率：生产环境建议 10%
- 日志级别：生产环境使用 INFO

## 安全建议

1. 启用 HTTPS
2. 配置 IP 白名单
3. 定期轮换 API Key
4. 监控异常请求
5. 限制请求大小

## 版本信息

- Spring Boot: 3.0.x
- Spring Cloud: 2022.0.x
- Spring Cloud Alibaba: 2022.0.0.0
- Sentinel: 1.8.6
- Nacos: 2.2.3

## 相关文档

- [Spring Cloud Gateway 官方文档](https://spring.io/projects/spring-cloud-gateway)
- [Sentinel 官方文档](https://sentinelguard.io/)
- [Nacos 官方文档](https://nacos.io/)
- [Zipkin 官方文档](https://zipkin.io/)
