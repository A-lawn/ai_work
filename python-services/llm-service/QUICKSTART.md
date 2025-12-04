# LLM Service - 快速开始指南

本指南帮助您快速启动和测试 LLM Service。

## 前置要求

- Python 3.10+
- pip
- （可选）Docker
- LLM API Key（OpenAI 或 Azure OpenAI）

## 快速启动步骤

### 1. 配置环境变量

```bash
cd python-services/llm-service
cp .env.example .env
```

编辑 `.env` 文件，配置您的 LLM 提供商：

**使用 OpenAI:**
```bash
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_MODEL=gpt-4
```

**使用 Azure OpenAI:**
```bash
LLM_PROVIDER=azure
AZURE_OPENAI_API_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT=gpt-4
```

**使用本地模型:**
```bash
LLM_PROVIDER=local
LOCAL_MODEL_ENDPOINT=http://localhost:8000
LOCAL_MODEL_NAME=llama-2-7b
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 启动服务

```bash
# 开发模式（带自动重载）
uvicorn app.main:app --host 0.0.0.0 --port 9004 --reload

# 或生产模式
python -m uvicorn app.main:app --host 0.0.0.0 --port 9004
```

服务将在 http://localhost:9004 启动。

### 4. 验证服务

在新终端运行验证脚本：

```bash
chmod +x verify-service.sh
./verify-service.sh
```

或手动测试：

```bash
# 检查服务状态
curl http://localhost:9004/

# 健康检查
curl http://localhost:9004/api/health

# 获取服务信息
curl http://localhost:9004/api/info
```

## 测试 API

### 1. 同步文本生成

```bash
curl -X POST http://localhost:9004/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "你是一个专业的运维助手。请简要解释什么是微服务架构？",
    "max_tokens": 200,
    "temperature": 0.7,
    "stream": false
  }'
```

预期响应：
```json
{
  "text": "微服务架构是一种软件架构风格...",
  "model": "gpt-4",
  "provider": "openai",
  "usage": {
    "prompt_tokens": 30,
    "completion_tokens": 150,
    "total_tokens": 180
  },
  "finish_reason": "stop"
}
```

### 2. 流式文本生成

```bash
curl -X POST http://localhost:9004/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "解释什么是 Docker",
    "max_tokens": 100,
    "stream": true
  }'
```

预期响应（Server-Sent Events）：
```
data: {"delta": "Docker"}
data: {"delta": " 是"}
data: {"delta": "一个"}
...
data: {"delta": "", "finish_reason": "stop"}
data: [DONE]
```

### 3. Token 计数

```bash
curl -X POST http://localhost:9004/api/count-tokens \
  -H "Content-Type: application/json" \
  -d '{"text": "这是一段测试文本，用于计算 Token 数量"}'
```

预期响应：
```json
{
  "text_length": 23,
  "token_count": 15
}
```

## 使用 Docker

### 构建镜像

```bash
docker build -t llm-service:latest .
```

### 运行容器

```bash
docker run -d \
  --name llm-service \
  -p 9004:9004 \
  -e OPENAI_API_KEY=your-api-key \
  -e LLM_PROVIDER=openai \
  llm-service:latest
```

### 查看日志

```bash
docker logs -f llm-service
```

### 停止容器

```bash
docker stop llm-service
docker rm llm-service
```

## API 文档

启动服务后，访问交互式 API 文档：

- **Swagger UI**: http://localhost:9004/docs
- **ReDoc**: http://localhost:9004/redoc

## 监控指标

查看 Prometheus 指标：

```bash
curl http://localhost:9004/metrics
```

主要指标：
- `llm_requests_total`: 请求总数
- `llm_request_duration_seconds`: 请求耗时
- `llm_tokens_used_total`: Token 使用量
- `llm_errors_total`: 错误总数
- `llm_active_requests`: 活跃请求数

## 常见问题

### 1. 服务启动失败

**问题**: `ValueError: OpenAI API key is required`

**解决**: 确保在 `.env` 文件中配置了正确的 API Key。

### 2. 连接超时

**问题**: 请求超时

**解决**: 增加超时时间：
```bash
OPENAI_TIMEOUT=120
```

### 3. Token 超限

**问题**: `max_tokens` 超过限制

**解决**: 调整配置：
```bash
MAX_RESPONSE_TOKENS=2000
```

### 4. 本地模型连接失败

**问题**: 无法连接到本地模型

**解决**: 
- 确认本地模型服务正在运行
- 检查端点 URL 是否正确
- 确认模型支持 OpenAI 兼容接口

## 下一步

- 查看 [README.md](README.md) 了解详细功能
- 查看 [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) 了解实现细节
- 集成到 RAG Query Service 中使用

## 支持

如有问题，请查看：
- API 文档: http://localhost:9004/docs
- 日志输出
- 健康检查: http://localhost:9004/api/health
