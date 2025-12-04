# 运维手册

## 1. 日常运维

### 1.1 服务启动和停止

**启动所有服务**:
```bash
./start.sh
# 或
docker-compose up -d
```

**停止所有服务**:
```bash
./stop.sh
# 或
docker-compose down
```

**重启服务**:
```bash
./restart.sh [service-name]
# 或
docker-compose restart [service-name]
```

**查看服务状态**:
```bash
./status.sh
# 或
docker-compose ps
```

### 1.2 日志查看

**查看所有服务日志**:
```bash
./logs.sh
# 或
docker-compose logs -f
```

**查看特定服务日志**:
```bash
./logs.sh gateway-service
# 或
docker-compose logs -f gateway-service
```

**查看最近 100 行日志**:
```bash
docker-compose logs --tail=100 gateway-service
```

**搜索日志**:
```bash
docker-compose logs gateway-service | grep "ERROR"
```

### 1.3 服务扩缩容

**扩展服务实例**:
```bash
./scale.sh document-service 3
# 或
docker-compose up -d --scale document-service=3
```

**查看服务实例**:
```bash
docker-compose ps document-service
```

### 1.4 健康检查

**检查所有服务健康状态**:
```bash
./infrastructure/verify-infrastructure.sh
```

**检查单个服务**:
```bash
curl http://localhost:8080/actuator/health
```

**检查 Nacos 服务注册**:
访问 http://localhost:8848/nacos，查看服务列表

## 2. 监控和告警

### 2.1 Prometheus 监控

**访问 Prometheus**:
http://localhost:9090

**常用查询**:

1. **服务 QPS**:
```promql
rate(http_requests_total[5m])
```

2. **服务响应时间 P95**:
```promql
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
```

3. **服务错误率**:
```promql
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m])
```

4. **JVM 堆内存使用**:
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

5. **CPU 使用率**:
```promql
rate(process_cpu_seconds_total[5m])
```

### 2.2 Grafana 仪表盘

**访问 Grafana**:
http://localhost:3001 (admin/admin)

**导入仪表盘**:
1. 点击 "+" → "Import"
2. 上传 `infrastructure/grafana/dashboards/` 目录下的 JSON 文件
3. 选择 Prometheus 数据源
4. 点击 "Import"

**推荐仪表盘**:
- 服务概览: 显示所有服务的健康状态、QPS、响应时间
- JVM 监控: 显示 Java 服务的 JVM 指标
- 数据库监控: 显示 PostgreSQL 和 Redis 指标
- 业务指标: 显示文档数、查询数、Token 消耗

### 2.3 Sentinel 监控

**访问 Sentinel Dashboard**:
http://localhost:8858 (sentinel/sentinel)

**监控内容**:
- 实时 QPS
- 响应时间
- 限流和熔断统计
- 系统资源使用

**配置流控规则**:
1. 选择服务
2. 点击 "流控规则"
3. 点击 "新增流控规则"
4. 配置资源名、阈值类型、阈值
5. 点击 "新增"

**配置熔断规则**:
1. 选择服务
2. 点击 "熔断规则"
3. 点击 "新增熔断规则"
4. 配置资源名、熔断策略、阈值
5. 点击 "新增"

### 2.4 链路追踪

**访问 Zipkin**:
http://localhost:9411

**查看调用链路**:
1. 选择服务名称
2. 选择时间范围
3. 点击 "Find Traces"
4. 点击具体的 Trace 查看详情

**分析慢请求**:
1. 在搜索框中设置 "minDuration"
2. 查找耗时超过阈值的请求
3. 分析各个 Span 的耗时

### 2.5 告警配置

**Prometheus 告警规则**:
编辑 `infrastructure/prometheus/alerting_rules.yml`

**常用告警规则**:

1. **服务下线告警**:
```yaml
- alert: ServiceDown
  expr: up == 0
  for: 1m
  labels:
    severity: critical
  annotations:
    summary: "Service {{ $labels.job }} is down"
```

2. **高错误率告警**:
```yaml
- alert: HighErrorRate
  expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High error rate on {{ $labels.service }}"
```

3. **慢响应告警**:
```yaml
- alert: SlowResponse
  expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 3
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "Slow response on {{ $labels.service }}"
```

## 3. 故障排查

### 3.1 服务无法启动

**问题**: 服务启动失败

**排查步骤**:
1. 查看服务日志:
```bash
docker-compose logs [service-name]
```

2. 检查依赖服务是否正常:
```bash
docker-compose ps
```

3. 检查 Nacos 连接:
```bash
curl http://localhost:8848/nacos/
```

4. 检查数据库连接:
```bash
docker exec postgres pg_isready -U postgres
```

5. 检查端口占用:
```bash
netstat -tulpn | grep [port]
```

**常见原因**:
- Nacos 未启动或无法连接
- 数据库连接失败
- 端口被占用
- 配置错误

### 3.2 服务频繁重启

**问题**: 服务不断重启

**排查步骤**:
1. 查看容器状态:
```bash
docker ps -a | grep [service-name]
```

2. 查看容器资源使用:
```bash
docker stats [container-id]
```

3. 检查健康检查配置:
```bash
docker inspect [container-id] | grep -A 10 Healthcheck
```

4. 查看 OOM 日志:
```bash
dmesg | grep -i "out of memory"
```

**常见原因**:
- 内存不足（OOM）
- 健康检查失败
- 应用崩溃
- 配置错误

### 3.3 查询响应慢

**问题**: 查询接口响应时间过长

**排查步骤**:
1. 查看 Zipkin 链路追踪，定位慢的环节

2. 检查 ChromaDB 性能:
```bash
docker-compose logs chroma
```

3. 检查 LLM Service 响应时间:
```bash
curl -X POST http://localhost:9004/api/generate \
  -H "Content-Type: application/json" \
  -d '{"prompt": "test", "max_tokens": 10}'
```

4. 检查 Embedding Service 响应时间:
```bash
curl -X POST http://localhost:9003/api/embeddings \
  -H "Content-Type: application/json" \
  -d '{"texts": ["test"]}'
```

5. 检查 Redis 缓存命中率:
```bash
docker exec redis redis-cli info stats | grep keyspace
```

**优化建议**:
- 增加 Redis 缓存
- 优化向量检索参数（减少 top_k）
- 使用更快的 LLM 模型
- 增加服务实例数

### 3.4 文档处理失败

**问题**: 文档上传后处理失败

**排查步骤**:
1. 查看 Document Processing Service 日志:
```bash
docker-compose logs document-processing-service
```

2. 检查 RabbitMQ 消息队列:
访问 http://localhost:15672，查看队列状态

3. 检查 Celery Worker 状态:
```bash
docker-compose logs celery-worker
```

4. 检查文档格式是否支持

5. 检查 Embedding Service 是否可用

**常见原因**:
- 文档格式不支持或损坏
- Embedding Service 不可用
- ChromaDB 连接失败
- RabbitMQ 消息堆积

### 3.5 Sentinel 熔断触发

**问题**: 服务被 Sentinel 熔断

**排查步骤**:
1. 访问 Sentinel Dashboard，查看熔断统计

2. 查看被熔断的服务日志

3. 检查下游服务是否正常

4. 查看熔断规则配置是否合理

**恢复步骤**:
1. 修复下游服务问题
2. 等待熔断时间窗口结束（自动恢复）
3. 或手动调整熔断规则

### 3.6 数据库连接池耗尽

**问题**: 数据库连接池耗尽

**排查步骤**:
1. 查看数据库连接数:
```sql
SELECT count(*) FROM pg_stat_activity;
```

2. 查看长时间运行的查询:
```sql
SELECT pid, now() - pg_stat_activity.query_start AS duration, query 
FROM pg_stat_activity 
WHERE state = 'active' 
ORDER BY duration DESC;
```

3. 检查连接池配置:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
```

**解决方案**:
- 增加连接池大小
- 优化慢查询
- 杀死长时间运行的查询
- 检查连接泄漏

## 4. 数据备份和恢复

### 4.1 PostgreSQL 备份

**手动备份**:
```bash
docker exec postgres pg_dump -U postgres rag_db > backup_$(date +%Y%m%d).sql
```

**自动备份脚本**:
```bash
#!/bin/bash
BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

docker exec postgres pg_dump -U postgres rag_db > $BACKUP_DIR/rag_db_$DATE.sql
gzip $BACKUP_DIR/rag_db_$DATE.sql

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

**配置定时任务**:
```bash
crontab -e
# 每天凌晨 2 点执行备份
0 2 * * * /path/to/backup.sh
```

### 4.2 PostgreSQL 恢复

**恢复数据库**:
```bash
# 解压备份文件
gunzip backup_20240101.sql.gz

# 恢复数据
docker exec -i postgres psql -U postgres rag_db < backup_20240101.sql
```

### 4.3 ChromaDB 备份

**备份向量数据**:
```bash
docker cp chroma:/chroma/chroma ./backup/chroma_$(date +%Y%m%d)
```

**恢复向量数据**:
```bash
docker cp ./backup/chroma_20240101 chroma:/chroma/chroma
docker-compose restart chroma
```

### 4.4 MinIO 备份

**备份对象存储**:
```bash
docker exec minio mc mirror /data /backup/minio_$(date +%Y%m%d)
```

### 4.5 配置备份

**备份 Nacos 配置**:
```bash
# 导出所有配置
curl "http://localhost:8848/nacos/v1/cs/configs?export=true&group=DEFAULT_GROUP" \
  -o nacos_config_$(date +%Y%m%d).zip
```

## 5. 性能优化

### 5.1 JVM 调优

**推荐 JVM 参数**:
```bash
JAVA_OPTS="
  -Xms2g
  -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+HeapDumpOnOutOfMemoryError
  -XX:HeapDumpPath=/logs/heapdump.hprof
  -XX:+PrintGCDetails
  -XX:+PrintGCDateStamps
  -Xloggc:/logs/gc.log
"
```

### 5.2 数据库优化

**创建索引**:
```sql
-- 文档表索引
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_upload_time ON documents(upload_time);

-- 会话表索引
CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_created_at ON sessions(created_at);

-- 消息表索引
CREATE INDEX idx_messages_session_id ON messages(session_id);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
```

**分析查询性能**:
```sql
EXPLAIN ANALYZE SELECT * FROM documents WHERE status = 'COMPLETED';
```

### 5.3 Redis 优化

**配置持久化**:
```conf
# RDB 持久化
save 900 1
save 300 10
save 60 10000

# AOF 持久化
appendonly yes
appendfsync everysec
```

**配置内存淘汰策略**:
```conf
maxmemory 2gb
maxmemory-policy allkeys-lru
```

### 5.4 向量检索优化

**优化检索参数**:
- 减少 top_k 值（5 → 3）
- 提高相似度阈值（0.7 → 0.8）
- 启用结果缓存

**优化分块策略**:
- 调整 chunk_size（512 → 256）
- 减少 chunk_overlap（50 → 25）

## 6. 安全加固

### 6.1 修改默认密码

**PostgreSQL**:
```sql
ALTER USER postgres WITH PASSWORD 'new-strong-password';
```

**Redis**:
```bash
docker exec redis redis-cli CONFIG SET requirepass "new-strong-password"
```

**Nacos**:
访问 http://localhost:8848/nacos，修改 nacos 用户密码

### 6.2 配置防火墙

**只开放必要端口**:
```bash
# 允许 HTTP/HTTPS
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 允许 SSH
sudo ufw allow 22/tcp

# 启用防火墙
sudo ufw enable
```

### 6.3 配置 SSL/TLS

**生成 SSL 证书**:
```bash
cd infrastructure/nginx
./generate-ssl-cert.sh your-domain.com
```

**配置 Nginx HTTPS**:
见 [CONFIG.md](CONFIG.md) 的 Nginx 配置部分

### 6.4 定期更新

**更新 Docker 镜像**:
```bash
docker-compose pull
docker-compose up -d
```

**更新系统包**:
```bash
sudo apt update
sudo apt upgrade
```

## 7. 容量规划

### 7.1 资源需求估算

**单个服务资源需求**:
- Gateway Service: 1 CPU, 2GB 内存
- Document Service: 1 CPU, 2GB 内存
- Session Service: 1 CPU, 2GB 内存
- Python Services: 1 CPU, 2GB 内存

**基础设施资源需求**:
- PostgreSQL: 2 CPU, 4GB 内存, 100GB 磁盘
- Redis: 1 CPU, 2GB 内存
- ChromaDB: 2 CPU, 4GB 内存, 200GB 磁盘
- Elasticsearch: 2 CPU, 4GB 内存, 100GB 磁盘

**总计（最小配置）**:
- CPU: 16 核
- 内存: 32 GB
- 磁盘: 500 GB

### 7.2 扩容建议

**QPS < 100**:
- 每个服务 1 个实例
- 单机部署

**QPS 100-500**:
- 每个服务 2-3 个实例
- 单机或小集群部署

**QPS > 500**:
- 每个服务 3+ 个实例
- 集群部署
- 数据库读写分离
- Redis 集群

## 8. 运维检查清单

### 8.1 日常检查（每天）

- [ ] 检查所有服务状态
- [ ] 查看错误日志
- [ ] 检查磁盘空间
- [ ] 查看监控告警

### 8.2 周检查（每周）

- [ ] 检查数据库性能
- [ ] 清理过期日志
- [ ] 检查备份完整性
- [ ] 查看资源使用趋势

### 8.3 月检查（每月）

- [ ] 更新系统和依赖
- [ ] 审查安全配置
- [ ] 优化数据库索引
- [ ] 容量规划评估

### 8.4 季度检查（每季度）

- [ ] 灾难恢复演练
- [ ] 性能压测
- [ ] 安全审计
- [ ] 架构优化评估
