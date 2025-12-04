# Document Processing Service - 快速开始

## 前置条件

### 必需服务
1. **ChromaDB** - 向量数据库
2. **RabbitMQ** - 消息队列
3. **Nacos** - 服务注册中心
4. **Embedding Service** - 嵌入模型服务

### Python 环境
- Python 3.10+
- pip

## 快速启动

### 1. 安装依赖

```bash
cd python-services/document-processing-service
pip install -r requirements.txt
```

### 2. 配置环境变量

复制环境变量示例文件：
```bash
cp .env.example .env
```

编辑 `.env` 文件，配置必要的服务地址：
```bash
# 基础配置
SERVICE_PORT=9001

# Nacos 配置
NACOS_SERVER=localhost:8848

# ChromaDB 配置
CHROMA_HOST=localhost
CHROMA_PORT=8000

# RabbitMQ 配置
RABBITMQ_URL=amqp://admin:admin@localhost:5672

# Embedding Service 配置
EMBEDDING_SERVICE_URL=http://localhost:9003
```

### 3. 启动服务

```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 9001 --reload
```

或者使用 Python 直接运行：
```bash
python -m app.main
```

### 4. 验证服务

运行验证脚本：
```bash
chmod +x verify-service.sh
./verify-service.sh
```

或手动验证：
```bash
# 健康检查
curl http://localhost:9001/health

# 查看服务信息
curl http://localhost:9001/

# 访问 API 文档
open http://localhost:9001/docs
```

## Docker 快速启动

### 1. 构建镜像

```bash
docker build -t document-processing-service:latest .
```

### 2. 运行容器

```bash
docker run -d \
  --name document-processing-service \
  -p 9001:9001 \
  -e NACOS_SERVER=nacos:8848 \
  -e CHROMA_HOST=chroma \
  -e CHROMA_PORT=8000 \
  -e RABBITMQ_URL=amqp://admin:admin@rabbitmq:5672 \
  -e EMBEDDING_SERVICE_URL=http://embedding-service:9003 \
  document-processing-service:latest
```

### 3. 查看日志

```bash
docker logs -f document-processing-service
```

## 使用 Docker Compose

如果使用项目的 Docker Compose 配置：

```bash
# 启动所有服务
docker-compose up -d

# 查看 Document Processing Service 日志
docker-compose logs -f document-processing-service

# 停止服务
docker-compose down
```

## API 使用示例

### 1. 处理文档

```bash
curl -X POST http://localhost:9001/api/process-document \
  -H "Content-Type: application/json" \
  -d '{
    "document_id": "doc-001",
    "file_path": "/path/to/document.pdf",
    "file_type": "pdf",
    "filename": "运维手册.pdf",
    "metadata": {
      "category": "ops",
      "tags": ["kubernetes", "troubleshooting"]
    }
  }'
```

响应示例：
```json
{
  "success": true,
  "document_id": "doc-001",
  "chunk_count": 25,
  "message": "文档处理成功",
  "error": null
}
```

### 2. 删除文档向量

```bash
curl -X DELETE http://localhost:9001/api/vectors/doc-001
```

响应示例：
```json
{
  "success": true,
  "document_id": "doc-001",
  "deleted_count": 25,
  "message": "成功删除 25 个向量"
}
```

### 3. 查看 Prometheus 指标

```bash
curl http://localhost:9001/metrics
```

## 测试文档处理

### 准备测试文件

创建一个测试文本文件：
```bash
echo "这是一个测试文档。用于验证文档处理服务的功能。" > /tmp/test.txt
```

### 处理测试文件

```bash
curl -X POST http://localhost:9001/api/process-document \
  -H "Content-Type: application/json" \
  -d '{
    "document_id": "test-001",
    "file_path": "/tmp/test.txt",
    "file_type": "txt",
    "filename": "test.txt"
  }'
```

## 通过 RabbitMQ 处理文档

服务会自动监听 RabbitMQ 队列 `document.processing`。

发送消息到队列：
```python
import pika
import json

# 连接 RabbitMQ
connection = pika.BlockingConnection(
    pika.URLParameters('amqp://admin:admin@localhost:5672')
)
channel = connection.channel()

# 发送消息
message = {
    "document_id": "doc-002",
    "file_path": "/path/to/document.pdf",
    "file_type": "pdf",
    "filename": "document.pdf"
}

channel.basic_publish(
    exchange='document.exchange',
    routing_key='document.process',
    body=json.dumps(message)
)

connection.close()
```

## 常见问题

### 1. ChromaDB 连接失败

**问题**: 服务启动时提示 ChromaDB 连接失败

**解决**:
```bash
# 检查 ChromaDB 是否运行
curl http://localhost:8000/api/v1/heartbeat

# 启动 ChromaDB
docker run -d -p 8000:8000 chromadb/chroma:latest
```

### 2. RabbitMQ 连接失败

**问题**: 无法连接到 RabbitMQ

**解决**:
```bash
# 检查 RabbitMQ 状态
docker ps | grep rabbitmq

# 启动 RabbitMQ
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin \
  rabbitmq:3.12-management
```

### 3. Embedding Service 不可用

**问题**: 调用 Embedding Service 失败

**解决**:
```bash
# 检查 Embedding Service 状态
curl http://localhost:9003/health

# 确保 Embedding Service 已启动
```

### 4. 文档解析失败

**问题**: 某些文档无法解析

**解决**:
- 检查文件格式是否支持（PDF、DOCX、TXT、MD）
- 检查文件是否损坏
- 查看日志获取详细错误信息

### 5. 日志目录权限问题

**问题**: 无法创建日志文件

**解决**:
```bash
# 创建日志目录
mkdir -p logs

# 设置权限
chmod 755 logs
```

## 监控和调试

### 查看日志

```bash
# 实时查看日志
tail -f logs/document-processing-*.log

# 搜索错误日志
grep ERROR logs/document-processing-*.log
```

### 查看 Prometheus 指标

访问 http://localhost:9001/metrics 查看服务指标

### 使用 API 文档

访问 http://localhost:9001/docs 使用交互式 API 文档

## 性能调优

### 1. 调整分块大小

编辑 `.env` 文件：
```bash
CHUNK_SIZE=1024  # 增加分块大小
CHUNK_OVERLAP=100  # 增加重叠大小
```

### 2. 增加服务实例

使用 Docker Compose 扩展：
```bash
docker-compose up -d --scale document-processing-service=3
```

### 3. 优化 RabbitMQ 配置

调整 prefetch_count 以提高并发处理能力

## 下一步

1. **集成到完整系统**: 将服务集成到 RAG 系统中
2. **配置监控**: 设置 Prometheus 和 Grafana 监控
3. **测试文档处理**: 使用真实文档测试处理流程
4. **性能测试**: 进行压力测试和性能优化

## 相关文档

- [README.md](README.md) - 完整服务文档
- [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - 实现总结
- [requirements.txt](requirements.txt) - Python 依赖列表

## 获取帮助

- 查看日志文件: `logs/document-processing-*.log`
- 访问 API 文档: http://localhost:9001/docs
- 检查健康状态: http://localhost:9001/health
