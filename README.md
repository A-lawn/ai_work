# 智能运维问答助手 (RAG Ops QA Assistant)

企业级 RAG（检索增强生成）微服务应用系统，基于 Spring Cloud Alibaba 和 FastAPI 构建。

## 项目概述

智能运维问答助手是一个完整的微服务架构系统，旨在解决企业运维团队知识管理分散、故障排查效率低下的痛点。系统提供文档智能检索、多轮对话、知识库管理等核心功能，支持私有化部署。

## 技术栈

### 微服务基础设施
- **服务注册与发现**: Nacos Discovery
- **配置中心**: Nacos Config
- **API 网关**: Spring Cloud Gateway
- **熔断降级**: Sentinel（流量控制、熔断降级、系统保护）
- **链路追踪**: Spring Cloud Sleuth + Zipkin
- **服务监控**: Prometheus + Grafana
- **消息队列**: RabbitMQ

### Java 微服务 (Spring Boot 3.x)
- Gateway Service (API 网关服务) - 端口 8080
- Document Service (文档管理服务) - 端口 8081
- Session Service (会话管理服务) - 端口 8082
- Auth Service (认证授权服务) - 端口 8083
- Monitor Service (监控日志服务) - 端口 8084
- Config Service (配置管理服务) - 端口 8085

### Python 微服务 (FastAPI)
- Document Processing Service (文档处理服务) - 端口 9001
- RAG Query Service (RAG 查询服务) - 端口 9002
- Embedding Service (嵌入模型服务) - 端口 9003
- LLM Service (大模型服务) - 端口 9004

### 前端
- React 18 + TypeScript
- Ant Design
- Vite

### 数据存储
- PostgreSQL (关系数据库)
- Redis (缓存)
- ChromaDB (向量数据库)
- Elasticsearch (日志存储)
- MinIO (对象存储)

## 项目结构

```
rag-ops-qa-assistant/
├── java-services/              # Java 微服务
│   ├── gateway-service/        # API 网关
│   ├── document-service/       # 文档管理
│   ├── session-service/        # 会话管理
│   ├── auth-service/           # 认证授权
│   ├── monitor-service/        # 监控日志
│   └── config-service/         # 配置管理
├── python-services/            # Python 微服务
│   ├── document-processing-service/  # 文档处理
│   ├── rag-query-service/      # RAG 查询
│   ├── embedding-service/      # 嵌入模型
│   └── llm-service/            # 大模型
├── frontend/                   # 前端应用
├── infrastructure/             # 基础设施配置
│   ├── nacos/                  # Nacos 配置
│   ├── prometheus/             # Prometheus 配置
│   ├── grafana/                # Grafana 配置
│   └── postgres/               # PostgreSQL 初始化脚本
├── docker-compose.yml          # Docker Compose 配置
├── .env.example                # 环境变量模板
└── pom.xml                     # Maven 父 POM

```

## 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+
- Java 17+ (本地开发)
- Python 3.10+ (本地开发)
- Node.js 18+ (本地开发)
- Maven 3.8+ (本地开发)

### 环境配置

1. 复制环境变量模板：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，配置必要的环境变量（特别是 OpenAI API Key）：
```bash
OPENAI_API_KEY=sk-your-api-key-here
```

### 使用 Docker Compose 启动

启动所有服务：
```bash
docker-compose up -d
```

查看服务状态：
```bash
docker-compose ps
```

查看服务日志：
```bash
docker-compose logs -f [service-name]
```

停止所有服务：
```bash
docker-compose down
```

### 服务访问地址

#### 应用服务
- 前端应用: http://localhost
- API 网关: http://localhost:8080

#### 基础设施
- Nacos 控制台: http://localhost:8848/nacos (用户名/密码: nacos/nacos)
- Sentinel 控制台: http://localhost:8858 (用户名/密码: sentinel/sentinel)
- RabbitMQ 管理界面: http://localhost:15672 (用户名/密码: admin/admin123)
- Zipkin 链路追踪: http://localhost:9411
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (用户名/密码: admin/admin)
- MinIO 控制台: http://localhost:9001 (用户名/密码: admin/admin123456)
- Elasticsearch: http://localhost:9200

## 本地开发

### Java 服务开发

1. 构建父项目：
```bash
mvn clean install
```

2. 启动单个服务：
```bash
cd java-services/gateway-service
mvn spring-boot:run
```

### Python 服务开发

1. 创建虚拟环境：
```bash
cd python-services/document-processing-service
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
```

2. 安装依赖：
```bash
pip install -r requirements.txt
```

3. 启动服务：
```bash
uvicorn app.main:app --reload --port 9001
```

### 前端开发

1. 安装依赖：
```bash
cd frontend
npm install
```

2. 启动开发服务器：
```bash
npm run dev
```

## 配置说明

### Nacos 配置

系统使用 Nacos 作为配置中心，配置文件位于 `infrastructure/nacos/` 目录。

主要配置：
- `common-config.yaml`: 公共配置（所有服务共享）
- `sentinel-rules/`: Sentinel 流控和熔断规则

### Sentinel 规则配置

Sentinel 规则存储在 Nacos 中，可通过 Sentinel Dashboard 动态修改：

1. 访问 Sentinel Dashboard: http://localhost:8858
2. 选择对应的服务
3. 配置流控规则、熔断规则、系统保护规则
4. 规则会自动持久化到 Nacos

### 监控配置

Prometheus 配置文件: `infrastructure/prometheus/prometheus.yml`
告警规则: `infrastructure/prometheus/alerting_rules.yml`

## 核心功能

1. **文档管理**
   - 支持 PDF、DOCX、TXT、MD 格式
   - 文档上传、删除、查询
   - 批量文档处理

2. **智能问答**
   - 基于 RAG 的智能检索
   - 多轮对话支持
   - 流式响应
   - 引用来源展示

3. **知识库管理**
   - 文档向量化存储
   - 向量检索
   - 相似度匹配

4. **系统配置**
   - LLM 后端配置（OpenAI、Azure、本地模型）
   - 检索参数配置
   - 动态配置更新

5. **监控告警**
   - 服务健康监控
   - 性能指标采集
   - 链路追踪
   - 日志聚合

## 微服务治理

### 服务注册与发现
所有服务自动注册到 Nacos，支持服务发现和负载均衡。

### 熔断降级
使用 Sentinel 实现熔断降级，保护系统稳定性：
- 流量控制（QPS 限流）
- 熔断降级（异常比例、慢调用比例）
- 系统保护（CPU、Load、RT、线程数）

### 链路追踪
使用 Sleuth + Zipkin 实现分布式链路追踪，可视化服务调用链路。

### 配置管理
使用 Nacos Config 统一管理配置，支持配置动态刷新。

## API 文档

启动服务后，访问以下地址查看 API 文档：

- Gateway Service: http://localhost:8080/swagger-ui.html
- Document Service: http://localhost:8081/swagger-ui.html
- Python Services: http://localhost:9001/docs (FastAPI 自动生成)

## 文档

### 完整文档

- [架构设计文档](docs/ARCHITECTURE.md) - 微服务架构设计、服务拆分、通信方式
- [安装部署指南](docs/INSTALLATION.md) - 本地开发环境、Docker Compose 部署、生产环境部署
- [API 文档](docs/API.md) - RESTful API 接口说明、请求响应示例
- [配置说明](docs/CONFIG.md) - Nacos 配置、环境变量、Sentinel 规则
- [运维手册](docs/OPERATIONS.md) - 服务监控、日志查看、故障排查、数据备份
- [常见问题](docs/FAQ.md) - 常见问题解答、故障排查流程
- [开发指南](docs/DEVELOPMENT.md) - 如何添加新服务、如何调试、代码规范

### 快速指南

- [快速开始](QUICK_START.md) - 5 分钟快速启动系统
- [查询流程](QUERY_FLOW_QUICKSTART.md) - 端到端查询流程说明
- [批量处理](BATCH_PROCESSING_QUICKSTART.md) - 批量文档处理功能
- [中文支持](frontend/CHINESE_SUPPORT_QUICKSTART.md) - 中文界面支持说明

## 系统验证

### 快速验证

启动系统后，运行快速验证脚本：

```bash
chmod +x quick-verify.sh
./quick-verify.sh
```

这将快速检查所有关键服务的健康状态。

### 完整端到端验证

运行完整的端到端验证测试：

```bash
chmod +x verify-e2e.sh
./verify-e2e.sh
```

这将执行 19 个测试类别，全面验证系统功能。

### Python 集成测试

运行 Python 集成测试套件：

```bash
cd tests/integration
pytest test_e2e_flow.py -v -s
```

### 验证文档

- [端到端验证指南](E2E_VERIFICATION_GUIDE.md) - 详细的验证步骤和程序
- [验证检查清单](VERIFICATION_CHECKLIST.md) - 系统化的验证检查清单
- [任务 20 实施总结](TASK_20_E2E_VERIFICATION_SUMMARY.md) - 验证框架说明

## 故障排查

### 服务无法启动
1. 检查 Nacos 是否正常运行
2. 检查数据库连接配置
3. 查看服务日志: `docker-compose logs [service-name]`

### Sentinel 规则不生效
1. 确认 Sentinel Dashboard 已启动
2. 检查服务是否连接到 Dashboard
3. 验证规则是否正确配置在 Nacos

### 向量检索失败
1. 检查 ChromaDB 是否正常运行
2. 确认文档已成功处理并向量化
3. 检查 Embedding Service 是否可用

更多故障排查信息，请参考 [运维手册](docs/OPERATIONS.md) 和 [常见问题](docs/FAQ.md)。

## 贡献指南

欢迎提交 Issue 和 Pull Request！

开发前请阅读 [开发指南](docs/DEVELOPMENT.md)。

## 许可证

MIT License

## 联系方式

如有问题，请：
1. 查看 [文档](#文档)
2. 查看 [常见问题](docs/FAQ.md)
3. 提交 Issue
4. 联系项目维护者
