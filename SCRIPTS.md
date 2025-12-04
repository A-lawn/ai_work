# 管理脚本使用指南

本文档介绍智能运维问答助手的各种管理脚本及其使用方法。

## 脚本列表

| 脚本 | 功能 | 说明 |
|------|------|------|
| `start.sh` | 启动系统 | 启动所有微服务和基础设施 |
| `stop.sh` | 停止系统 | 停止所有服务 |
| `restart.sh` | 重启服务 | 重启指定服务或所有服务 |
| `status.sh` | 状态检查 | 检查所有服务的健康状态 |
| `logs.sh` | 查看日志 | 查看服务日志 |
| `scale.sh` | 扩缩容 | 动态调整服务副本数 |

## 详细使用说明

### 1. start.sh - 启动系统

启动所有微服务和基础设施组件。

**基本用法:**

```bash
# Linux/Mac
chmod +x start.sh
./start.sh

# Windows (Git Bash)
bash start.sh
```

**高级选项:**

```bash
# 启用 Nginx 负载均衡器（HTTPS 支持）
./start.sh --nginx

# 重新构建 Docker 镜像
./start.sh --build

# 扩展特定服务的副本数
./start.sh --scale rag-query-service=5

# 组合使用
./start.sh --nginx --build --scale rag-query-service=5
```

**启动后访问地址:**

- 前端应用: http://localhost (或 https://localhost 如果使用 --nginx)
- API 网关: http://localhost:8080
- Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)
- Sentinel 控制台: http://localhost:8858 (sentinel/sentinel)
- RabbitMQ 管理界面: http://localhost:15672 (admin/admin123)
- Zipkin 链路追踪: http://localhost:9411
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)
- MinIO 控制台: http://localhost:9001 (admin/admin123456)

### 2. stop.sh - 停止系统

停止所有运行中的服务。

**基本用法:**

```bash
# 停止所有服务（保留数据）
./stop.sh

# 停止服务并删除数据卷（警告：会删除所有数据）
./stop.sh -v
# 或
./stop.sh --volumes

# 停止服务并删除 Docker 镜像
./stop.sh --images

# 查看帮助
./stop.sh --help
```

**注意事项:**

- 默认情况下，数据卷会被保留，下次启动时数据会恢复
- 使用 `-v` 或 `--volumes` 选项会**永久删除**所有数据，包括：
  - 数据库数据
  - 向量数据库数据
  - 上传的文档
  - 日志文件
  - 缓存数据

### 3. restart.sh - 重启服务

重启指定服务或所有服务。

**基本用法:**

```bash
# 重启所有服务
./restart.sh

# 重启指定服务
./restart.sh gateway-service
./restart.sh rag-query-service
./restart.sh document-service

# 查看帮助
./restart.sh --help
```

**常用场景:**

```bash
# 配置更新后重启网关
./restart.sh gateway-service

# 代码更新后重启 Python 服务
./restart.sh rag-query-service

# 数据库连接问题，重启相关服务
./restart.sh postgres
./restart.sh document-service
```

### 4. status.sh - 状态检查

全面检查系统各组件的健康状态。

**基本用法:**

```bash
./status.sh
```

**检查内容:**

- ✓ 基础设施服务健康状态
- ✓ 数据存储服务状态
- ✓ Java 微服务健康检查
- ✓ Python 微服务健康检查
- ✓ 前端服务状态
- ✓ 容器资源使用情况（CPU、内存、网络）
- ✓ 服务副本数统计
- ✓ 磁盘使用情况

**输出示例:**

```
[✓] Nacos 服务注册中心 运行正常
[✓] Gateway API 网关服务 运行正常
[✗] Document 文档管理服务 无法访问
[!] Redis 缓存 容器状态: restarting
```

### 5. logs.sh - 查看日志

查看服务日志，支持实时跟踪和历史日志查看。

**基本用法:**

```bash
# 查看所有服务日志（最后 100 行）
./logs.sh

# 查看指定服务日志
./logs.sh gateway-service
./logs.sh rag-query-service

# 实时跟踪日志
./logs.sh -f gateway-service
./logs.sh --follow rag-query-service

# 查看最后 N 行日志
./logs.sh -n 50 document-service
./logs.sh --tail 200 gateway-service

# 组合使用
./logs.sh -f -n 50 rag-query-service

# 查看帮助
./logs.sh --help
```

**常用场景:**

```bash
# 调试网关路由问题
./logs.sh -f gateway-service

# 查看文档处理错误
./logs.sh -n 200 document-processing-service

# 监控 RAG 查询性能
./logs.sh -f rag-query-service

# 查看数据库连接日志
./logs.sh postgres

# 查看消息队列日志
./logs.sh rabbitmq
```

**可用服务列表:**

- `gateway-service` - API 网关
- `document-service` - 文档管理服务
- `session-service` - 会话管理服务
- `auth-service` - 认证授权服务
- `monitor-service` - 监控日志服务
- `config-service` - 配置管理服务
- `document-processing-service` - 文档处理服务
- `rag-query-service` - RAG 查询服务
- `embedding-service` - 嵌入模型服务
- `llm-service` - 大模型服务
- `celery-worker` - Celery 异步任务
- `batch-consumer` - 批量处理消费者
- `nacos` - Nacos 服务注册中心
- `sentinel-dashboard` - Sentinel 控制台
- `rabbitmq` - RabbitMQ 消息队列
- `postgres` - PostgreSQL 数据库
- `redis` - Redis 缓存
- `chroma` - ChromaDB 向量数据库

### 6. scale.sh - 服务扩缩容

动态调整服务的副本数，实现水平扩展。

**基本用法:**

```bash
# 扩展 RAG 查询服务到 5 个副本
./scale.sh rag-query-service 5

# 扩展文档服务到 3 个副本
./scale.sh document-service 3

# 扩展嵌入服务到 4 个副本
./scale.sh embedding-service 4

# 查看帮助
./scale.sh --help
```

**可扩展的服务:**

| 服务 | 默认副本数 | 推荐范围 | 说明 |
|------|-----------|---------|------|
| `document-service` | 2 | 2-5 | 文档管理服务 |
| `session-service` | 2 | 2-5 | 会话管理服务 |
| `document-processing-service` | 2 | 2-10 | 文档处理服务（CPU 密集） |
| `rag-query-service` | 3 | 3-10 | RAG 查询服务（高并发） |
| `embedding-service` | 2 | 2-5 | 嵌入模型服务 |
| `llm-service` | 2 | 2-5 | 大模型服务 |
| `celery-worker` | 2 | 2-10 | 异步任务处理 |

**扩缩容策略:**

```bash
# 高并发场景：扩展查询服务
./scale.sh rag-query-service 10

# 大量文档处理：扩展处理服务和 Worker
./scale.sh document-processing-service 5
./scale.sh celery-worker 5

# 降低资源使用：缩减到最小副本数
./scale.sh rag-query-service 1
./scale.sh embedding-service 1

# 恢复默认配置
./scale.sh rag-query-service 3
./scale.sh document-service 2
```

**注意事项:**

- 扩缩容不会中断现有请求
- 新副本会自动注册到 Nacos
- 负载均衡会自动分发请求到所有副本
- 缩容时会优雅关闭多余的副本

## 常见使用场景

### 场景 1: 首次部署

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，设置 OPENAI_API_KEY 等

# 2. 启动系统
./start.sh

# 3. 检查状态
./status.sh

# 4. 查看日志确认启动成功
./logs.sh -f gateway-service
```

### 场景 2: 生产环境部署（HTTPS）

```bash
# 1. 生成 SSL 证书（或使用 Let's Encrypt）
bash infrastructure/nginx/generate-ssl-cert.sh

# 2. 启动系统（启用 Nginx）
./start.sh --nginx

# 3. 访问 HTTPS 地址
# https://your-domain.com
```

### 场景 3: 代码更新

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建并启动
./start.sh --build

# 3. 检查服务状态
./status.sh
```

### 场景 4: 性能调优

```bash
# 1. 检查当前状态
./status.sh

# 2. 扩展高负载服务
./scale.sh rag-query-service 5
./scale.sh embedding-service 3

# 3. 监控日志
./logs.sh -f rag-query-service

# 4. 查看资源使用
docker stats
```

### 场景 5: 故障排查

```bash
# 1. 检查整体状态
./status.sh

# 2. 查看问题服务日志
./logs.sh -n 200 document-service

# 3. 重启问题服务
./restart.sh document-service

# 4. 持续监控
./logs.sh -f document-service
```

### 场景 6: 数据备份和恢复

```bash
# 备份数据
docker-compose exec postgres pg_dump -U postgres rag_db > backup.sql
docker cp chroma:/chroma/chroma ./chroma_backup

# 恢复数据
docker-compose exec -T postgres psql -U postgres rag_db < backup.sql
docker cp ./chroma_backup chroma:/chroma/chroma
```

### 场景 7: 完全清理和重新部署

```bash
# 1. 停止并删除所有数据
./stop.sh -v

# 2. 删除镜像（可选）
./stop.sh --images

# 3. 清理 Docker 系统
docker system prune -a --volumes

# 4. 重新部署
./start.sh --build
```

## 故障排查

### 问题 1: 服务启动失败

```bash
# 查看服务日志
./logs.sh -n 100 <service-name>

# 检查容器状态
docker-compose ps

# 重启服务
./restart.sh <service-name>
```

### 问题 2: 端口冲突

```bash
# 检查端口占用
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# 修改 docker-compose.yml 中的端口映射
# 然后重新启动
./stop.sh
./start.sh
```

### 问题 3: 内存不足

```bash
# 查看资源使用
docker stats

# 停止不必要的服务
docker-compose stop <service-name>

# 或缩减副本数
./scale.sh rag-query-service 1
```

### 问题 4: 网络连接问题

```bash
# 检查网络
docker network ls
docker network inspect rag-network

# 重建网络
./stop.sh
docker network prune
./start.sh
```

## 性能监控

### 实时监控

```bash
# 查看所有容器资源使用
docker stats

# 查看特定服务
docker stats gateway-service rag-query-service

# 持续监控日志
./logs.sh -f rag-query-service
```

### 使用监控工具

- **Prometheus**: http://localhost:9090
  - 查询指标: `http_requests_total`, `http_request_duration_seconds`
  
- **Grafana**: http://localhost:3001
  - 导入预配置的仪表盘
  - 创建自定义告警规则

- **Zipkin**: http://localhost:9411
  - 查看请求链路
  - 分析性能瓶颈

## 最佳实践

### 1. 日常运维

```bash
# 每天检查系统状态
./status.sh

# 定期查看日志
./logs.sh -n 100 gateway-service

# 监控资源使用
docker stats --no-stream
```

### 2. 性能优化

```bash
# 根据负载动态调整副本数
./scale.sh rag-query-service 5  # 高峰期
./scale.sh rag-query-service 2  # 低峰期

# 定期清理 Docker 资源
docker system prune -f
```

### 3. 安全建议

- 定期更新 `.env` 文件中的密码
- 使用真实的 SSL 证书（生产环境）
- 限制管理界面的访问（配置防火墙）
- 定期备份数据

### 4. 日志管理

```bash
# 定期清理日志
docker-compose logs --tail=0 -f > /dev/null

# 或配置日志轮转
# 在 docker-compose.yml 中添加:
# logging:
#   driver: "json-file"
#   options:
#     max-size: "10m"
#     max-file: "3"
```

## 参考资料

- [Docker Compose 文档](https://docs.docker.com/compose/)
- [项目 README](./README.md)
- [部署指南](./SETUP.md)
- [Nginx 配置说明](./infrastructure/nginx/README.md)
- [基础设施快速开始](./infrastructure/QUICKSTART.md)

## 获取帮助

每个脚本都支持 `--help` 选项：

```bash
./start.sh --help
./stop.sh --help
./logs.sh --help
./scale.sh --help
./restart.sh --help
```

如有问题，请查看日志或提交 Issue。
