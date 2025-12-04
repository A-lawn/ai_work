# LLM Service

大语言模型服务，为 RAG 系统提供文本生成能力，支持多种 LLM 提供商。

## 功能特性

- **多提供商支持**: OpenAI、Azure OpenAI、本地模型（OpenAI 兼容接口）
- **同步和流式生成**: 支持同步响应和流式响应（SSE）
- **提示词管理**: 内置提示词模板和构建器
- **Token 统计**: 准确的 Token 计数和使用统计
- **服务注册**: 自动注册到 Nacos 服务中心
- **链路追踪**: 集成 Zipkin 分布式追踪
- **性能监控**: Prometheus 指标暴露
- **重试机制**: 自动重试和超时控制
- **健康检查**: 提供商可用性检查

## 技术栈

- **框架**: FastAPI 0.104.1
- **LLM SDK**: OpenAI 1.3.7, LangChain 0.0.340
- **服务发现**: Nacos SDK Python 0.1.9
- **监控**: Prometheus Client, OpenTelemetry
- **Token 计数**: tiktoken 0.5.2

## 快速开始

### 1. 环境配置

复制环境变量模板：

```bash
cp .env.example .env
```

编辑 `.env` 文件，配置 LLM 提供商：

```bash
# 使用 OpenAI
LLM_PROVIDER=openai
OPENAI_API_KEY=your-api-key-here

# 或使用 Azure OpenAI
LLM_PROVIDER=azure
AZURE_OPENAI_API_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/

# 或使用本地模型
LLM_PROVIDER=local
LOCAL_MODEL_ENDPOINT=http://localhost:8000
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 运行服务

```bash
# 开发模式
uvicorn app.main:app --host 0.0.0.0 --port 9004 --reload

# 生产模式
python -m uvicorn app.main:app --host 0.0.0.0 --port 9004
```

### 4. Docker 部署

```bash
# 构建镜像
docker build -t llm-service:latest .

# 运行容器
docker run -d \
  --name llm-service \
  -p 9004:9004 \
  --env-file .env \
  llm-service:latest
```

## API 文档

服务启动后，访问以下地址查看 API 文档：

- Swagger UI: http://localhost:9004/docs
- ReDoc: http://localhost:9004/redoc

### 主要端点

#### 1. 生成文本（同步）

```bash
POST /api/generate
Content-Type: application/json

{
  "prompt": "你是一个专业的运维助手。请回答：如何排查CPU使用率过高的问题？",
  "max_tokens": 500,
  "temperature": 0.7,
  "stream": false
}
```

响应：

```json
{
  "text": "排查CPU使用率过高的问题，可以按照以下步骤进行...",
  "model": "gpt-4",
  "provider": "openai",
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 200,
    "total_tokens": 250
  },
  "finish_reason": "stop"
}
```

#### 2. 生成文本（流式）

```bash
POST /api/generate
Content-Type: application/json

{
  "prompt": "解释什么是微服务架构",
  "stream": true
}
```

响应（Server-Sent Events）：

```
data: {"delta": "微"}
data: {"delta": "服务"}
data: {"delta": "架构"}
...
data: {"delta": "", "finish_reason": "stop"}
data: [DONE]
```

#### 3. Token 计数

```bash
POST /api/count-tokens
Content-Type: application/json

{
  "text": "这是一段测试文本"
}
```

#### 4. 健康检查

```bash
GET /api/health
```

响应：

```json
{
  "status": "healthy",
  "provider": "openai",
  "model": "gpt-4",
  "provider_available": true,
  "message": "LLM provider is available"
}
```

#### 5. 服务信息

```bash
GET /api/info
```

## 配置说明

### LLM 提供商配置

#### OpenAI

```bash
LLM_PROVIDER=openai
OPENAI_API_KEY=sk-xxx
OPENAI_MODEL=gpt-4
OPENAI_TEMPERATURE=0.7
```

#### Azure OpenAI

```bash
LLM_PROVIDER=azure
AZURE_OPENAI_API_KEY=xxx
AZURE_OPENAI_ENDPOINT=https://xxx.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT=gpt-4
```

#### 本地模型

支持任何 OpenAI 兼容的本地模型服务（vLLM、LocalAI、Ollama 等）：

```bash
LLM_PROVIDER=local
LOCAL_MODEL_ENDPOINT=http://localhost:8000
LOCAL_MODEL_NAME=llama-2-7b
```

### Token 限制

```bash
MAX_CONTEXT_TOKENS=4000    # 最大上下文 Token 数
MAX_RESPONSE_TOKENS=1000   # 最大响应 Token 数
```

### 重试配置

```bash
MAX_RETRIES=3              # 最大重试次数
RETRY_DELAY=1.0            # 重试延迟（秒）
OPENAI_TIMEOUT=60          # 请求超时（秒）
```

## 监控指标

服务暴露 Prometheus 指标：

```bash
GET /metrics
```

主要指标：

- `llm_requests_total`: 总请求数（按提供商、模型、状态分类）
- `llm_request_duration_seconds`: 请求耗时分布
- `llm_tokens_used_total`: Token 使用量（按类型分类）
- `llm_errors_total`: 错误总数
- `llm_active_requests`: 活跃请求数
- `llm_streaming_requests_total`: 流式请求总数

## 架构设计

### 适配器模式

使用适配器模式支持多种 LLM 提供商：

```
LLMService
    ├── LLMAdapter (抽象基类)
    │   ├── OpenAIAdapter
    │   ├── AzureOpenAIAdapter
    │   └── LocalModelAdapter
    └── PromptBuilder
```

### 提示词管理

内置提示词模板：

- RAG 查询模板（带上下文）
- 多轮对话模板（带历史）
- 简单查询模板（无上下文）

### Token 控制

- 自动截断超长上下文
- Token 数量估算
- 使用 tiktoken 精确计数

## 开发指南

### 添加新的 LLM 提供商

1. 在 `app/adapters/` 创建新的适配器类
2. 继承 `LLMAdapter` 基类
3. 实现必需的方法：
   - `generate()`: 同步生成
   - `generate_stream()`: 流式生成
   - `count_tokens()`: Token 计数
   - `health_check()`: 健康检查

4. 在 `app/llm_service.py` 的 `_create_adapter()` 中注册

### 自定义提示词模板

编辑 `app/prompt_manager.py` 中的 `PromptTemplate` 类。

## 故障排查

### 1. OpenAI API 连接失败

检查：
- API Key 是否正确
- 网络连接是否正常
- API Base URL 是否正确

### 2. Token 超限

调整配置：
```bash
MAX_CONTEXT_TOKENS=8000
MAX_RESPONSE_TOKENS=2000
```

### 3. 响应超时

增加超时时间：
```bash
OPENAI_TIMEOUT=120
```

### 4. 本地模型连接失败

确认：
- 本地模型服务是否运行
- 端点 URL 是否正确
- 模型是否支持 OpenAI 兼容接口

## 性能优化

1. **使用流式响应**: 减少首字节时间
2. **调整 Token 限制**: 平衡质量和速度
3. **配置重试策略**: 提高可靠性
4. **启用缓存**: 缓存常见查询（可选）

## 许可证

MIT License
