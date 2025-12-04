# 查询流程快速入门

## 概述

本文档介绍如何使用智能运维问答助手的查询功能，包括同步查询、流式查询和多轮对话。

## 前置条件

确保以下服务正在运行：
- Gateway Service (端口 8080)
- Session Service (端口 8082)
- RAG Query Service (端口 9002)
- Embedding Service (端口 9003)
- LLM Service (端口 9004)

启动所有服务：
```bash
docker-compose up -d
```

## 查询方式

### 1. 同步查询（推荐用于生产环境）

通过会话管理的同步查询，支持多轮对话。

**端点**: `POST /api/v1/sessions/query`

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/v1/sessions/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "如何重启 Nginx 服务？",
    "userId": "user123",
    "topK": 5,
    "similarityThreshold": 0.7
  }'
```

**响应示例**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "answer": "要重启 Nginx 服务，可以使用以下命令：\n\n1. 使用 systemctl：\n   sudo systemctl restart nginx\n\n2. 使用 service 命令：\n   sudo service nginx restart\n\n3. 使用 nginx 命令：\n   sudo nginx -s reload",
  "sources": [
    {
      "chunkText": "Nginx 服务管理...",
      "similarityScore": 0.92,
      "documentId": "doc-123",
      "documentName": "nginx-manual.pdf",
      "chunkIndex": 5,
      "pageNumber": 12,
      "section": "服务管理"
    }
  ],
  "queryTime": 2.5
}
```

### 2. 流式查询（推荐用于实时交互）

使用 Server-Sent Events (SSE) 实时返回答案片段。

**端点**: `POST /api/v1/sessions/query/stream`

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/v1/sessions/query/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "question": "如何重启 Nginx 服务？",
    "userId": "user123"
  }' \
  --no-buffer
```

**响应示例** (SSE 格式):
```
event: message
data: 要重启

event: message
data:  Nginx 服务

event: message
data: ，可以使用

event: message
data: 以下命令：

event: done
data: {"session_id": "550e8400-e29b-41d4-a716-446655440000"}
```

### 3. 多轮对话

使用会话 ID 进行多轮对话，系统会自动维护上下文。

**第一轮查询**:
```bash
# 创建新会话并查询
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/sessions/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是 Docker？",
    "userId": "user123"
  }')

# 提取会话 ID
SESSION_ID=$(echo "$RESPONSE" | jq -r '.sessionId')
echo "会话 ID: $SESSION_ID"
```

**第二轮查询** (使用会话 ID):
```bash
curl -X POST http://localhost:8080/api/v1/sessions/query \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"如何使用它？\",
    \"sessionId\": \"$SESSION_ID\"
  }"
```

系统会自动将第一轮的问题和答案作为上下文传递给 RAG 服务。

### 4. 查看会话历史

**端点**: `GET /api/v1/sessions/{sessionId}/history`

**请求示例**:
```bash
curl http://localhost:8080/api/v1/sessions/$SESSION_ID/history
```

**响应示例**:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user123",
  "messages": [
    {
      "id": "msg-1",
      "role": "USER",
      "content": "什么是 Docker？",
      "timestamp": "2024-01-15T10:30:00"
    },
    {
      "id": "msg-2",
      "role": "ASSISTANT",
      "content": "Docker 是一个开源的容器化平台...",
      "timestamp": "2024-01-15T10:30:02"
    },
    {
      "id": "msg-3",
      "role": "USER",
      "content": "如何使用它？",
      "timestamp": "2024-01-15T10:31:00"
    },
    {
      "id": "msg-4",
      "role": "ASSISTANT",
      "content": "使用 Docker 的基本步骤...",
      "timestamp": "2024-01-15T10:31:03"
    }
  ],
  "totalMessages": 4,
  "totalTokens": 1250
}
```

### 5. 直接查询（不通过会话）

用于测试或一次性查询，不保存历史。

**端点**: `POST /api/direct/query`

**请求示例**:
```bash
curl -X POST http://localhost:8080/api/direct/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是微服务？",
    "top_k": 5,
    "similarity_threshold": 0.7
  }'
```

## 前端集成示例

### JavaScript (Fetch API)

#### 同步查询
```javascript
async function syncQuery(question, sessionId = null) {
  const response = await fetch('http://localhost:8080/api/v1/sessions/query', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      question: question,
      sessionId: sessionId,
      userId: 'user123'
    })
  });
  
  const data = await response.json();
  console.log('答案:', data.answer);
  console.log('会话 ID:', data.sessionId);
  console.log('引用来源:', data.sources);
  
  return data;
}

// 使用示例
syncQuery('如何重启 Nginx？').then(result => {
  // 保存会话 ID 用于后续查询
  const sessionId = result.sessionId;
  
  // 第二轮查询
  syncQuery('有其他方法吗？', sessionId);
});
```

#### 流式查询
```javascript
async function streamQuery(question, sessionId = null) {
  const response = await fetch('http://localhost:8080/api/v1/sessions/query/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    },
    body: JSON.stringify({
      question: question,
      sessionId: sessionId,
      userId: 'user123'
    })
  });
  
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  
  let answer = '';
  
  while (true) {
    const { done, value } = await reader.read();
    
    if (done) break;
    
    const chunk = decoder.decode(value);
    const lines = chunk.split('\n');
    
    for (const line of lines) {
      if (line.startsWith('data: ')) {
        const data = line.substring(6);
        
        // 检查是否是完成事件
        if (line.includes('event: done')) {
          const doneData = JSON.parse(data);
          console.log('查询完成，会话 ID:', doneData.session_id);
        } else {
          // 显示答案片段
          answer += data;
          console.log('答案片段:', data);
          // 更新 UI
          document.getElementById('answer').textContent = answer;
        }
      }
    }
  }
  
  return answer;
}

// 使用示例
streamQuery('如何重启 Nginx？');
```

### React 示例

```jsx
import React, { useState } from 'react';

function ChatInterface() {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState('');
  const [sessionId, setSessionId] = useState(null);
  const [loading, setLoading] = useState(false);
  
  const handleStreamQuery = async () => {
    setLoading(true);
    setAnswer('');
    
    try {
      const response = await fetch('http://localhost:8080/api/v1/sessions/query/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({
          question: question,
          sessionId: sessionId,
          userId: 'user123'
        })
      });
      
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      
      let currentAnswer = '';
      
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        
        const chunk = decoder.decode(value);
        const lines = chunk.split('\n');
        
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.substring(6);
            
            if (line.includes('event: done')) {
              const doneData = JSON.parse(data);
              setSessionId(doneData.session_id);
            } else {
              currentAnswer += data;
              setAnswer(currentAnswer);
            }
          }
        }
      }
    } catch (error) {
      console.error('查询失败:', error);
      setAnswer('查询失败，请重试');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div>
      <input
        type="text"
        value={question}
        onChange={(e) => setQuestion(e.target.value)}
        placeholder="输入您的问题..."
      />
      <button onClick={handleStreamQuery} disabled={loading}>
        {loading ? '查询中...' : '发送'}
      </button>
      <div>
        <h3>答案：</h3>
        <p>{answer}</p>
      </div>
      {sessionId && <p>会话 ID: {sessionId}</p>}
    </div>
  );
}

export default ChatInterface;
```

## 参数说明

### 查询请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| question | string | 是 | 用户问题 |
| sessionId | string | 否 | 会话 ID，不提供则创建新会话 |
| userId | string | 否 | 用户 ID，默认为 "default" |
| topK | integer | 否 | 返回的文档块数量，默认为 5 |
| similarityThreshold | float | 否 | 相似度阈值，默认为 0.7 |

### 查询响应字段

| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | string | 会话 ID |
| answer | string | 生成的答案 |
| sources | array | 引用来源列表 |
| queryTime | float | 查询耗时（秒） |

### 引用来源字段

| 字段 | 类型 | 说明 |
|------|------|------|
| chunkText | string | 文档块文本 |
| similarityScore | float | 相似度分数 (0-1) |
| documentId | string | 文档 ID |
| documentName | string | 文档名称 |
| chunkIndex | integer | 文档块索引 |
| pageNumber | integer | 页码（如果适用） |
| section | string | 章节（如果适用） |

## 性能优化建议

### 1. 使用流式查询

对于需要实时反馈的场景，使用流式查询可以：
- 降低用户等待时间
- 提供更好的用户体验
- 减少超时风险

### 2. 复用会话

对于多轮对话场景：
- 始终传递 sessionId
- 系统会自动维护上下文
- 避免重复创建会话

### 3. 调整参数

根据实际需求调整参数：
- `topK`: 增加可能提高答案质量，但会增加响应时间
- `similarityThreshold`: 提高可以过滤低质量结果

### 4. 错误处理

实现完善的错误处理：
```javascript
try {
  const result = await syncQuery(question);
  // 处理成功响应
} catch (error) {
  if (error.response?.status === 503) {
    // 服务不可用，显示友好提示
    console.log('服务暂时不可用，请稍后重试');
  } else {
    // 其他错误
    console.error('查询失败:', error);
  }
}
```

## 故障排查

### 问题 1: 查询超时

**解决方案**:
1. 检查服务状态: `docker-compose ps`
2. 查看日志: `docker-compose logs -f gateway-service session-service rag-query-service`
3. 增加超时时间（如果需要）

### 问题 2: 流式查询中断

**解决方案**:
1. 检查网络连接
2. 确保客户端支持 SSE
3. 查看浏览器控制台错误

### 问题 3: 会话历史丢失

**解决方案**:
1. 确认 sessionId 正确传递
2. 检查 PostgreSQL 数据库连接
3. 查看 Session Service 日志

## 监控和调试

### 查看服务日志

```bash
# Gateway Service
docker-compose logs -f gateway-service

# Session Service
docker-compose logs -f session-service

# RAG Query Service
docker-compose logs -f rag-query-service
```

### 监控指标

访问 Prometheus: http://localhost:9090

查询指标：
```
# 查询 QPS
rate(http_server_requests_seconds_count{uri="/api/v1/sessions/query"}[1m])

# 平均响应时间
rate(http_server_requests_seconds_sum{uri="/api/v1/sessions/query"}[1m]) / 
rate(http_server_requests_seconds_count{uri="/api/v1/sessions/query"}[1m])
```

### Sentinel 监控

访问 Sentinel Dashboard: http://localhost:8858

监控：
- 实时 QPS
- 响应时间
- 异常比例
- 熔断状态

## 下一步

1. 阅读 [TASK_14_IMPLEMENTATION_SUMMARY.md](./TASK_14_IMPLEMENTATION_SUMMARY.md) 了解实现细节
2. 运行 `./verify-task-14.sh` 验证功能
3. 开发前端界面集成查询功能
4. 配置监控和告警

## 支持

如有问题，请查看：
- [README.md](./README.md) - 项目总体说明
- [SETUP.md](./SETUP.md) - 安装和配置指南
- 项目 Issues
