# 微服务架构设计文档

## 1. 架构概述

智能运维问答助手采用完整的微服务架构，将系统拆分为多个独立的服务单元。系统使用 Spring Cloud Alibaba 生态（Java）和 FastAPI（Python）构建，支持服务注册发现、配置中心、API 网关、链路追踪等企业级微服务特性。

### 1.1 设计原则

- **单一职责**: 每个微服务专注于特定的业务领域
- **服务自治**: 服务独立部署、独立扩展、独立升级
- **去中心化**: 数据去中心化，每个服务管理自己的数据
- **容错设计**: 熔断降级、限流保护、优雅降级
- **可观测性**: 完整的日志、监控、链路追踪体系

### 1.2 技术选型理由

**Spring Cloud Alibaba vs Spring Cloud Netflix**
- Nacos 提供更好的配置管理和服务发现能力
- Sentinel 提供更强大的流量控制和熔断降级功能
- 更好的中文文档和社区支持

**FastAPI vs Flask**
- 原生异步支持，性能更好
- 自动生成 OpenAPI 文档
- 类型提示和数据验证

**ChromaDB vs Milvus/Qdrant**
- 轻量级，易于部署
- Python 原生支持
- 适合中小规模向量数据

## 2. 微服务拆分

### 2.1 Java 微服务

#### Gateway Service (API 网关服务)
**职责**: 
- 统一入口，路由转发到各个微服务
- 全局认证鉴权（JWT 验证、API Key 验证）
- 限流熔断（基于 Sentinel）
- 请求日志记录和链路追踪
- 跨域处理（CORS）

**端口**: 8080

**关键技术**:
- Spring Cloud Gateway
- Sentinel (流控、熔断)
- Spring Cloud Sleuth (链路追踪)

#### Document Service (文档管理服务)
**职责**:
- 文档元数据的 CRUD 操作
- 文档上传接收和文件存储（MinIO）
- 调用 Python 文档处理服务
- 文档状态管理和更新
- 批量文档任务管理

**端口**: 8081

**数据库**: PostgreSQL (documents 表)

**关键技术**:
- Spring Data JPA
- MinIO Client
- OpenFeign (服务间调用)
- RabbitMQ (异步消息)

#### Session Service (会话管理服务)
**职责**:
- 会话创建和管理
- 多轮对话历史存储
- 会话上下文维护（滑动窗口）
- 会话缓存（Redis）

**端口**: 8082

**数据库**: PostgreSQL (sessions, messages 表)

**关键技术**:
- Spring Data JPA
- Spring Cache + Redis
- OpenFeign (调用 RAG Query Service)

#### Auth Service (认证授权服务)
**职责**:
- 用户认证（JWT）
- API Key 管理和验证
- 权限控制（RBAC）
- Token 刷新和过期管理

**端口**: 8083

**数据库**: PostgreSQL (users, api_keys, roles 表)

**关键技术**:
- Spring Security
- JWT (io.jsonwebtoken)
- BCrypt (密码加密)

#### Monitor Service (监控日志服务)
**职责**:
- 操作日志收集和存储
- 性能指标统计
- 系统监控数据聚合
- 日志查询接口（Elasticsearch）
- 告警规则管理

**端口**: 8084

**数据库**: PostgreSQL + Elasticsearch

**关键技术**:
- Spring Data JPA
- Elasticsearch Client
- Micrometer (指标采集)
- Prometheus (指标暴露)

#### Config Service (配置管理服务)
**职责**:
- 系统配置管理
- 动态配置更新
- 配置版本控制
- 配置同步到各服务

**端口**: 8085

**数据库**: PostgreSQL (configs 表)

**关键技术**:
- Spring Data JPA
- Nacos Config Client
- OpenFeign (调用 LLM Service 测试连接)

### 2.2 Python 微服务

#### Document Processing Service (文档处理服务)
**职责**:
- 文档解析（PDF、DOCX、TXT、MD）
- 文本提取和清洗
- 文本分块（RecursiveCharacterTextSplitter）
- 调用 Embedding Service 生成向量
- 向量存储到 ChromaDB
- RabbitMQ 消费者（异步处理）
- Celery 任务（批量处理）

**端口**: 9001

**关键技术**:
- FastAPI
- LangChain (文本分块)
- PyPDF2, python-docx (文档解析)
- ChromaDB Client
- RabbitMQ Consumer
- Celery + Redis

#### RAG Query Service (RAG 查询服务)
**职责**:
- 接收查询请求
- 查询向量化（调用 Embedding Service）
- 向量检索（ChromaDB 相似度搜索）
- 检索结果重排序
- 调用 LLM Service 生成答案
- 返回答案和引用来源
- 查询结果缓存（Redis）

**端口**: 9002

**关键技术**:
- FastAPI
- ChromaDB Client
- Redis (缓存)
- HTTP Client (调用其他服务)

#### Embedding Service (嵌入模型服务)
**职责**:
- 文本向量化
- 批量嵌入生成
- 支持多种嵌入模型（OpenAI、BGE）
- 向量缓存（Redis）

**端口**: 9003

**关键技术**:
- FastAPI
- OpenAI API
- Sentence Transformers (本地模型)
- Redis (缓存)

#### LLM Service (大模型服务)
**职责**:
- LLM 调用封装
- 多模型适配器（OpenAI、Azure、本地）
- 流式响应支持
- Token 统计和成本计算
- 提示词模板管理

**端口**: 9004

**关键技术**:
- FastAPI
- OpenAI API
- Azure OpenAI API
- StreamingResponse (流式输出)

## 3. 服务间通信

### 3.1 同步调用（REST API）

**调用链路**:
```
Gateway → Document Service → Document Processing Service
Gateway → Session Service → RAG Query Service → Embedding Service
                                              → LLM Service
Gateway → Auth Service
Gateway → Config Service → LLM Service
Gateway → Monitor Service
```

**技术实现**:
- Java 服务间: OpenFeign (声明式 HTTP 客户端)
- Java → Python: OpenFeign + HTTP
- Python → Python: httpx (异步 HTTP 客户端)

### 3.2 异步通信（消息队列）

**消息流**:
```
Document Service → [RabbitMQ: document.processing] → Document Processing Service
                                                    (文档处理完成事件)

Document Service → [RabbitMQ: batch.processing] → Celery Worker
                                                  (批量文档处理)
```

**技术实现**:
- RabbitMQ (消息代理)
- Spring AMQP (Java 生产者/消费者)
- aio-pika (Python 异步消费者)
- Celery (Python 异步任务队列)

### 3.3 服务发现

**注册流程**:
1. 服务启动时自动注册到 Nacos
2. 定期发送心跳保持注册状态
3. 服务下线时自动注销

**发现流程**:
1. Gateway 从 Nacos 获取服务列表
2. 使用 LoadBalancer 进行客户端负载均衡
3. 动态感知服务实例变化

**技术实现**:
- Nacos Discovery (服务注册中心)
- Spring Cloud LoadBalancer (负载均衡)
- nacos-sdk-python (Python 服务注册)

## 4. 数据架构

### 4.1 关系数据库 (PostgreSQL)

**documents 表** (Document Service)
- 存储文档元数据
- 字段: id, filename, file_type, file_size, upload_time, chunk_count, status, metadata

**sessions 表** (Session Service)
- 存储会话信息
- 字段: id, user_id, created_at, updated_at, metadata

**messages 表** (Session Service)
- 存储对话消息
- 字段: id, session_id, role, content, timestamp, metadata

**users 表** (Auth Service)
- 存储用户信息
- 字段: id, username, password_hash, email, created_at

**api_keys 表** (Auth Service)
- 存储 API Key
- 字段: id, key_hash, name, created_at, expires_at, is_active

**operation_logs 表** (Monitor Service)
- 存储操作日志
- 字段: id, operation_type, user_id, resource_id, details, timestamp

**performance_metrics 表** (Monitor Service)
- 存储性能指标
- 字段: id, metric_type, metric_value, timestamp, metadata

**system_configs 表** (Config Service)
- 存储系统配置
- 字段: id, config_key, config_value, description, updated_at

### 4.2 向量数据库 (ChromaDB)

**Collection**: ops_knowledge_base

**文档块元数据**:
- document_id: 文档 ID
- filename: 文件名
- chunk_index: 块索引
- chunk_text: 文本内容
- page_number: 页码（如果适用）
- section: 章节信息
- created_at: 创建时间

### 4.3 缓存 (Redis)

**用途**:
- 会话缓存 (Session Service)
- 查询结果缓存 (RAG Query Service)
- 向量缓存 (Embedding Service)
- Celery 任务队列和结果存储

**Key 设计**:
- `session:{session_id}`: 会话数据
- `query:{query_hash}`: 查询结果
- `embedding:{text_hash}`: 向量缓存
- `celery:*`: Celery 任务数据

### 4.4 对象存储 (MinIO)

**Bucket**: documents

**存储内容**:
- 原始文档文件
- 文件路径: `{document_id}/{filename}`

### 4.5 日志存储 (Elasticsearch)

**Index**: operation-logs-*

**文档结构**:
- timestamp: 时间戳
- level: 日志级别
- service: 服务名称
- message: 日志消息
- trace_id: 链路追踪 ID
- metadata: 额外元数据

## 5. 安全架构

### 5.1 认证机制

**JWT 认证**:
- 用户登录后获取 JWT Token
- Token 包含用户信息和权限
- Token 有效期: 24 小时
- 支持 Token 刷新

**API Key 认证**:
- 用于服务间调用和第三方集成
- Header: `X-API-Key`
- Key 使用 SHA-256 哈希存储
- 支持 Key 过期时间设置

### 5.2 授权机制

**网关层鉴权**:
- Gateway 统一验证 Token/API Key
- 白名单路径（健康检查、登录接口）
- 黑名单 IP 过滤

**服务层鉴权**:
- 基于角色的访问控制（RBAC）
- 资源级别权限控制

### 5.3 数据安全

**传输安全**:
- HTTPS/TLS 加密
- 服务间通信加密（可选）

**存储安全**:
- 密码使用 BCrypt 加密
- API Key 使用 SHA-256 哈希
- 敏感配置加密存储

**输入验证**:
- 文档上传大小限制: 50MB
- 文件类型白名单验证
- SQL 注入防护（使用 ORM）
- XSS 防护（前端输入验证）

## 6. 可观测性架构

### 6.1 日志系统

**日志级别**:
- ERROR: 错误日志
- WARN: 警告日志
- INFO: 信息日志
- DEBUG: 调试日志

**日志格式**:
```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "level": "INFO",
  "service": "gateway-service",
  "trace_id": "abc123",
  "span_id": "def456",
  "message": "Request processed",
  "metadata": {}
}
```

**日志收集**:
- 应用日志 → Elasticsearch
- 通过 Monitor Service 统一收集
- 支持日志查询和分析

### 6.2 监控系统

**指标采集**:
- Micrometer (Java 指标)
- Prometheus Client (Python 指标)
- 自定义业务指标

**监控指标**:
- 系统指标: CPU、内存、磁盘、网络
- JVM 指标: 堆内存、GC、线程
- 应用指标: QPS、响应时间、错误率
- 业务指标: 文档数、查询数、Token 消耗

**可视化**:
- Grafana 仪表盘
- 实时监控大屏
- 告警通知

### 6.3 链路追踪

**追踪系统**: Sleuth + Zipkin

**追踪信息**:
- Trace ID: 全局唯一标识
- Span ID: 单个操作标识
- Parent Span ID: 父操作标识
- Service Name: 服务名称
- Duration: 操作耗时

**追踪链路示例**:
```
Gateway (Span 1)
  → Document Service (Span 2)
    → Document Processing Service (Span 3)
      → Embedding Service (Span 4)
      → ChromaDB (Span 5)
```

### 6.4 告警系统

**告警规则**:
- 服务下线告警
- 高错误率告警（错误率 > 5%）
- 慢响应告警（P95 > 3s）
- 资源告警（CPU > 80%, 内存 > 80%）
- 限流熔断告警

**告警渠道**:
- Prometheus Alertmanager
- 邮件通知
- Webhook（可集成钉钉、企业微信）

## 7. 容错设计

### 7.1 熔断降级 (Sentinel)

**熔断策略**:
- 异常比例: 异常比例 > 50% 触发熔断
- 慢调用比例: 慢调用比例 > 50% 触发熔断
- 异常数: 异常数 > 10 触发熔断

**降级策略**:
- 返回默认值
- 返回缓存数据
- 返回友好错误信息

**熔断恢复**:
- 半开状态: 允许少量请求通过
- 成功率达标: 恢复正常状态
- 失败继续熔断

### 7.2 限流保护 (Sentinel)

**限流维度**:
- QPS 限流: 每秒请求数限制
- 并发线程数限流: 并发线程数限制
- 热点参数限流: 针对特定参数限流

**限流策略**:
- 快速失败: 直接拒绝
- 排队等待: 匀速排队
- 预热启动: 逐步增加流量

### 7.3 重试机制

**重试策略**:
- 最大重试次数: 3 次
- 重试间隔: 指数退避（1s, 2s, 4s）
- 幂等性保证: 只对幂等操作重试

**适用场景**:
- 网络抖动
- 服务临时不可用
- 超时重试

### 7.4 超时控制

**超时配置**:
- 连接超时: 5 秒
- 读取超时: 30 秒
- LLM 调用超时: 60 秒
- 文档处理超时: 300 秒

### 7.5 优雅降级

**降级场景**:
- Embedding Service 不可用: 使用缓存或返回错误
- LLM Service 不可用: 返回检索结果，不生成答案
- ChromaDB 不可用: 返回友好错误信息

## 8. 扩展性设计

### 8.1 水平扩展

**无状态服务**:
- Gateway Service: 支持多实例部署
- Document Service: 支持多实例部署
- Session Service: 会话数据存储在 Redis
- RAG Query Service: 支持多实例部署

**有状态服务**:
- PostgreSQL: 主从复制
- Redis: 哨兵模式或集群模式
- ChromaDB: 分片存储（未来）

### 8.2 垂直扩展

**资源配置**:
- Java 服务: 调整 JVM 堆内存
- Python 服务: 调整 Worker 数量
- 数据库: 增加 CPU 和内存

### 8.3 服务拆分

**拆分原则**:
- 按业务领域拆分
- 按数据边界拆分
- 按团队组织拆分

**未来拆分方向**:
- 文档解析服务（从 Document Processing Service 拆分）
- 向量检索服务（从 RAG Query Service 拆分）
- 提示词管理服务（独立服务）

## 9. 部署架构

### 9.1 容器化部署

**Docker 镜像**:
- Java 服务: eclipse-temurin:17-jre-alpine
- Python 服务: python:3.10-slim
- 前端: nginx:alpine

**多阶段构建**:
- 构建阶段: 编译代码
- 运行阶段: 最小化镜像

### 9.2 编排部署

**Docker Compose**:
- 适用于开发和测试环境
- 单机部署
- 快速启动

**Kubernetes** (未来):
- 适用于生产环境
- 多节点部署
- 自动扩缩容
- 滚动更新

### 9.3 网络架构

**网络拓扑**:
```
Internet
  ↓
Nginx (HTTPS)
  ↓
Gateway Service
  ↓
Internal Network (Docker Network)
  ↓
Microservices + Infrastructure
```

**网络隔离**:
- 前端网络: 对外暴露
- 服务网络: 内部通信
- 数据网络: 数据库访问

## 10. 技术债务和改进方向

### 10.1 当前限制

- ChromaDB 单机部署，不支持分布式
- 没有实现服务网格（Service Mesh）
- 配置管理依赖 Nacos，没有多配置中心支持
- 日志收集依赖应用主动上报

### 10.2 改进方向

**短期**:
- 完善单元测试和集成测试
- 优化 Docker 镜像大小
- 增加更多监控指标
- 完善 API 文档

**中期**:
- 引入服务网格（Istio）
- 实现配置加密
- 增加灰度发布能力
- 优化向量检索性能

**长期**:
- 迁移到 Kubernetes
- 实现多租户隔离
- 支持向量数据库集群
- 引入 AI 模型管理平台

## 11. 参考资料

- [Spring Cloud Alibaba 官方文档](https://spring-cloud-alibaba-group.github.io/github-pages/2022/zh-cn/index.html)
- [FastAPI 官方文档](https://fastapi.tiangolo.com/)
- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/)
- [Nacos 官方文档](https://nacos.io/zh-cn/)
- [ChromaDB 官方文档](https://docs.trychroma.com/)
- [LangChain 官方文档](https://python.langchain.com/)
