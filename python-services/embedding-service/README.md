# Embedding Service - 嵌入模型服务

## 概述

Embedding Service 是 RAG 系统的嵌入模型服务，负责将文本转换为向量表示。支持 OpenAI Embedding API 和本地 BGE 模型，提供批量向量化、缓存加速、模型切换等功能。

## 功能特性

- ✅ **多模型支持**: OpenAI text-embedding-ada-002 / 本地 BGE 模型
- ✅ **批量处理**: 支持批量文本向量化，提高效率
- ✅ **缓存加速**: Redis 缓存向量结果，避免重复计算
- ✅ **模型切换**: 运行时动态切换嵌入模型
- ✅ **服务注册**: 自动注册到 Nacos 服务中心
- ✅ **监控指标**: Prometheus 指标暴露
- ✅ **链路追踪**: Zipkin 分布式追踪支持

## 技术栈

- **框架**: FastAPI (Python 3.10+)
- **嵌入模型**: OpenAI API / Sentence Transformers (BGE)
- **缓存**: Redis
- **服务发现**: Nacos
- **监控**: Prometheus + Zipkin

## 快速开始

### 1. 环境准备

```bash
# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt
```

### 2. 配置环境变量

```bash
# 复制配置模板
cp .env.example .env

# 编辑配置文件
# 必须配置 OPENAI_API_KEY（如果使用 OpenAI）
```

### 3. 启动服务

```bash
# 开发模式
uvicorn app.main:app --reload --port 9003

# 生产模式
uvicorn app.main:app --host 0.0.0.0 --port 9003 --workers 4
```

### 4. 验证服务

```bash
# 健康检查
curl http://localhost:9003/health

# 生成嵌入向量
curl -X POST http://localhost:9003/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "texts": ["如何重启 Nginx 服务？", "MySQL 连接失败"],
    "use_cache": true
  }'
```

## API 文档

### 1. 生成嵌入向量

**POST** `/api/embeddings`

批量将文本转换为向量表示。

**请求体**:
```json
{
  "texts": ["文本1", "文本2"],
  "model": "text-embedding-ada-002",  // 可选
  "use_cache": true
}
```

**响应**:
```json
{
  "embeddings": [[0.1, 0.2, ...], [0.3, 0.4, ...]],
  "model": "text-embedding-ada-002",
  "dimension": 1536,
  "cached_count": 1,
  "total_count": 2
}
```

### 2. 健康检查

**GET** `/health`

返回服务状态和配置信息。

**响应**:
```json
{
  "status": "healthy",
  "service_name": "embedding-service",
  "version": "1.0.0",
  "model_provider": "openai",
  "model_name": "text-embedding-ada-002",
  "redis_connected": true,
  "nacos_registered": true
}
```

### 3. 切换模型

**POST** `/api/model/switch`

在 OpenAI 和本地模型之间切换。

**请求体**:
```json
{
  "use_local_model": false,
  "model_name": "text-embedding-ada-002"
}
```

### 4. 清空缓存

**POST** `/api/cache/clear`

清空 Redis 中所有缓存的嵌入向量。

### 5. Prometheus 指标

**GET** `/metrics`

暴露 Prometheus 监控指标。

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SERVICE_NAME` | 服务名称 | `embedding-service` |
| `SERVICE_PORT` | 服务端口 | `9003` |
| `NACOS_SERVER` | Nacos 服务器地址 | `localhost:8848` |
| `REDIS_URL` | Redis 连接 URL | `redis://localhost:6379/0` |
| `OPENAI_API_KEY` | OpenAI API 密钥 | - |
| `USE_LOCAL_MODEL` | 是否使用本地模型 | `false` |
| `LOCAL_MODEL_NAME` | 本地模型名称 | `BAAI/bge-base-zh-v1.5` |
| `BATCH_SIZE` | 批量处理大小 | `32` |

### 模型选择

#### OpenAI 模型
- **优点**: 高质量、无需本地资源、易于使用
- **缺点**: 需要 API 密钥、有调用成本、依赖网络
- **适用场景**: 生产环境、对质量要求高

#### 本地 BGE 模型
- **优点**: 免费、私有化部署、无网络依赖
- **缺点**: 需要本地资源、首次加载慢
- **适用场景**: 离线环境、成本敏感

## Docker 部署

### 构建镜像

```bash
docker build -t embedding-service:latest .
```

### 运行容器

```bash
docker run -d \
  --name embedding-service \
  -p 9003:9003 \
  -e OPENAI_API_KEY=your-key \
  -e NACOS_SERVER=nacos:8848 \
  -e REDIS_URL=redis://redis:6379/0 \
  embedding-service:latest
```

### Docker Compose

```yaml
embedding-service:
  build: ./python-services/embedding-service
  ports:
    - "9003:9003"
  environment:
    - NACOS_SERVER=nacos:8848
    - REDIS_URL=redis://redis:6379/0
    - OPENAI_API_KEY=${OPENAI_API_KEY}
  depends_on:
    - nacos
    - redis
```

## 监控指标

### Prometheus 指标

- `embedding_requests_total`: 嵌入请求总数
- `embedding_texts_total`: 向量化文本总数
- `embedding_cache_hits_total`: 缓存命中次数
- `embedding_cache_misses_total`: 缓存未命中次数
- `embedding_duration_seconds`: 向量生成耗时
- `embedding_batch_size`: 批量处理大小
- `redis_connection_status`: Redis 连接状态
- `nacos_registration_status`: Nacos 注册状态

### 查看指标

```bash
curl http://localhost:9003/metrics
```

## 性能优化

### 1. 批量处理

将多个文本合并为一个请求，减少网络开销：

```python
# 推荐
response = requests.post("/api/embeddings", json={
    "texts": ["文本1", "文本2", "文本3"]
})

# 不推荐
for text in texts:
    response = requests.post("/api/embeddings", json={
        "texts": [text]
    })
```

### 2. 启用缓存

对于重复查询，启用缓存可以显著提升性能：

```python
response = requests.post("/api/embeddings", json={
    "texts": ["常见问题"],
    "use_cache": True  # 启用缓存
})
```

### 3. 本地模型

对于高并发场景，使用本地模型可以避免 API 限流：

```bash
# 切换到本地模型
curl -X POST http://localhost:9003/api/model/switch \
  -H "Content-Type: application/json" \
  -d '{"use_local_model": true}'
```

## 故障排查

### 1. OpenAI API 调用失败

**问题**: `openai.error.AuthenticationError`

**解决**:
- 检查 `OPENAI_API_KEY` 是否正确
- 确认 API 密钥有效且有余额
- 检查网络连接和代理设置

### 2. 本地模型加载失败

**问题**: `OSError: Can't load model`

**解决**:
- 首次使用会自动下载模型，需要网络连接
- 检查磁盘空间是否充足（模型约 400MB）
- 手动下载模型到 `~/.cache/huggingface/`

### 3. Redis 连接失败

**问题**: `redis.exceptions.ConnectionError`

**解决**:
- 检查 Redis 服务是否启动
- 确认 `REDIS_URL` 配置正确
- 检查网络连接和防火墙设置

### 4. 内存不足

**问题**: 本地模型加载时内存溢出

**解决**:
- 使用更小的模型（如 `bge-small-zh-v1.5`）
- 减小 `BATCH_SIZE` 配置
- 增加系统内存或使用 OpenAI API

## 开发指南

### 添加新的嵌入模型

1. 在 `app/embedding_generator.py` 中创建新的生成器类：

```python
class CustomEmbeddingGenerator(EmbeddingGenerator):
    def generate(self, texts: List[str]) -> List[List[float]]:
        # 实现向量生成逻辑
        pass
```

2. 在 `EmbeddingService` 中注册新模型：

```python
def _initialize_generator(self):
    if settings.use_custom_model:
        self.generator = CustomEmbeddingGenerator()
```

### 运行测试

```bash
# 安装测试依赖
pip install pytest pytest-asyncio httpx

# 运行测试
pytest tests/
```

## 相关文档

- [OpenAI Embeddings API](https://platform.openai.com/docs/guides/embeddings)
- [Sentence Transformers](https://www.sbert.net/)
- [BGE Models](https://huggingface.co/BAAI)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)

## 许可证

MIT License
