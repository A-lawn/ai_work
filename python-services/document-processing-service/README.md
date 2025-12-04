# Document Processing Service

文档处理服务 - 负责文档解析、分块和向量化

## 功能特性

- 支持多种文档格式：PDF、DOCX、TXT、Markdown
- 智能文本分块（使用 LangChain RecursiveCharacterTextSplitter）
- 文档向量化（调用 Embedding Service）
- 向量存储（ChromaDB）
- RabbitMQ 消息队列集成
- Nacos 服务注册与发现
- Prometheus 指标监控
- Zipkin 链路追踪

## 技术栈

- **框架**: FastAPI 0.104.1
- **文档解析**: PyPDF2, python-docx, markdown
- **文本分块**: LangChain
- **向量数据库**: ChromaDB
- **消息队列**: RabbitMQ (pika)
- **服务注册**: Nacos
- **监控**: Prometheus, OpenTelemetry

## API 端点

### 健康检查
```
GET /health
```

### 处理文档
```
POST /api/process-document
Content-Type: application/json

{
  "document_id": "doc-123",
  "file_path": "/path/to/document.pdf",
  "file_type": "pdf",
  "filename": "document.pdf",
  "metadata": {}
}
```

### 删除文档向量
```
DELETE /api/vectors/{document_id}
```

### Prometheus 指标
```
GET /metrics
```

### API 文档
```
GET /docs
```

## 环境变量

```bash
# 服务配置
SERVICE_NAME=document-processing-service
SERVICE_PORT=9001
SERVICE_HOST=0.0.0.0

# Nacos 配置
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=rag-system
NACOS_GROUP=DEFAULT_GROUP

# ChromaDB 配置
CHROMA_HOST=localhost
CHROMA_PORT=8000

# RabbitMQ 配置
RABBITMQ_URL=amqp://admin:admin@localhost:5672
RABBITMQ_QUEUE=document.processing
RABBITMQ_EXCHANGE=document.exchange
RABBITMQ_ROUTING_KEY=document.process

# Redis 配置
REDIS_URL=redis://localhost:6379/0

# Embedding Service 配置
EMBEDDING_SERVICE_URL=http://localhost:9003

# 文档处理配置
CHUNK_SIZE=512
CHUNK_OVERLAP=50

# Zipkin 配置
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
```

## 本地开发

### 安装依赖
```bash
pip install -r requirements.txt
```

### 启动服务
```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 9001 --reload
```

## Docker 部署

### 构建镜像
```bash
docker build -t document-processing-service:latest .
```

### 运行容器
```bash
docker run -d \
  --name document-processing-service \
  -p 9001:9001 \
  -e NACOS_SERVER=nacos:8848 \
  -e CHROMA_HOST=chroma \
  -e RABBITMQ_URL=amqp://admin:admin@rabbitmq:5672 \
  document-processing-service:latest
```

## 支持的文档格式

- **PDF**: .pdf
- **Word**: .docx, .doc
- **文本**: .txt, .log, .csv
- **Markdown**: .md, .markdown

## 文档处理流程

1. **接收请求**: 通过 API 或 RabbitMQ 消息接收文档处理请求
2. **文档解析**: 根据文件类型选择对应的解析器提取文本
3. **文本分块**: 使用 RecursiveCharacterTextSplitter 将文本分割成 512 token 的块
4. **向量生成**: 调用 Embedding Service 为每个文本块生成向量
5. **向量存储**: 将文本块和向量存储到 ChromaDB
6. **返回结果**: 返回处理结果（成功/失败、分块数量等）

## RabbitMQ 消息格式

### 文档处理消息
```json
{
  "document_id": "doc-123",
  "file_path": "/path/to/document.pdf",
  "file_type": "pdf",
  "filename": "document.pdf",
  "metadata": {
    "user_id": "user-456",
    "upload_time": "2024-01-01T00:00:00Z"
  }
}
```

## 监控指标

服务暴露以下 Prometheus 指标：

- HTTP 请求计数
- HTTP 请求延迟
- 文档处理成功/失败计数
- 文档分块数量
- 向量生成时间
- ChromaDB 操作时间

## 日志

日志文件位置：`logs/document-processing-YYYYMMDD.log`

日志级别：
- INFO: 正常操作日志
- WARNING: 警告信息
- ERROR: 错误信息

## 故障排查

### ChromaDB 连接失败
检查 ChromaDB 服务是否运行：
```bash
curl http://localhost:8000/api/v1/heartbeat
```

### RabbitMQ 连接失败
检查 RabbitMQ 服务状态：
```bash
curl http://localhost:15672/api/overview
```

### Embedding Service 不可用
检查 Embedding Service 健康状态：
```bash
curl http://localhost:9003/health
```

## 开发指南

### 添加新的文档解析器

1. 在 `app/parsers/` 目录创建新的解析器类
2. 继承 `DocumentParser` 基类
3. 实现 `parse()` 和 `get_metadata()` 方法
4. 在 `parser_factory.py` 中注册新的解析器

示例：
```python
from app.parsers.base import DocumentParser

class MyParser(DocumentParser):
    def parse(self, file_path: str) -> str:
        # 实现解析逻辑
        pass
    
    def get_metadata(self, file_path: str) -> Dict[str, Any]:
        # 实现元数据提取
        pass
    
    @property
    def supported_extensions(self) -> list:
        return [".myext"]
```

## 许可证

MIT License
