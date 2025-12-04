# Embedding Service - 快速开始指南

## 5 分钟快速启动

### 步骤 1: 安装依赖

```bash
cd python-services/embedding-service
pip install -r requirements.txt
```

### 步骤 2: 配置环境

```bash
# 复制配置文件
cp .env.example .env

# 编辑 .env 文件，设置 OpenAI API Key
# OPENAI_API_KEY=your-api-key-here
```

### 步骤 3: 启动服务

```bash
uvicorn app.main:app --reload --port 9003
```

### 步骤 4: 测试服务

```bash
# 健康检查
curl http://localhost:9003/health

# 生成嵌入向量
curl -X POST http://localhost:9003/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "texts": ["如何重启 Nginx 服务？"],
    "use_cache": true
  }'
```

## 常见使用场景

### 场景 1: 批量文本向量化

```bash
curl -X POST http://localhost:9003/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "texts": [
      "如何重启 Nginx 服务？",
      "MySQL 数据库连接失败",
      "Redis 缓存清理方法"
    ],
    "use_cache": true
  }'
```

### 场景 2: 切换到本地模型（无需 API Key）

```bash
# 切换到本地 BGE 模型
curl -X POST http://localhost:9003/api/model/switch \
  -H "Content-Type: application/json" \
  -d '{
    "use_local_model": true,
    "model_name": "BAAI/bge-base-zh-v1.5"
  }'

# 再次生成向量（使用本地模型）
curl -X POST http://localhost:9003/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "texts": ["测试本地模型"],
    "use_cache": false
  }'
```

### 场景 3: 清空缓存

```bash
curl -X POST http://localhost:9003/api/cache/clear
```

## Docker 快速启动

```bash
# 构建镜像
docker build -t embedding-service .

# 启动容器
docker run -d \
  --name embedding-service \
  -p 9003:9003 \
  -e OPENAI_API_KEY=your-key \
  embedding-service
```

## 访问 API 文档

启动服务后，访问交互式 API 文档：

- Swagger UI: http://localhost:9003/docs
- ReDoc: http://localhost:9003/redoc

## 下一步

- 查看完整 [README.md](README.md) 了解详细功能
- 查看 [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) 了解实现细节
- 集成到其他服务（Document Processing Service、RAG Query Service）
