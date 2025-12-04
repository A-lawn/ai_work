# RAG Query Service - 快速开始指南

## 前置条件

在启动 RAG Query Service 之前，确保以下服务已运行：

1. **ChromaDB** (端口 8000)
2. **Redis** (端口 6379)
3. **Nacos** (端口 8848) - 可选，用于服务发现
4. **Embedding Service** (端口 9003) - 必需
5. **LLM Service** (端口 9004) - 必需

## 快速启动步骤

### 1. 安装依赖

```bash
cd python-services/rag-query-service
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，配置必要的参数：

```bash
# 最小配置
CHROMA_HOST=localhost
CHROMA_PORT=8000
REDIS_URL=redis://localhost:6379/0
EMBEDDING_SERVICE_URL=http://localhost:9003
LLM_SERVICE_URL=http://localhost:9004
```

### 3. 启动服务

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 9002
```

### 4. 验证服务

打开浏览器访问：

- API 文档: http://localhost:9002/docs
- 健康检查: http://localhost:9002/health
- Prometheus 指标: http://localhost:9002/metrics

## 测试 API

### 测试查询接口

```bash
curl -X POST http://localhost:9002/api/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "如何重启 Nginx 服务？",
    "top_k": 5,
    "similarity_threshold": 0.7
  }'
```

### 测试流式查询

```bash
curl -X POST http://localhost:9002/api/query/stream \
  -H "Content-Type: application/json" \
  -d '{
    "question": "如何重启 Nginx 服务？",
    "stream": true
  }'
```

### 测试健康检查

```bash
curl http://localhost:9002/api/health
```

## Docker 部署

### 构建镜像

```bash
docker build -t rag-query-service:latest .
```

### 运行容器

```bash
docker run -d \
  --name rag-query-service \
  -p 9002:9002 \
  -e CHROMA_HOST=host.docker.internal \
  -e CHROMA_PORT=8000 \
  -e REDIS_URL=redis://host.docker.internal:6379/0 \
  -e EMBEDDING_SERVICE_URL=http://host.docker.internal:9003 \
  -e LLM_SERVICE_URL=http://host.docker.internal:9004 \
  rag-query-service:latest
```

## 使用 Docker Compose

在项目根目录的 `docker-compose.yml` 中已包含 RAG Query Service 配置：

```bash
# 启动所有服务
docker-compose up -d

# 查看 RAG Query Service 日志
docker-compose logs -f rag-query-service

# 停止服务
docker-compose down
```

## 常见问题

### Q: ChromaDB 连接失败

**A**: 确保 ChromaDB 服务正在运行，并且集合已创建：

```bash
# 检查 ChromaDB
curl http://localhost:8000/api/v1/heartbeat

# 如果需要，手动创建集合
# 通过 Document Processing Service 上传文档会自动创建集合
```

### Q: 查询返回"未找到相关信息"

**A**: 确保知识库中有文档：

1. 使用 Document Service 上传文档
2. 等待文档处理完成
3. 检查 ChromaDB 集合中的文档数量

```bash
curl http://localhost:9002/api/health
# 查看 collection_count 字段
```

### Q: LLM Service 调用失败

**A**: 检查 LLM Service 是否正常运行：

```bash
curl http://localhost:9004/health
```

### Q: 缓存不工作

**A**: 检查 Redis 连接：

```bash
redis-cli ping
# 应该返回 PONG
```

## 下一步

1. 通过 Document Service 上传运维文档
2. 使用 RAG Query Service 进行智能问答
3. 集成到前端应用
4. 配置监控和告警

## 相关文档

- [README.md](README.md) - 完整文档
- [API 文档](http://localhost:9002/docs) - 交互式 API 文档
- [Document Processing Service](../document-processing-service/README.md)
- [Embedding Service](../embedding-service/README.md)
- [LLM Service](../llm-service/README.md)
