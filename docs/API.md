# API 文档

## 目录

1. [API 概述](#1-api-概述)
2. [认证方式](#2-认证方式)
3. [文档管理 API](#3-文档管理-api)
4. [查询问答 API](#4-查询问答-api)
5. [会话管理 API](#5-会话管理-api)
6. [配置管理 API](#6-配置管理-api)
7. [监控日志 API](#7-监控日志-api)
8. [认证授权 API](#8-认证授权-api)
9. [错误码说明](#9-错误码说明)
10. [示例代码](#10-示例代码)

## 1. API 概述

### 1.1 基础信息

- **Base URL**: `http://localhost:8080` (开发环境)
- **Base URL**: `https://your-domain.com` (生产环境)
- **API 版本**: v1
- **数据格式**: JSON
- **字符编码**: UTF-8

### 1.2 通用响应格式

**成功响应**:
```json
{
  "code": 200,
  "message": "Success",
  "data": { ... }
}
```

**错误响应**:
```json
{
  "code": 4001,
  "message": "Document not found",
  "error": {
    "details": "Document with id xxx does not exist"
  }
}
```

### 1.3 HTTP 状态码

- `200 OK`: 请求成功
- `201 Created`: 资源创建成功
- `400 Bad Request`: 请求参数错误
- `401 Unauthorized`: 未认证
- `403 Forbidden`: 无权限
- `404 Not Found`: 资源不存在
- `429 Too Many Requests`: 请求过于频繁
- `500 Internal Server Error`: 服务器内部错误
- `503 Service Unavailable`: 服务不可用

### 1.4 自动生成的 API 文档

启动服务后，可以访问以下地址查看自动生成的 API 文档：

- **Java 服务**: `http://localhost:8080/swagger-ui.html`
- **Python 服务**: `http://localhost:9001/docs` (FastAPI 自动生成)

## 2. 认证方式

### 2.1 API Key 认证

在请求头中添加 API Key：

```http
X-API-Key: your-api-key-here
```

### 2.2 JWT 认证

在请求头中添加 JWT Token：

```http
Authorization: Bearer your-jwt-token-here
```

### 2.3 获取 API Key

```http
POST /api/v1/auth/api-keys
Authorization: Bearer your-jwt-token

{
  "name": "My API Key",
  "expiresAt": "2025-12-31T23:59:59Z"
}
```

## 3. 文档管理 API

### 3.1 上传文档

上传单个文档到系统。

**请求**:
```http
POST /api/v1/documents/upload
Content-Type: multipart/form-data
X-API-Key: your-api-key

file: <binary-file>
metadata: {"description": "运维手册"}
```

**响应**:
```json
{
  "code": 200,
  "message": "Document uploaded successfully",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "ops-manual.pdf",
    "fileType": "pdf",
    "fileSize": 1048576,
    "status": "PROCESSING",
    "uploadTime": "2024-01-01T12:00:00Z"
  }
}
```

**支持的文件格式**:
- PDF (.pdf)
- Word (.docx)
- 文本 (.txt)
- Markdown (.md)

**文件大小限制**: 50MB

### 3.2 批量上传文档

上传包含多个文档的 ZIP 文件。

**请求**:
```http
POST /api/v1/documents/batch-upload
Content-Type: multipart/form-data
X-API-Key: your-api-key

file: <zip-file>
```

**响应**:
```json
{
  "code": 200,
  "message": "Batch upload task created",
  "data": {
    "taskId": "batch-550e8400-e29b-41d4-a716-446655440000",
    "status": "PROCESSING",
    "totalFiles": 10
  }
}
```

### 3.3 查询批量任务状态

查询批量上传任务的处理状态。

**请求**:
```http
GET /api/v1/documents/batch-upload/{taskId}
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "taskId": "batch-550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "progress": 100,
    "totalFiles": 10,
    "processedFiles": 10,
    "successCount": 9,
    "failureCount": 1,
    "results": [
      {
        "filename": "doc1.pdf",
        "status": "SUCCESS",
        "documentId": "550e8400-e29b-41d4-a716-446655440001"
      },
      {
        "filename": "doc2.pdf",
        "status": "FAILED",
        "error": "Unsupported file format"
      }
    ]
  }
}
```

### 3.4 获取文档列表

分页查询文档列表。

**请求**:
```http
GET /api/v1/documents?page=1&pageSize=20&status=COMPLETED
X-API-Key: your-api-key
```

**查询参数**:
- `page`: 页码（从 1 开始）
- `pageSize`: 每页数量（默认 20，最大 100）
- `status`: 文档状态（PROCESSING, COMPLETED, FAILED）
- `fileType`: 文件类型（pdf, docx, txt, md）
- `keyword`: 搜索关键词

**响应**:
```json
{
  "code": 200,
  "data": {
    "documents": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "filename": "ops-manual.pdf",
        "fileType": "pdf",
        "fileSize": 1048576,
        "uploadTime": "2024-01-01T12:00:00Z",
        "chunkCount": 25,
        "status": "COMPLETED"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  }
}
```

### 3.5 获取文档详情

获取单个文档的详细信息。

**请求**:
```http
GET /api/v1/documents/{documentId}
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "filename": "ops-manual.pdf",
    "fileType": "pdf",
    "fileSize": 1048576,
    "uploadTime": "2024-01-01T12:00:00Z",
    "chunkCount": 25,
    "status": "COMPLETED",
    "metadata": {
      "description": "运维手册",
      "author": "Admin"
    }
  }
}
```

### 3.6 删除文档

删除文档及其所有向量数据。

**请求**:
```http
DELETE /api/v1/documents/{documentId}
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "message": "Document deleted successfully",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "deletedChunks": 25
  }
}
```

## 4. 查询问答 API

### 4.1 提交查询

提交问题并获取答案。

**请求**:
```http
POST /api/v1/query
Content-Type: application/json
X-API-Key: your-api-key

{
  "question": "如何重启 Nginx 服务？",
  "sessionId": "session-123",
  "topK": 5,
  "includeSources": true,
  "similarityThreshold": 0.7
}
```

**请求参数**:
- `question` (必填): 用户问题
- `sessionId` (可选): 会话 ID，用于多轮对话
- `topK` (可选): 返回的相关文档数量（默认 5）
- `includeSources` (可选): 是否包含引用来源（默认 true）
- `similarityThreshold` (可选): 相似度阈值（默认 0.7）

**响应**:
```json
{
  "code": 200,
  "data": {
    "answer": "重启 Nginx 服务的步骤如下：\n1. 使用命令 `sudo systemctl restart nginx`\n2. 或者使用 `sudo nginx -s reload` 重新加载配置\n3. 验证服务状态 `sudo systemctl status nginx`",
    "sessionId": "session-123",
    "sources": [
      {
        "chunkText": "Nginx 服务管理：使用 systemctl restart nginx 命令可以重启服务...",
        "similarityScore": 0.92,
        "documentName": "ops-manual.pdf",
        "pageNumber": 15,
        "documentId": "550e8400-e29b-41d4-a716-446655440000"
      },
      {
        "chunkText": "配置文件修改后需要重新加载：nginx -s reload...",
        "similarityScore": 0.85,
        "documentName": "nginx-guide.pdf",
        "pageNumber": 8,
        "documentId": "550e8400-e29b-41d4-a716-446655440001"
      }
    ],
    "metadata": {
      "retrievalTime": 0.5,
      "generationTime": 2.3,
      "totalTime": 2.8,
      "tokensUsed": 450
    }
  }
}
```

### 4.2 流式查询

使用 Server-Sent Events (SSE) 获取流式响应。

**请求**:
```http
POST /api/v1/query/stream
Content-Type: application/json
X-API-Key: your-api-key

{
  "question": "如何重启 Nginx 服务？",
  "sessionId": "session-123"
}
```

**响应** (SSE 流):
```
data: {"type": "sources", "data": [...]}

data: {"type": "token", "data": "重启"}

data: {"type": "token", "data": " Nginx"}

data: {"type": "token", "data": " 服务"}

data: {"type": "done", "data": {"tokensUsed": 450}}
```

## 5. 会话管理 API

### 5.1 创建会话

**请求**:
```http
POST /api/v1/sessions
Content-Type: application/json
X-API-Key: your-api-key

{
  "userId": "user-123"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "sessionId": "session-550e8400-e29b-41d4-a716-446655440000",
    "userId": "user-123",
    "createdAt": "2024-01-01T12:00:00Z"
  }
}
```

### 5.2 获取会话历史

**请求**:
```http
GET /api/v1/sessions/{sessionId}/history
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "sessionId": "session-123",
    "messages": [
      {
        "role": "user",
        "content": "如何重启 Nginx？",
        "timestamp": "2024-01-01T12:00:00Z"
      },
      {
        "role": "assistant",
        "content": "重启 Nginx 服务的步骤...",
        "timestamp": "2024-01-01T12:00:03Z",
        "metadata": {
          "sources": [...]
        }
      }
    ]
  }
}
```

### 5.3 删除会话

**请求**:
```http
DELETE /api/v1/sessions/{sessionId}
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "message": "Session deleted successfully"
}
```

## 6. 配置管理 API

### 6.1 获取系统配置

**请求**:
```http
GET /api/v1/config
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "chunkSize": 512,
    "chunkOverlap": 50,
    "topK": 5,
    "similarityThreshold": 0.7,
    "llmProvider": "openai",
    "llmModel": "gpt-4",
    "llmTemperature": 0.7,
    "llmMaxTokens": 1000,
    "embeddingProvider": "openai",
    "embeddingModel": "text-embedding-ada-002"
  }
}
```

### 6.2 更新系统配置

**请求**:
```http
PUT /api/v1/config
Content-Type: application/json
X-API-Key: your-api-key

{
  "chunkSize": 1024,
  "topK": 10,
  "llmTemperature": 0.5
}
```

**响应**:
```json
{
  "code": 200,
  "message": "Configuration updated successfully"
}
```

### 6.3 测试 LLM 连接

**请求**:
```http
POST /api/v1/config/test-llm
Content-Type: application/json
X-API-Key: your-api-key

{
  "provider": "openai",
  "apiKey": "sk-xxx",
  "endpoint": "https://api.openai.com/v1"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "message": "Connection successful",
    "latency": 0.5,
    "model": "gpt-4"
  }
}
```

## 7. 监控日志 API

### 7.1 获取操作日志

**请求**:
```http
GET /api/v1/logs?startTime=2024-01-01T00:00:00Z&endTime=2024-01-02T00:00:00Z&operationType=DOCUMENT_UPLOAD&page=1&pageSize=20
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "logs": [
      {
        "id": "log-123",
        "operationType": "DOCUMENT_UPLOAD",
        "userId": "user-123",
        "resourceId": "doc-456",
        "details": {
          "filename": "ops-manual.pdf",
          "fileSize": 1048576
        },
        "timestamp": "2024-01-01T12:00:00Z"
      }
    ],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

### 7.2 获取性能指标

**请求**:
```http
GET /api/v1/metrics?metricType=QUERY_TIME&startTime=2024-01-01T00:00:00Z&endTime=2024-01-02T00:00:00Z
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "metrics": [
      {
        "metricType": "QUERY_TIME",
        "metricValue": 2.5,
        "timestamp": "2024-01-01T12:00:00Z"
      }
    ]
  }
}
```

### 7.3 获取系统统计

**请求**:
```http
GET /api/v1/stats
X-API-Key: your-api-key
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "totalDocuments": 150,
    "totalChunks": 3750,
    "totalQueries": 5000,
    "avgQueryTime": 2.8,
    "avgLlmTime": 2.3,
    "avgRetrievalTime": 0.5,
    "totalTokensUsed": 1500000
  }
}
```

## 8. 认证授权 API

### 8.1 用户登录

**请求**:
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400,
    "user": {
      "id": "user-123",
      "username": "admin",
      "email": "admin@example.com"
    }
  }
}
```

### 8.2 刷新 Token

**请求**:
```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 86400
  }
}
```

### 8.3 创建 API Key

**请求**:
```http
POST /api/v1/auth/api-keys
Content-Type: application/json
Authorization: Bearer your-jwt-token

{
  "name": "Production API Key",
  "expiresAt": "2025-12-31T23:59:59Z"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": "key-123",
    "key": "rak_1234567890abcdef",
    "name": "Production API Key",
    "createdAt": "2024-01-01T12:00:00Z",
    "expiresAt": "2025-12-31T23:59:59Z"
  }
}
```

### 8.4 获取 API Key 列表

**请求**:
```http
GET /api/v1/auth/api-keys
Authorization: Bearer your-jwt-token
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "apiKeys": [
      {
        "id": "key-123",
        "name": "Production API Key",
        "createdAt": "2024-01-01T12:00:00Z",
        "expiresAt": "2025-12-31T23:59:59Z",
        "isActive": true
      }
    ]
  }
}
```

### 8.5 删除 API Key

**请求**:
```http
DELETE /api/v1/auth/api-keys/{keyId}
Authorization: Bearer your-jwt-token
```

**响应**:
```json
{
  "code": 200,
  "message": "API Key deleted successfully"
}
```

## 9. 错误码说明

### 9.1 通用错误码 (1xxx)

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| 1000 | 内部服务器错误 | 500 |
| 1001 | 请求参数无效 | 400 |
| 1002 | 未授权访问 | 401 |
| 1003 | 请求过于频繁 | 429 |
| 1004 | 服务不可用 | 503 |

### 9.2 文档相关错误码 (2xxx)

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| 2001 | 不支持的文件格式 | 400 |
| 2002 | 文件过大 | 400 |
| 2003 | 文档不存在 | 404 |
| 2004 | 文档处理失败 | 500 |

### 9.3 查询相关错误码 (3xxx)

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| 3001 | 查询内容为空 | 400 |
| 3002 | 未找到相关结果 | 404 |
| 3003 | LLM 服务错误 | 500 |
| 3004 | 嵌入服务错误 | 500 |

### 9.4 会话相关错误码 (4xxx)

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| 4001 | 会话不存在 | 404 |
| 4002 | 会话已过期 | 410 |

## 10. 示例代码

### 10.1 Python 示例

```python
import requests

# 配置
BASE_URL = "http://localhost:8080"
API_KEY = "your-api-key"

# 上传文档
def upload_document(file_path):
    url = f"{BASE_URL}/api/v1/documents/upload"
    headers = {"X-API-Key": API_KEY}
    files = {"file": open(file_path, "rb")}
    
    response = requests.post(url, headers=headers, files=files)
    return response.json()

# 查询问答
def query(question, session_id=None):
    url = f"{BASE_URL}/api/v1/query"
    headers = {
        "X-API-Key": API_KEY,
        "Content-Type": "application/json"
    }
    data = {
        "question": question,
        "sessionId": session_id,
        "includeSources": True
    }
    
    response = requests.post(url, headers=headers, json=data)
    return response.json()

# 使用示例
if __name__ == "__main__":
    # 上传文档
    result = upload_document("ops-manual.pdf")
    print(f"Document uploaded: {result['data']['documentId']}")
    
    # 查询
    answer = query("如何重启 Nginx 服务？")
    print(f"Answer: {answer['data']['answer']}")
```

### 10.2 JavaScript 示例

```javascript
const BASE_URL = "http://localhost:8080";
const API_KEY = "your-api-key";

// 上传文档
async function uploadDocument(file) {
  const formData = new FormData();
  formData.append("file", file);
  
  const response = await fetch(`${BASE_URL}/api/v1/documents/upload`, {
    method: "POST",
    headers: {
      "X-API-Key": API_KEY
    },
    body: formData
  });
  
  return await response.json();
}

// 查询问答
async function query(question, sessionId = null) {
  const response = await fetch(`${BASE_URL}/api/v1/query`, {
    method: "POST",
    headers: {
      "X-API-Key": API_KEY,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      question,
      sessionId,
      includeSources: true
    })
  });
  
  return await response.json();
}

// 流式查询
async function streamQuery(question, sessionId = null) {
  const response = await fetch(`${BASE_URL}/api/v1/query/stream`, {
    method: "POST",
    headers: {
      "X-API-Key": API_KEY,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      question,
      sessionId
    })
  });
  
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    const lines = chunk.split("\n");
    
    for (const line of lines) {
      if (line.startsWith("data: ")) {
        const data = JSON.parse(line.substring(6));
        console.log(data);
      }
    }
  }
}

// 使用示例
(async () => {
  // 上传文档
  const fileInput = document.querySelector('input[type="file"]');
  const result = await uploadDocument(fileInput.files[0]);
  console.log("Document uploaded:", result.data.documentId);
  
  // 查询
  const answer = await query("如何重启 Nginx 服务？");
  console.log("Answer:", answer.data.answer);
  
  // 流式查询
  await streamQuery("如何重启 Nginx 服务？");
})();
```

### 10.3 cURL 示例

```bash
# 上传文档
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "X-API-Key: your-api-key" \
  -F "file=@ops-manual.pdf"

# 查询问答
curl -X POST http://localhost:8080/api/v1/query \
  -H "X-API-Key: your-api-key" \
  -H "Content-Type: application/json" \
  -d '{
    "question": "如何重启 Nginx 服务？",
    "includeSources": true
  }'

# 获取文档列表
curl -X GET "http://localhost:8080/api/v1/documents?page=1&pageSize=20" \
  -H "X-API-Key: your-api-key"

# 删除文档
curl -X DELETE http://localhost:8080/api/v1/documents/{documentId} \
  -H "X-API-Key: your-api-key"
```

## 11. 速率限制

### 11.1 限制规则

- 文档上传: 10 次/分钟
- 查询请求: 60 次/分钟
- 其他 API: 100 次/分钟

### 11.2 响应头

```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 59
X-RateLimit-Reset: 1640995200
```

### 11.3 超出限制

当超出速率限制时，API 将返回 429 状态码：

```json
{
  "code": 1003,
  "message": "Rate limit exceeded",
  "error": {
    "retryAfter": 60
  }
}
```

## 12. Webhook (未来功能)

### 12.1 文档处理完成

```json
{
  "event": "document.processed",
  "timestamp": "2024-01-01T12:00:00Z",
  "data": {
    "documentId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "COMPLETED",
    "chunkCount": 25
  }
}
```

### 12.2 批量任务完成

```json
{
  "event": "batch.completed",
  "timestamp": "2024-01-01T12:00:00Z",
  "data": {
    "taskId": "batch-550e8400-e29b-41d4-a716-446655440000",
    "totalFiles": 10,
    "successCount": 9,
    "failureCount": 1
  }
}
```
