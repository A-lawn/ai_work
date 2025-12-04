# 快速开始指南

## 5 分钟快速部署

### 前置条件

- Docker 20.10+
- Docker Compose 2.0+
- 8GB+ 内存
- 50GB+ 磁盘空间

### 快速启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd rag-ops-qa-assistant

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件，设置 OPENAI_API_KEY

# 3. 启动系统
chmod +x *.sh
./start.sh

# 4. 访问系统
# 前端: http://localhost
# API: http://localhost:8080
```

### Windows 用户

```bash
# 使用 Git Bash
bash start.sh

# 或使用批处理文件
start.bat
```

## 常用命令

| 操作 | 命令 |
|------|------|
| 启动系统 | `./start.sh` |
| 启动（HTTPS） | `./start.sh --nginx` |
| 停止系统 | `./stop.sh` |
| 检查状态 | `./status.sh` |
| 查看日志 | `./logs.sh -f gateway-service` |
| 重启服务 | `./restart.sh gateway-service` |
| 扩展服务 | `./scale.sh rag-query-service 5` |

## 访问地址

### 应用服务

- **前端应用**: http://localhost
- **API 网关**: http://localhost:8080

### 管理控制台

- **Nacos**: http://localhost:8848/nacos (nacos/nacos)
- **Sentinel**: http://localhost:8858 (sentinel/sentinel)
- **RabbitMQ**: http://localhost:15672 (admin/admin123)
- **MinIO**: http://localhost:9001 (admin/admin123456)

### 监控系统

- **Zipkin**: http://localhost:9411
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)

## 服务架构

```
┌─────────────┐
│   前端 :80  │
└──────┬──────┘
       │
┌──────▼──────────────────────────────────┐
│   API 网关 :8080                         │
└──────┬──────────────────────────────────┘
       │
       ├─► 文档服务 :8081 (x2)
       ├─► 会话服务 :8082 (x2)
       ├─► 认证服务 :8083
       ├─► 监控服务 :8084
       ├─► 配置服务 :8085
       ├─► 文档处理 :9001 (x2)
       ├─► RAG查询 :9002 (x3)
       ├─► 嵌入模型 :9003 (x2)
       └─► 大模型 :9004 (x2)
```

## 故障排查

### 服务启动失败

```bash
# 查看日志
./logs.sh -n 200 <service-name>

# 重启服务
./restart.sh <service-name>
```

### 端口冲突

```bash
# 检查端口占用
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# 修改 docker-compose.yml 中的端口
```

### 内存不足

```bash
# 查看资源使用
docker stats

# 缩减副本数
./scale.sh rag-query-service 1
```

## 性能优化

### 高并发场景

```bash
# 扩展查询服务
./scale.sh rag-query-service 10
./scale.sh embedding-service 5
```

### 大量文档处理

```bash
# 扩展处理服务
./scale.sh document-processing-service 5
./scale.sh celery-worker 5
```

## 数据管理

### 备份数据

```bash
# 数据库备份
docker-compose exec postgres pg_dump -U postgres rag_db > backup.sql

# 向量数据备份
docker cp chroma:/chroma/chroma ./chroma_backup
```

### 清理数据

```bash
# 停止并删除所有数据（警告：不可恢复）
./stop.sh -v
```

## 下一步

- 📖 阅读 [完整部署指南](./DEPLOYMENT.md)
- 🛠️ 查看 [脚本使用指南](./SCRIPTS.md)
- 🔧 配置 [Nginx 负载均衡](./infrastructure/nginx/README.md)
- 📊 设置 [监控告警](./infrastructure/QUICKSTART.md)

## 获取帮助

```bash
# 查看脚本帮助
./start.sh --help
./logs.sh --help
./scale.sh --help
```

---

**提示**: 首次启动需要下载镜像，可能需要 5-10 分钟。后续启动只需 1-2 分钟。
