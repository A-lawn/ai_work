# 常见问题解答 (FAQ)

## 1. 安装和部署

### Q1.1: Docker Compose 启动失败，提示端口被占用怎么办？

**A**: 检查端口占用情况：
```bash
# Linux/Mac
netstat -tulpn | grep [port]
lsof -i :[port]

# Windows
netstat -ano | findstr [port]
```

解决方案：
1. 停止占用端口的程序
2. 或修改 `docker-compose.yml` 中的端口映射

### Q1.2: 服务启动后无法访问 Nacos 控制台？

**A**: 可能原因：
1. Nacos 容器未启动：`docker-compose ps nacos`
2. 端口映射错误：检查 `docker-compose.yml` 中的端口配置
3. 防火墙阻止：`sudo ufw allow 8848/tcp`

### Q1.3: 内存不足导致容器被杀死？

**A**: 解决方案：
1. 增加 Docker 内存限制（Docker Desktop 设置）
2. 减少服务副本数
3. 调整服务资源限制：
```yaml
deploy:
  resources:
    limits:
      memory: 1G
```

### Q1.4: 如何在 Windows 上运行？

**A**: 
1. 安装 Docker Desktop for Windows
2. 启用 WSL 2
3. 使用 `start.bat` 脚本启动
4. 注意路径分隔符（使用 `\` 或 `/`）

### Q1.5: 如何配置代理访问 OpenAI API？

**A**: 在 `.env` 文件中配置：
```bash
HTTP_PROXY=http://proxy-server:port
HTTPS_PROXY=http://proxy-server:port
```

或在 Python 服务中配置：
```python
import os
os.environ['HTTP_PROXY'] = 'http://proxy-server:port'
os.environ['HTTPS_PROXY'] = 'http://proxy-server:port'
```

## 2. 配置问题

### Q2.1: 如何切换到 Azure OpenAI？

**A**: 修改 `.env` 文件：
```bash
LLM_PROVIDER=azure
AZURE_OPENAI_API_KEY=your-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT_NAME=gpt-4
```

然后在 Config Service 中更新配置。

### Q2.2: 如何使用本地 LLM 模型？

**A**: 
1. 部署本地模型服务（如 Ollama、vLLM）
2. 确保提供 OpenAI 兼容的 API 接口
3. 配置 `.env`：
```bash
LLM_PROVIDER=local
LOCAL_LLM_ENDPOINT=http://localhost:8000/v1
```

### Q2.3: 如何调整文档分块大小？

**A**: 修改配置：
```bash
CHUNK_SIZE=1024
CHUNK_OVERLAP=100
```

或通过 Config Service API 更新：
```bash
curl -X PUT http://localhost:8080/api/v1/config \
  -H "X-API-Key: your-key" \
  -H "Content-Type: application/json" \
  -d '{"chunkSize": 1024, "chunkOverlap": 100}'
```

### Q2.4: 如何修改 Sentinel 限流规则？

**A**: 
1. 访问 Sentinel Dashboard: http://localhost:8858
2. 选择服务
3. 点击"流控规则" → "新增流控规则"
4. 配置资源名、阈值类型、阈值
5. 规则会自动持久化到 Nacos

### Q2.5: 如何配置多个 Embedding 模型？

**A**: 目前系统支持单个 Embedding 模型。如需支持多个模型，需要：
1. 修改 Embedding Service 代码
2. 添加模型选择参数
3. 在配置中添加多个模型配置

## 3. 功能使用

### Q3.1: 支持哪些文档格式？

**A**: 目前支持：
- PDF (.pdf)
- Word (.docx)
- 文本 (.txt)
- Markdown (.md)

不支持：
- 图片格式（.jpg, .png）
- Excel (.xlsx)
- PPT (.pptx)
- 扫描件 PDF（需要 OCR）

### Q3.2: 文档上传大小限制是多少？

**A**: 默认限制 50MB。可以修改：

1. 修改 `.env`:
```bash
MAX_FILE_SIZE=104857600  # 100MB
```

2. 修改 Nginx 配置:
```nginx
client_max_body_size 100M;
```

### Q3.3: 如何实现多轮对话？

**A**: 
1. 创建会话：
```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "X-API-Key: your-key"
```

2. 使用会话 ID 进行查询：
```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "X-API-Key: your-key" \
  -H "Content-Type: application/json" \
  -d '{"question": "问题", "sessionId": "session-id"}'
```

### Q3.4: 如何查看文档处理进度？

**A**: 
1. 上传文档后获取 document_id
2. 查询文档状态：
```bash
curl http://localhost:8080/api/v1/documents/{document_id} \
  -H "X-API-Key: your-key"
```

状态说明：
- `PROCESSING`: 处理中
- `COMPLETED`: 处理完成
- `FAILED`: 处理失败

### Q3.5: 如何批量上传文档？

**A**: 
1. 将文档打包成 ZIP 文件
2. 上传 ZIP 文件：
```bash
curl -X POST http://localhost:8080/api/v1/documents/batch-upload \
  -H "X-API-Key: your-key" \
  -F "file=@documents.zip"
```

3. 查询任务状态：
```bash
curl http://localhost:8080/api/v1/documents/batch-upload/{task_id} \
  -H "X-API-Key: your-key"
```

### Q3.6: 查询结果不准确怎么办？

**A**: 优化建议：
1. 增加 top_k 值（5 → 10）
2. 降低相似度阈值（0.7 → 0.6）
3. 优化文档内容（添加更多相关文档）
4. 调整分块大小（512 → 256）
5. 使用更好的 Embedding 模型

### Q3.7: 如何删除文档？

**A**: 
```bash
curl -X DELETE http://localhost:8080/api/v1/documents/{document_id} \
  -H "X-API-Key: your-key"
```

注意：删除文档会同时删除所有相关的向量数据。

## 4. 性能问题

### Q4.1: 查询响应时间过长？

**A**: 排查步骤：
1. 查看 Zipkin 链路追踪，定位慢的环节
2. 检查 LLM API 响应时间
3. 检查向量检索性能
4. 启用 Redis 缓存
5. 增加服务实例数

### Q4.2: 文档处理速度慢？

**A**: 优化建议：
1. 增加 Celery Worker 数量
2. 使用更快的 Embedding 模型
3. 减少分块数量（增加 chunk_size）
4. 并行处理多个文档

### Q4.3: 数据库查询慢？

**A**: 
1. 添加索引：
```sql
CREATE INDEX idx_documents_status ON documents(status);
```

2. 分析慢查询：
```sql
EXPLAIN ANALYZE SELECT * FROM documents WHERE status = 'COMPLETED';
```

3. 优化查询语句
4. 增加数据库资源

### Q4.4: Redis 内存不足？

**A**: 
1. 增加 Redis 内存限制
2. 配置内存淘汰策略：
```conf
maxmemory 4gb
maxmemory-policy allkeys-lru
```

3. 清理过期缓存
4. 减少缓存 TTL

### Q4.5: 如何提高并发处理能力？

**A**: 
1. 增加服务实例数：
```bash
./scale.sh document-service 3
```

2. 配置负载均衡
3. 优化数据库连接池
4. 启用缓存
5. 使用异步处理

## 5. 错误处理

### Q5.1: 服务频繁重启？

**A**: 可能原因：
1. 内存不足（OOM）：增加内存限制
2. 健康检查失败：调整健康检查配置
3. 应用崩溃：查看日志定位问题
4. 配置错误：检查配置文件

### Q5.2: Sentinel 熔断触发？

**A**: 
1. 查看 Sentinel Dashboard，确认熔断原因
2. 检查下游服务是否正常
3. 调整熔断规则（如果规则过于严格）
4. 等待熔断时间窗口结束（自动恢复）

### Q5.3: 文档处理失败？

**A**: 常见原因：
1. 文档格式不支持：检查文件格式
2. 文档损坏：尝试修复或重新生成
3. Embedding Service 不可用：检查服务状态
4. ChromaDB 连接失败：检查 ChromaDB 状态

### Q5.4: API 返回 401 Unauthorized？

**A**: 
1. 检查 API Key 是否正确
2. 检查 API Key 是否过期
3. 检查请求头格式：`X-API-Key: your-key`
4. 检查 Auth Service 是否正常

### Q5.5: API 返回 429 Too Many Requests？

**A**: 
1. 请求过于频繁，触发限流
2. 等待一段时间后重试
3. 或调整 Sentinel 限流规则

## 6. 数据管理

### Q6.1: 如何备份数据？

**A**: 
1. PostgreSQL 备份：
```bash
docker exec postgres pg_dump -U postgres rag_db > backup.sql
```

2. ChromaDB 备份：
```bash
docker cp chroma:/chroma/chroma ./backup/chroma
```

3. MinIO 备份：
```bash
docker exec minio mc mirror /data /backup/minio
```

详见 [OPERATIONS.md](OPERATIONS.md) 的备份部分。

### Q6.2: 如何恢复数据？

**A**: 
1. PostgreSQL 恢复：
```bash
docker exec -i postgres psql -U postgres rag_db < backup.sql
```

2. ChromaDB 恢复：
```bash
docker cp ./backup/chroma chroma:/chroma/chroma
docker-compose restart chroma
```

### Q6.3: 如何清理过期数据？

**A**: 
1. 清理过期会话：
```sql
DELETE FROM sessions WHERE created_at < NOW() - INTERVAL '30 days';
```

2. 清理过期日志：
```sql
DELETE FROM operation_logs WHERE timestamp < NOW() - INTERVAL '90 days';
```

3. 清理 Redis 缓存：
```bash
docker exec redis redis-cli FLUSHDB
```

### Q6.4: 如何迁移数据到新环境？

**A**: 
1. 备份旧环境数据
2. 在新环境部署系统
3. 恢复数据到新环境
4. 验证数据完整性
5. 切换流量到新环境

### Q6.5: 数据库磁盘空间不足？

**A**: 
1. 清理过期数据
2. 压缩数据库：
```sql
VACUUM FULL;
```

3. 增加磁盘空间
4. 配置数据归档策略

## 7. 安全问题

### Q7.1: 如何修改默认密码？

**A**: 
1. PostgreSQL:
```sql
ALTER USER postgres WITH PASSWORD 'new-password';
```

2. Redis:
```bash
docker exec redis redis-cli CONFIG SET requirepass "new-password"
```

3. Nacos: 访问控制台修改

### Q7.2: 如何配置 HTTPS？

**A**: 
1. 生成 SSL 证书
2. 配置 Nginx（见 [CONFIG.md](CONFIG.md)）
3. 启动 Nginx:
```bash
docker-compose -f docker-compose.yml -f docker-compose.nginx.yml up -d
```

### Q7.3: 如何限制 API 访问？

**A**: 
1. 使用 API Key 认证
2. 配置 IP 白名单（Nginx）
3. 配置 Sentinel 限流规则
4. 启用 JWT 认证

### Q7.4: 如何防止 SQL 注入？

**A**: 
系统使用 ORM（JPA），自动防止 SQL 注入。
避免使用原生 SQL 查询，或使用参数化查询。

### Q7.5: 如何审计用户操作？

**A**: 
系统自动记录所有操作日志到 `operation_logs` 表。
查询日志：
```bash
curl http://localhost:8080/api/v1/logs \
  -H "X-API-Key: your-key"
```

## 8. 开发问题

### Q8.1: 如何添加新的文档格式支持？

**A**: 
1. 在 Document Processing Service 中添加新的 Parser
2. 实现 `BaseParser` 接口
3. 在 `ParserFactory` 中注册新的 Parser
4. 更新支持的格式列表

详见 [DEVELOPMENT.md](DEVELOPMENT.md)。

### Q8.2: 如何添加新的 LLM 提供商？

**A**: 
1. 在 LLM Service 中添加新的 Adapter
2. 实现 `BaseLLMAdapter` 接口
3. 在 `LLMService` 中注册新的 Adapter
4. 更新配置

### Q8.3: 如何调试微服务？

**A**: 
1. 本地启动服务（不使用 Docker）
2. 使用 IDE 的调试功能
3. 查看日志
4. 使用 Zipkin 追踪调用链路

### Q8.4: 如何运行单元测试？

**A**: 
Java 服务：
```bash
mvn test
```

Python 服务：
```bash
pytest
```

### Q8.5: 如何贡献代码？

**A**: 
1. Fork 项目
2. 创建特性分支
3. 提交代码
4. 创建 Pull Request
5. 等待 Code Review

## 9. 其他问题

### Q9.1: 系统支持多租户吗？

**A**: 
当前版本不支持多租户。如需多租户支持，需要：
1. 添加租户 ID 字段
2. 修改数据隔离逻辑
3. 添加租户管理功能

### Q9.2: 系统支持多语言吗？

**A**: 
系统支持中文和英文。LLM 可以理解和生成多种语言。
前端界面目前只支持中文。

### Q9.3: 如何监控 Token 消耗？

**A**: 
1. 查看 Monitor Service 统计：
```bash
curl http://localhost:8080/api/v1/stats \
  -H "X-API-Key: your-key"
```

2. 查看 Grafana 仪表盘的 Token 消耗图表

### Q9.4: 系统可以部署在 Kubernetes 上吗？

**A**: 
可以。需要：
1. 将 Docker Compose 配置转换为 Kubernetes YAML
2. 配置 Service、Deployment、ConfigMap、Secret
3. 配置 Ingress
4. 配置持久化存储（PV/PVC）

### Q9.5: 如何获取技术支持？

**A**: 
1. 查看文档：README.md、INSTALLATION.md、OPERATIONS.md
2. 查看日志：`docker-compose logs [service-name]`
3. 提交 Issue: <repository-url>/issues
4. 联系技术支持团队

## 10. 故障排查流程

### 通用排查流程

1. **确认问题**：明确问题现象和影响范围
2. **查看日志**：`docker-compose logs [service-name]`
3. **检查服务状态**：`docker-compose ps`
4. **检查资源使用**：`docker stats`
5. **检查网络连接**：`curl http://localhost:[port]/health`
6. **查看监控数据**：Grafana、Prometheus、Sentinel
7. **查看链路追踪**：Zipkin
8. **尝试重启服务**：`docker-compose restart [service-name]`
9. **查看配置**：检查环境变量和配置文件
10. **寻求帮助**：提交 Issue 或联系技术支持

### 紧急故障处理

1. **服务全部下线**：
   - 检查基础设施（Nacos、数据库）
   - 重启所有服务：`./restart.sh`

2. **数据库故障**：
   - 检查数据库状态
   - 恢复数据库备份
   - 切换到备用数据库

3. **性能严重下降**：
   - 检查资源使用
   - 启用 Sentinel 限流
   - 增加服务实例数
   - 清理缓存

4. **数据丢失**：
   - 停止服务
   - 恢复最近的备份
   - 验证数据完整性
   - 重启服务

---

如果您的问题未在此列出，请：
1. 查看其他文档：[README.md](../README.md)、[INSTALLATION.md](INSTALLATION.md)、[OPERATIONS.md](OPERATIONS.md)
2. 提交 Issue: <repository-url>/issues
3. 联系技术支持
