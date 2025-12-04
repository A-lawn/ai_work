# 批量文档处理功能快速开始

## 快速启动

### 1. 启动所有服务

```bash
# 启动所有服务（包括 Celery Worker 和批量处理消费者）
docker-compose up -d

# 查看服务状态
docker-compose ps
```

### 2. 验证服务运行

```bash
# 检查 Document Service
curl http://localhost:8081/health

# 检查 Document Processing Service
curl http://localhost:9001/health

# 检查 Celery Worker
docker logs celery-worker

# 检查批量处理消费者
docker logs batch-consumer

# 检查 RabbitMQ
curl -u admin:admin123 http://localhost:15672/api/queues
```

## 使用示例

### 准备测试文档

```bash
# 创建测试目录
mkdir test-docs
cd test-docs

# 创建测试文档
cat > doc1.txt << EOF
这是第一个测试文档。
包含一些运维相关的内容。
EOF

cat > doc2.md << EOF
# 运维手册

## 服务器配置
- CPU: 8核
- 内存: 16GB
- 磁盘: 500GB SSD
EOF

cat > doc3.txt << EOF
故障排查步骤：
1. 检查服务状态
2. 查看日志文件
3. 重启服务
EOF

# 创建 ZIP 文件
zip ../test-documents.zip *.txt *.md
cd ..
```

### 批量上传文档

```bash
# 上传 ZIP 文件
curl -X POST http://localhost:8081/api/v1/documents/batch-upload \
  -F "file=@test-documents.zip" \
  | jq

# 示例响应:
# {
#   "taskId": "550e8400-e29b-41d4-a716-446655440000",
#   "status": "PENDING",
#   "documentCount": 3,
#   "documentIds": ["doc-id-1", "doc-id-2", "doc-id-3"],
#   "message": "批量上传成功，共 3 个文档，正在处理中"
# }
```

### 查询处理进度

```bash
# 保存 taskId
TASK_ID="550e8400-e29b-41d4-a716-446655440000"

# 查询任务状态
curl http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID | jq

# 持续监控进度
watch -n 2 "curl -s http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID | jq '.progress, .status'"
```

### 自动化脚本

创建 `batch-upload-test.sh`:

```bash
#!/bin/bash

# 批量上传测试脚本

# 1. 上传文档
echo "上传文档..."
RESPONSE=$(curl -s -X POST http://localhost:8081/api/v1/documents/batch-upload \
  -F "file=@test-documents.zip")

echo "上传响应:"
echo $RESPONSE | jq

# 2. 提取 taskId
TASK_ID=$(echo $RESPONSE | jq -r '.taskId')
echo "任务 ID: $TASK_ID"

# 3. 轮询任务状态
echo "监控任务进度..."
while true; do
  STATUS_RESPONSE=$(curl -s http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID)
  STATUS=$(echo $STATUS_RESPONSE | jq -r '.status')
  PROGRESS=$(echo $STATUS_RESPONSE | jq -r '.progress')
  
  echo "状态: $STATUS, 进度: $PROGRESS%"
  
  # 检查是否完成
  if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "COMPLETED_WITH_ERRORS" ] || [ "$STATUS" = "FAILED" ]; then
    echo "任务完成!"
    echo $STATUS_RESPONSE | jq
    break
  fi
  
  sleep 2
done
```

运行脚本:
```bash
chmod +x batch-upload-test.sh
./batch-upload-test.sh
```

## 监控和调试

### 查看日志

```bash
# Document Service 日志
docker logs -f document-service

# Document Processing Service 日志
docker logs -f document-processing-service

# Celery Worker 日志
docker logs -f celery-worker

# 批量处理消费者日志
docker logs -f batch-consumer

# RabbitMQ 日志
docker logs -f rabbitmq
```

### 查看 RabbitMQ 队列

访问 RabbitMQ 管理界面:
- URL: http://localhost:15672
- 用户名: admin
- 密码: admin123

查看队列:
- `document.batch.processing.queue`: 批量处理队列
- `document.processing.queue`: 单文档处理队列

### 查看 Redis 数据

```bash
# 连接到 Redis
docker exec -it redis redis-cli -a redis123

# 查看所有批量任务
KEYS batch_task:*

# 查看特定任务
GET batch_task:550e8400-e29b-41d4-a716-446655440000

# 查看任务 TTL
TTL batch_task:550e8400-e29b-41d4-a716-446655440000
```

### 查看 Celery 任务

```bash
# 进入 Celery Worker 容器
docker exec -it celery-worker bash

# 查看活动任务
celery -A app.celery_app inspect active

# 查看已注册任务
celery -A app.celery_app inspect registered

# 查看统计信息
celery -A app.celery_app inspect stats
```

## 性能测试

### 创建大量测试文档

```bash
#!/bin/bash
# create-test-docs.sh

mkdir -p test-docs-large
cd test-docs-large

# 创建 50 个测试文档
for i in {1..50}; do
  cat > "doc${i}.txt" << EOF
测试文档 ${i}

这是一个用于性能测试的文档。
包含一些示例内容用于测试批量处理功能。

内容段落 1: 系统监控和告警
内容段落 2: 日志分析和故障排查
内容段落 3: 性能优化和调优
EOF
done

# 创建 ZIP 文件
zip ../test-documents-large.zip *.txt
cd ..
```

### 运行性能测试

```bash
# 创建测试文档
chmod +x create-test-docs.sh
./create-test-docs.sh

# 上传并计时
time curl -X POST http://localhost:8081/api/v1/documents/batch-upload \
  -F "file=@test-documents-large.zip" \
  | jq

# 监控处理时间
TASK_ID="your-task-id"
START_TIME=$(date +%s)

while true; do
  STATUS=$(curl -s http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID | jq -r '.status')
  if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "COMPLETED_WITH_ERRORS" ]; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    echo "处理完成，耗时: ${DURATION} 秒"
    break
  fi
  sleep 2
done
```

## 故障排查

### 问题 1: 任务一直处于 PENDING 状态

**原因**: Celery Worker 未启动或未连接到 Redis

**解决方案**:
```bash
# 检查 Celery Worker 状态
docker ps | grep celery-worker

# 查看 Celery Worker 日志
docker logs celery-worker

# 重启 Celery Worker
docker-compose restart celery-worker
```

### 问题 2: 批量处理消费者未接收消息

**原因**: RabbitMQ 连接失败或队列未创建

**解决方案**:
```bash
# 检查 RabbitMQ 连接
docker logs batch-consumer

# 检查队列是否存在
curl -u admin:admin123 http://localhost:15672/api/queues/%2F/document.batch.processing.queue

# 重启消费者
docker-compose restart batch-consumer
```

### 问题 3: 文档处理失败

**原因**: 文件格式不支持或文件损坏

**解决方案**:
```bash
# 查看详细错误信息
curl http://localhost:8081/api/v1/documents/batch-upload/$TASK_ID | jq '.results'

# 查看处理日志
docker logs document-processing-service
```

### 问题 4: Redis 连接失败

**原因**: Redis 服务未启动或密码错误

**解决方案**:
```bash
# 检查 Redis 状态
docker ps | grep redis

# 测试 Redis 连接
docker exec -it redis redis-cli -a redis123 ping

# 重启 Redis
docker-compose restart redis
```

## 清理和重置

### 清理测试数据

```bash
# 删除所有测试文档
curl -X DELETE http://localhost:8081/api/v1/documents/{document-id}

# 清理 Redis 中的任务数据
docker exec -it redis redis-cli -a redis123 FLUSHDB

# 清理 RabbitMQ 队列
docker exec -it rabbitmq rabbitmqctl purge_queue document.batch.processing.queue
```

### 重启所有服务

```bash
# 停止所有服务
docker-compose down

# 清理数据卷（可选）
docker-compose down -v

# 重新启动
docker-compose up -d
```

## 下一步

- 查看完整文档: [BATCH_PROCESSING.md](java-services/document-service/BATCH_PROCESSING.md)
- 集成到前端界面
- 配置监控和告警
- 优化性能参数
