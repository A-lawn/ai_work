# 项目结构说明

## 目录树

```
rag-ops-qa-assistant/
│
├── .env.example                          # 环境变量模板
├── .gitignore                            # Git 忽略文件
├── docker-compose.yml                    # Docker Compose 配置
├── pom.xml                               # Maven 父 POM
├── README.md                             # 项目说明文档
├── SETUP.md                              # 初始化完成说明
├── start.sh                              # 启动脚本
├── stop.sh                               # 停止脚本
│
├── java-services/                        # Java 微服务目录
│   ├── gateway-service/                  # API 网关服务 (8080)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/
│   │       ├── java/com/rag/ops/gateway/
│   │       │   └── GatewayServiceApplication.java
│   │       └── resources/
│   │           ├── bootstrap.yml
│   │           └── application.yml
│   │
│   ├── document-service/                 # 文档管理服务 (8081)
│   │   ├── Dockerfile
│   │   ├── pom.xml
│   │   └── src/main/resources/
│   │       ├── bootstrap.yml
│   │       └── application.yml
│   │
│   ├── session-service/                  # 会话管理服务 (8082)
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── auth-service/                     # 认证授权服务 (8083)
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── monitor-service/                  # 监控日志服务 (8084)
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   └── config-service/                   # 配置管理服务 (8085)
│       ├── Dockerfile
│       └── pom.xml
│
├── python-services/                      # Python 微服务目录
│   ├── document-processing-service/      # 文档处理服务 (9001)
│   │   ├── Dockerfile
│   │   ├── requirements.txt
│   │   └── app/
│   │       ├── __init__.py
│   │       └── main.py
│   │
│   ├── rag-query-service/                # RAG 查询服务 (9002)
│   │   ├── Dockerfile
│   │   └── requirements.txt
│   │
│   ├── embedding-service/                # 嵌入模型服务 (9003)
│   │   ├── Dockerfile
│   │   └── requirements.txt
│   │
│   └── llm-service/                      # 大模型服务 (9004)
│       ├── Dockerfile
│       └── requirements.txt
│
├── frontend/                             # 前端应用
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── tsconfig.json
│   ├── tsconfig.node.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── App.css
│       └── index.css
│
└── infrastructure/                       # 基础设施配置
    ├── nacos/                            # Nacos 配置
    │   ├── common-config.yaml
    │   └── sentinel-rules/
    │       ├── gateway-service-flow-rules.json
    │       ├── gateway-service-degrade-rules.json
    │       ├── gateway-service-system-rules.json
    │       ├── document-service-flow-rules.json
    │       └── document-service-degrade-rules.json
    │
    ├── prometheus/                       # Prometheus 配置
    │   ├── prometheus.yml
    │   └── alerting_rules.yml
    │
    └── postgres/                         # PostgreSQL 配置
        └── init.sql

```

## 服务端口分配

### Java 微服务
- 8080: Gateway Service (API 网关)
- 8081: Document Service (文档管理)
- 8082: Session Service (会话管理)
- 8083: Auth Service (认证授权)
- 8084: Monitor Service (监控日志)
- 8085: Config Service (配置管理)

### Python 微服务
- 9001: Document Processing Service (文档处理)
- 9002: RAG Query Service (RAG 查询)
- 9003: Embedding Service (嵌入模型)
- 9004: LLM Service (大模型)

### 基础设施
- 80: Frontend (前端应用)
- 5432: PostgreSQL
- 6379: Redis
- 8001: ChromaDB
- 8848: Nacos
- 8858: Sentinel Dashboard
- 9000: MinIO API
- 9001: MinIO Console
- 9090: Prometheus
- 9200: Elasticsearch
- 9411: Zipkin
- 15672: RabbitMQ Management
- 3001: Grafana

## 关键配置文件

### Maven 依赖管理
- `pom.xml`: 父 POM，统一管理版本
- Spring Boot: 3.1.5
- Spring Cloud: 2022.0.4
- Spring Cloud Alibaba: 2022.0.0.0

### Spring Cloud Alibaba 组件
- Nacos Discovery: 服务注册发现
- Nacos Config: 配置中心
- Sentinel: 流控熔断降级
- Sentinel Nacos DataSource: 规则持久化
- OpenFeign: 服务调用
- LoadBalancer: 负载均衡
- Sleuth: 链路追踪
- Zipkin: 链路追踪可视化

### Python 依赖
- FastAPI: Web 框架
- Uvicorn: ASGI 服务器
- Nacos SDK: 服务注册
- OpenTelemetry: 链路追踪
- Prometheus Client: 监控指标

### 前端技术栈
- React 18
- TypeScript
- Ant Design
- Vite
- Axios
- React Router

## Docker Compose 服务

### 基础设施服务 (11 个)
1. Nacos - 服务注册和配置中心
2. Sentinel Dashboard - 流控熔断控制台
3. RabbitMQ - 消息队列
4. Zipkin - 链路追踪
5. Prometheus - 指标采集
6. Grafana - 可视化监控
7. PostgreSQL - 关系数据库
8. Redis - 缓存
9. ChromaDB - 向量数据库
10. Elasticsearch - 日志存储
11. MinIO - 对象存储

### 应用服务 (11 个)
- 6 个 Java 微服务
- 4 个 Python 微服务
- 1 个前端服务

## 配置文件说明

### 环境变量 (.env)
包含所有服务的配置参数：
- 数据库连接
- Redis 配置
- MinIO 配置
- Nacos 配置
- OpenAI API Key
- Azure OpenAI 配置
- 系统参数

### Java 服务配置
- `bootstrap.yml`: Nacos 连接配置
- `application.yml`: 服务详细配置
  - 数据库连接
  - Redis 配置
  - Sentinel 配置
  - Zipkin 配置
  - Actuator 配置

### Nacos 配置
- `common-config.yaml`: 所有服务共享的公共配置
- Sentinel 规则: 流控、熔断、系统保护规则

### Prometheus 配置
- `prometheus.yml`: 监控采集配置
- `alerting_rules.yml`: 告警规则

### PostgreSQL 初始化
- `init.sql`: 数据库表结构、索引、触发器

## 启动流程

1. 配置环境变量: `cp .env.example .env`
2. 编辑 `.env` 文件，配置必要参数
3. 启动服务: `./start.sh`
4. 访问服务控制台验证

## 开发流程

1. 本地开发时，可以只启动必要的基础设施
2. 在 IDE 中运行单个微服务
3. 使用 Docker Compose 启动依赖服务
4. 通过 Nacos 查看服务注册情况
5. 通过 Sentinel Dashboard 配置流控规则
6. 通过 Zipkin 查看调用链路
7. 通过 Grafana 查看监控指标

## 下一步

参考 `tasks.md` 继续实现各个微服务的业务逻辑。
