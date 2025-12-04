# 批量文档处理功能

## 概述

批量文档处理功能允许用户通过上传 ZIP 压缩包一次性上传多个文档，系统会自动解压并异步处理所有文档。

## 功能特性

- ✅ 支持 ZIP 文件批量上传
- ✅ 自动解压和文档提取
- ✅ 异步批量处理（Celery）
- ✅ 实时进度跟踪（Redis）
- ✅ 任务状态查询
- ✅ 处理结果报告

## 架构设计

```
用户上传 ZIP → Document Service → RabbitMQ → Document Processing Service
                                                        ↓
                                                   Celery Worker
                                                        ↓
                                                   处理文档
                                                        ↓
                                                   更新进度 (Redis)
```

## API 接口

### 1. 批量上传文档

**端点**: `POST /api/v1/documents/batch-upload`

**请求**:
```bash
curl -X POST http://localhost:8081/api/v1/documents/batch-upload \
  -H "Content-Type: multipart/form-data" \
  -F "file=@documents.zip"
```

**响应**:
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "documentCount": 5,
  "documentIds": [
    "doc-id-1",
    "doc-id-2",
    "doc-id-3",
    "doc-id-4",
    "doc-id-5"
  ],
  "message": "批量上传成功，共 5 个文档，正在处理中"
}
```

### 2. 查询任务状态

**端点**: `GET /api/v1/documents/batch-upload/{taskId}`

**请求**:
```bash
curl http://localhost:8081/api/v1/documents/batch-upload/550e8400-e29b-41d4-a716-446655440000
```

**响应**:
```json
{
  "taskId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PROCESSING",
  "total": 5,
  "processed": 3,
  "success": 2,
  "failed": 1,
  "progress": 60,
  "results": [
    {
      "document_id": "doc-id-1",
      "filename": "doc1.pdf",
      "status": "SUCCESS",
      "chunk_count": 10,
      "message": "处理成功"
    },
    {
      "document_id": "doc-id-2",
      "filename": "doc2.docx",
      "status": "SUCCESS",
      "chunk_count": 8,
      "message": "处理成功"
    },
    {
      "document_id": "doc-id-3",
      "filename": "doc3.txt",
      "status": "FAILED",
      "chunk_count": 0,
      "message": "文件格式错误"
    }
  ],
  "summary": "批量处理进行中: 成功 2, 失败 1"
}
```

## 任务状态

- `PENDING`: 任务已创建，等待处理
- `PROCESSING`: 正在处理中
- `COMPLETED`: 全部成功完成
- `COMPLETED_WITH_ERRORS`: 完成但有部分失败
- `FAILED`: 任务失败
- `NOT_FOUND`: 任务不存在或已过期

## 支持的文件格式

- PDF (`.pdf`)
- Word 文档 (`.docx`)
- 文本文件 (`.txt`)
- Markdown (`.md`)

## 配置说明

### Document Service 配置

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin
```

### Document Processing Service 配置

```bash
# .env
REDIS_URL=redis://localhost:6379/0
RABBITMQ_URL=amqp://admin:admin@localhost:5672
```

## 启动服务

### 1. 启动 Celery Worker

```bash
cd python-services/document-processing-service
chmod +x start_celery_worker.sh
./start_celery_worker.sh
```

或者直接运行:
```bash
celery -A app.celery_app worker --loglevel=info --concurrency=2
```

### 2. 启动批量处理消费者

```bash
cd python-services/document-processing-service
chmod +x start_batch_consumer.sh
./start_batch_consumer.sh
```

或者直接运行:
```bash
python -m app.batch_consumer
```

## 监控和调试

### 查看 Celery 任务状态

```bash
# 使用 Celery Flower (可选)
pip install flower
celery -A app.celery_app flower
# 访问 http://localhost:5555
```

### 查看 Redis 中的任务进度

```bash
redis-cli
> GET batch_task:550e8400-e29b-41d4-a716-446655440000
```

### 查看 RabbitMQ 队列

访问 RabbitMQ 管理界面: http://localhost:15672
- 用户名: admin
- 密码: admin

查看队列:
- `document.batch.processing.queue`: 批量处理队列
- `document.processing.queue`: 单文档处理队列

## 性能优化

### Celery Worker 配置

```bash
# 增加并发数
celery -A app.celery_app worker --concurrency=4

# 设置任务超时
celery -A app.celery_app worker --time-limit=1800

# 设置预取数量
celery -A app.celery_app worker --prefetch-multiplier=1
```

### Redis 缓存优化

任务进度数据在 Redis 中保留 1 小时，可以根据需要调整:

```python
# app/tasks.py
redis_client.setex(key, 3600, json.dumps(progress_data))  # 3600 秒 = 1 小时
```

## 错误处理

### 常见错误

1. **ZIP 文件格式错误**
   - 错误: "只支持 ZIP 格式的压缩文件"
   - 解决: 确保上传的是 .zip 文件

2. **没有支持的文档**
   - 错误: "ZIP 文件中没有找到支持的文档"
   - 解决: 确保 ZIP 中包含 PDF、DOCX、TXT 或 MD 文件

3. **任务不存在**
   - 错误: "任务不存在或已过期"
   - 解决: 任务进度在 Redis 中保留 1 小时，超时后会被清除

4. **Celery Worker 未启动**
   - 错误: 任务一直处于 PENDING 状态
   - 解决: 确保 Celery Worker 正在运行

## 测试示例

### 创建测试 ZIP 文件

```bash
# 创建测试文档
echo "Test document 1" > doc1.txt
echo "Test document 2" > doc2.txt
echo "Test document 3" > doc3.md

# 创建 ZIP 文件
zip test-documents.zip doc1.txt doc2.txt doc3.md
```

### 上传并查询

```bash
# 1. 上传 ZIP 文件
RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/documents/batch-upload \
  -F "file=@test-documents.zip")

# 2. 提取 taskId
TASK_ID=$(echo $RESPONSE | jq -r '.taskId')

# 3. 查询任务状态
curl http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID | jq

# 4. 持续查询直到完成
while true; do
  STATUS=$(curl -s http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID | jq -r '.status')
  echo "Status: $STATUS"
  if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "COMPLETED_WITH_ERRORS" ]; then
    break
  fi
  sleep 2
done
```

## 限制和注意事项

1. **文件大小限制**: 单个文件最大 50MB
2. **ZIP 文件大小**: 建议不超过 500MB
3. **并发处理**: 默认 2 个并发 worker，可根据服务器性能调整
4. **任务保留时间**: Redis 中的任务进度保留 1 小时
5. **队列消息过期**: RabbitMQ 消息 2 小时后过期

## 未来改进

- [ ] 支持更多压缩格式（RAR、7Z）
- [ ] 支持嵌套目录结构
- [ ] 添加批量处理优先级
- [ ] 支持批量处理暂停/恢复
- [ ] 添加批量处理统计和报表
- [ ] 支持批量处理失败重试
- [ ] 添加 WebSocket 实时推送进度
