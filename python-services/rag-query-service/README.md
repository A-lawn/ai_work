# RAG Query Service

RAG 查询服务 - 提供智能问答功能，整合向量检索和大语言模型生成。

## 功能特性

- **向量检索**: 从 ChromaDB 检索相关文档块
- **RAG 查询**: 基于检索到的上下文生成答案
- **流式响应**: 支持流式生成答案
- **查询缓存**: 使用 Redis 缓存查询结果
- **服务发现**: 集成 Nacos 服务注册与发现
- **监控指标**: 提供 Prometheus 指标端点
- **链路追踪**: 集成 Zipkin 链路追踪

## 技术栈

- **框架**: FastAPI
- **向量数据库**: ChromaDB
- **缓存**: Redis
- **服务发现**: Nacos
- **监控**: Prometheus
- **链路追踪**: Zipkin

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境变量

复制 `.env.example` 到 `.env` 并修改配置：

```bash
cp .env.example .env
```

### 3. 启动服务

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 9002
```

或使用 Docker：

```bash
docker build -t rag-query-service .
docker run -p 9002:9002 --env-file .env rag-query-service
```

## API 接口

### 1. RAG 查询（同步模式）

```bash
POST /api/query
Content-Type: application/json

{
  "question": "如何重启 Nginx 服务？",
  "session_history": [
    {"role": "user", "content": "之前的问题"},
    {"role": "assistant", "content": "之前的回答"}
  ],
  "top_k": 5,
  "similarity_threshold": 0.7
}
```

响应：

```json
{
  "answer": "要重启 Nginx 服务，可以使用以下命令...",
  "sources": [
    {
      "chunk_text": "文档内容...",
      "similarity_score": 0.85,
      "document_id": "doc-123",
      "document_name": "nginx-manual.pdf",
      "chunk_index": 5,
      "page_number": 10,
      "section": "服务管理"
    }
  ],
  "query_time": 2.5
}
```

### 2. RAG 查询（流式模式）

```bash
POST /api/query/stream
Content-Type: application/json

{
  "question": "如何重启 Nginx 服务？",
  "stream": true
}
```

响应：Server-Sent Events 流

### 3. 健康检查

```bash
GET /api/health
```

响应：

```json
{
  "status": "healthy",
  "chroma_connected": true,
  "redis_connected": true,
  "nacos_registered": true,
  "collection_count": 1250
}
```

### 4. Prometheus 指标

```bash
GET /metrics
```

## 配置说明

### 服务配置

- `SERVICE_NAME`: 服务名称（默认: rag-query-service）
- `SERVICE_PORT`: 服务端口（默认: 9002）
- `SERVICE_HOST`: 服务主机（默认: 0.0.0.0）

### ChromaDB 配置

- `CHROMA_HOST`: ChromaDB 主机地址
- `CHROMA_PORT`: ChromaDB 端口
- `CHROMA_COLLECTION_NAME`: 集合名称

### Redis 配置

- `REDIS_URL`: Redis 连接 URL
- `REDIS_CACHE_TTL`: 缓存过期时间（秒）

### 检索配置

- `TOP_K`: 返回的文档块数量（默认: 5）
- `SIMILARITY_THRESHOLD`: 相似度阈值（默认: 0.7）

### 服务依赖配置

- `EMBEDDING_SERVICE_NAME`: Embedding Service 名称
- `EMBEDDING_SERVICE_URL`: Embedding Service URL（可选）
- `LLM_SERVICE_NAME`: LLM Service 名称
- `LLM_SERVICE_URL`: LLM Service URL（可选）

## 监控指标

服务提供以下 Prometheus 指标：

- `rag_query_total`: RAG 查询总数
- `rag_query_duration_seconds`: RAG 查询耗时
- `rag_retrieval_duration_seconds`: 向量检索耗时
- `rag_llm_generation_duration_seconds`: LLM 生成耗时
- `rag_retrieval_results_count`: 检索返回的结果数量
- `rag_cache_hit_total`: 缓存命中次数
- `rag_cache_miss_total`: 缓存未命中次数
- `rag_chroma_connection_status`: ChromaDB 连接状态
- `rag_redis_connection_status`: Redis 连接状态
- `rag_nacos_registration_status`: Nacos 注册状态
- `rag_error_total`: 错误总数

## 开发指南

### 项目结构

```
rag-query-service/
├── app/
│   ├── __init__.py
│   ├── main.py              # 主应用入口
│   ├── config.py            # 配置管理
│   ├── models.py            # 数据模型
│   ├── routes.py            # API 路由
│   ├── query_engine.py      # RAG 查询引擎
│   ├── retriever.py         # 向量检索服务
│   ├── chroma_client.py     # ChromaDB 客户端
│   ├── embedding_client.py  # Embedding Service 客户端
│   ├── llm_client.py        # LLM Service 客户端
│   ├── redis_client.py      # Redis 缓存客户端
│   ├── nacos_client.py      # Nacos 服务注册
│   ├── metrics.py           # Prometheus 指标
│   └── logging_config.py    # 日志配置
├── requirements.txt
├── Dockerfile
├── .env.example
├── .gitignore
└── README.md
```

### 添加新功能

1. 在 `app/` 目录下创建新模块
2. 在 `routes.py` 中添加新的 API 端点
3. 更新 `models.py` 添加新的数据模型
4. 更新文档

## 故障排查

### ChromaDB 连接失败

检查 ChromaDB 服务是否运行：

```bash
curl http://localhost:8000/api/v1/heartbeat
```

### Redis 连接失败

检查 Redis 服务是否运行：

```bash
redis-cli ping
```

### 服务发现失败

检查 Nacos 服务是否运行：

```bash
curl http://localhost:8848/nacos/v1/ns/operator/metrics
```

### 查看日志

```bash
docker logs rag-query-service
```

## 许可证

MIT License
