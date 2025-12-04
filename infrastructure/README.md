# 微服务基础设施配置

本目录包含智能运维问答助手系统的所有基础设施配置文件。

## 目录结构

```
infrastructure/
├── nacos/                  # Nacos 服务注册与配置中心
│   ├── init-nacos.sh      # Nacos 初始化脚本
│   └── config/            # Nacos 配置文件
│       ├── common-config.yaml
│       ├── gateway-service.yaml
│       ├── document-service.yaml
│       ├── session-service.yaml
│       ├── auth-service.yaml
│       ├── monitor-service.yaml
│       ├── config-service.yaml
│       └── sentinel/      # Sentinel 规则配置
│           ├── *-flow-rules.json
│           ├── *-degrade-rules.json
│           └── *-system-rules.json
├── sentinel/              # Sentinel 控制台配置
│   └── application.properties
├── rabbitmq/              # RabbitMQ 消息队列配置
│   ├── init-rabbitmq.sh  # RabbitMQ 初始化脚本
│   └── definitions.json   # RabbitMQ 定义文件
├── zipkin/                # Zipkin 链路追踪配置
│   └── zipkin-config.yml
├── prometheus/            # Prometheus 监控配置
│   ├── prometheus.yml     # Prometheus 主配置
│   ├── alerting_rules.yml # 告警规则
│   └── alertmanager.yml   # Alertmanager 配置
├── grafana/               # Grafana 可视化配置
│   ├── datasources/       # 数据源配置
│   │   └── prometheus.yml
│   └── dashboards/        # 仪表盘配置
│       ├── dashboard.yml
│       └── json/
│           └── rag-system-overview.json
└── postgres/              # PostgreSQL 数据库配置
    └── init.sql           # 数据库初始化脚本
```

## 基础设施组件

### 1. Nacos (服务注册与配置中心)

**端口**: 8848, 9848

**功能**:
- 服务注册与发现
- 动态配置管理
- 命名空间隔离 (rag-system)

**访问地址**: http://localhost:8848/nacos
**默认账号**: nacos / nacos

**初始化**:
系统启动时会自动执行 `init-nacos.sh` 脚本，创建：
- 命名空间: rag-system
- 公共配置: common-config.yaml
- 各服务专属配置
- Sentinel 规则配置

### 2. Sentinel Dashboard (流量控制)

**端口**: 8858

**功能**:
- 流量控制（限流）
- 熔断降级
- 系统保护
- 实时监控

**访问地址**: http://localhost:8858
**默认账号**: sentinel / sentinel

**规则持久化**: 规则存储在 Nacos 中，重启后自动恢复

### 3. RabbitMQ (消息队列)

**端口**: 5672 (AMQP), 15672 (管理界面)

**功能**:
- 异步消息通信
- 文档处理队列
- 批量任务队列
- 日志事件队列

**访问地址**: http://localhost:15672
**默认账号**: admin / admin123

**队列列表**:
- `document.processing.queue` - 文档处理队列
- `document.completed.queue` - 文档处理完成队列
- `batch.processing.queue` - 批量处理队列
- `log.event.queue` - 日志事件队列
- `dlx.queue` - 死信队列

### 4. Zipkin (链路追踪)

**端口**: 9411

**功能**:
- 分布式链路追踪
- 服务调用关系分析
- 性能瓶颈定位

**访问地址**: http://localhost:9411
**存储**: Elasticsearch

**采样率**: 100% (可在配置中调整)

### 5. Prometheus (指标采集)

**端口**: 9090

**功能**:
- 指标采集与存储
- 告警规则评估
- 时序数据查询

**访问地址**: http://localhost:9090

**监控目标**:
- 所有 Java 微服务 (通过 /actuator/prometheus)
- 所有 Python 微服务 (通过 /metrics)
- 基础设施组件

### 6. Grafana (可视化)

**端口**: 3001

**功能**:
- 监控数据可视化
- 自定义仪表盘
- 告警通知

**访问地址**: http://localhost:3001
**默认账号**: admin / admin

**预置仪表盘**:
- RAG System Overview - 系统总览

### 7. PostgreSQL (关系数据库)

**端口**: 5432

**数据库**: rag_db
**默认账号**: postgres / postgres123

**表结构**:
- documents - 文档元数据
- sessions - 会话信息
- messages - 消息记录
- users - 用户信息
- api_keys - API 密钥
- operation_logs - 操作日志
- performance_metrics - 性能指标
- system_configs - 系统配置

### 8. Redis (缓存)

**端口**: 6379

**功能**:
- 会话缓存
- 查询结果缓存
- 分布式锁
- Celery 任务队列

**密码**: redis123

### 9. ChromaDB (向量数据库)

**端口**: 8001

**功能**:
- 文档向量存储
- 相似度检索

### 10. Elasticsearch (日志存储)

**端口**: 9200, 9300

**功能**:
- 日志存储与检索
- Zipkin 链路数据存储

### 11. MinIO (对象存储)

**端口**: 9000 (API), 9001 (控制台)

**功能**:
- 文档文件存储
- S3 兼容接口

**访问地址**: http://localhost:9001
**默认账号**: admin / admin123456

## 启动顺序

Docker Compose 会自动管理启动顺序，依赖关系如下：

1. 基础设施层
   - PostgreSQL
   - Redis
   - Elasticsearch
   - ChromaDB
   - MinIO

2. 服务治理层
   - Nacos
   - Sentinel Dashboard
   - RabbitMQ
   - Zipkin
   - Prometheus
   - Grafana

3. 初始化服务
   - nacos-init (配置初始化)
   - rabbitmq-init (队列初始化)

4. 应用服务层
   - Java 微服务
   - Python 微服务
   - 前端服务

## 配置说明

### Nacos 配置

所有服务的配置都存储在 Nacos 中，分为：

1. **公共配置** (common-config.yaml)
   - 数据库连接
   - Redis 配置
   - RabbitMQ 配置
   - Sleuth/Zipkin 配置
   - Actuator 配置

2. **服务专属配置** (各服务.yaml)
   - 服务端口
   - 业务配置
   - Sentinel 规则数据源

### Sentinel 规则

每个服务有三类规则：

1. **流控规则** (flow-rules)
   - QPS 限流
   - 并发线程数限流

2. **熔断规则** (degrade-rules)
   - 异常比例熔断
   - 慢调用比例熔断
   - RT 熔断

3. **系统保护规则** (system-rules)
   - CPU 使用率
   - 系统负载
   - 平均 RT
   - 并发线程数
   - 入口 QPS

## 健康检查

所有服务都配置了健康检查：

- **检查间隔**: 10秒
- **超时时间**: 5秒
- **重试次数**: 5次

可以通过以下命令查看服务健康状态：

```bash
docker-compose ps
```

## 日志查看

查看特定服务的日志：

```bash
docker-compose logs -f <service-name>
```

例如：
```bash
docker-compose logs -f nacos
docker-compose logs -f gateway-service
```

## 故障排查

### Nacos 无法启动
- 检查端口 8848 是否被占用
- 查看日志: `docker-compose logs nacos`

### Sentinel Dashboard 无法连接
- 确保 Nacos 已启动
- 检查服务配置中的 Sentinel Dashboard 地址

### RabbitMQ 队列未创建
- 检查 rabbitmq-init 服务日志
- 手动执行初始化脚本

### 服务无法注册到 Nacos
- 检查服务配置中的 Nacos 地址
- 确认命名空间配置正确 (rag-system)

## 维护操作

### 清理所有数据

```bash
docker-compose down -v
```

### 重新初始化配置

```bash
docker-compose restart nacos-init rabbitmq-init
```

### 扩展服务实例

```bash
docker-compose up -d --scale document-service=3
```

## 安全建议

生产环境部署时，请修改以下默认密码：

- Nacos: nacos / nacos
- Sentinel: sentinel / sentinel
- RabbitMQ: admin / admin123
- PostgreSQL: postgres / postgres123
- Redis: redis123
- MinIO: admin / admin123456
- Grafana: admin / admin

## 性能调优

### JVM 参数

Java 服务的 JVM 参数可以通过环境变量 `JAVA_OPTS` 调整：

```yaml
environment:
  - JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

### 数据库连接池

在 common-config.yaml 中调整：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
```

### Redis 连接池

```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
```

## 监控指标

### 关键指标

1. **服务健康**: up{job="service-name"}
2. **QPS**: rate(http_requests_total[5m])
3. **响应时间**: histogram_quantile(0.95, http_request_duration_seconds_bucket)
4. **错误率**: rate(http_requests_total{status=~"5.."}[5m])
5. **CPU 使用率**: process_cpu_usage
6. **内存使用率**: jvm_memory_used_bytes / jvm_memory_max_bytes

### 告警规则

参见 `prometheus/alerting_rules.yml`

## 参考文档

- [Nacos 官方文档](https://nacos.io/zh-cn/docs/what-is-nacos.html)
- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/docs/introduction.html)
- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Zipkin 官方文档](https://zipkin.io/)
- [Prometheus 官方文档](https://prometheus.io/docs/)
- [Grafana 官方文档](https://grafana.com/docs/)
