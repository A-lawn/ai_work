# 智能运维问答助手 - 部署指南

本文档提供完整的部署指南，包括 Docker 部署配置、Nginx 负载均衡、以及各种管理脚本的使用说明。

## 目录

- [系统架构](#系统架构)
- [部署前准备](#部署前准备)
- [快速开始](#快速开始)
- [Docker 部署](#docker-部署)
- [Nginx 负载均衡](#nginx-负载均衡)
- [管理脚本](#管理脚本)
- [服务扩缩容](#服务扩缩容)
- [监控和日志](#监控和日志)
- [故障排查](#故障排查)
- [生产环境建议](#生产环境建议)

## 系统架构

### 微服务架构

系统采用完整的微服务架构，包含以下组件：

**Java 微服务 (6个):**
- Gateway Service (8080) - API 网关
- Document Service (8081) - 文档管理
- Session Service (8082) - 会话管理
- Auth Service (8083) - 认证授权
- Monitor Service (8084) - 监控日志
- Config Service (8085) - 配置管理

**Python 微服务 (4个):**
- Document Processing Service (9001) - 文档处理
- RAG Query Service (9002) - RAG 查询
- Embedding Service (9003) - 嵌入模型
- LLM Service (9004) - 大模型服务

**基础设施:**
- Nacos (8848) - 服务注册与配置中心
- Sentinel Dashboard (8858) - 流量控制
- RabbitMQ (5672, 15672) - 消息队列
- Zipkin (9411) - 链路追踪
- Prometheus (9090) - 指标采集
- Grafana (3001) - 可视化监控

**数据存储:**
- PostgreSQL (5432) - 关系数据库
- Redis (6379) - 缓存
- ChromaDB (8001) - 向量数据库
- Elasticsearch (9200) - 日志存储
- MinIO (9000, 9001) - 对象存储

**前端:**
- React + TypeScript (80/443)

## 部署前准备

### 系统要求

**最低配置:**
- CPU: 4 核
- 内存: 8 GB
- 磁盘: 50 GB
- 操作系统: Linux / macOS / Windows 10+

**推荐配置:**
- CPU: 8 核
- 内存: 16 GB
- 磁盘: 100 GB SSD
- 操作系统: Linux (Ubuntu 20.04+)

### 软件依赖

1. **Docker** (20.10+)
   ```bash
   # Linux
   curl -fsSL https://get.docker.com | sh
   
   # macOS
   brew install docker
   
   # Windows
   # 下载并安装 Docker Desktop
   ```

2. **Docker Compose** (2.0+)
   ```bash
   # Linux
   sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
   sudo chmod +x /usr/local/bin/docker-compose
   
   # macOS / Windows
   # Docker Desktop 已包含 Docker Compose
   ```

3. **Git**
   ```bash
   # Linux
   sudo apt-get install git
   
   # macOS
   brew install git
   
   # Windows
   # 下载并安装 Git for Windows
   ```

### 环境配置

1. **克隆项目**
   ```bash
   git clone <repository-url>
   cd rag-ops-qa-assistant
   ```

2. **配置环境变量**
   ```bash
   cp .env.example .env
   ```

3. **编辑 .env 文件**
   ```bash
   # 必须配置
   OPENAI_API_KEY=sk-your-openai-api-key
   
   # 可选配置
   OPENAI_API_BASE=https://api.openai.com/v1
   AZURE_OPENAI_KEY=your-azure-key
   AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
   ```

## 快速开始

### Linux / macOS

```bash
# 1. 赋予脚本执行权限
chmod +x *.sh

# 2. 启动系统
./start.sh

# 3. 检查状态
./status.sh

# 4. 访问前端
# http://localhost
```

### Windows

```bash
# 使用 Git Bash
bash start.sh

# 或使用批处理文件
start.bat
```

## Docker 部署

### 部署架构

所有服务都通过 Docker Compose 编排，使用以下配置文件：

- `docker-compose.yml` - 主配置文件（所有服务）
- `docker-compose.nginx.yml` - Nginx 负载均衡扩展配置

### Dockerfile 说明

#### Java 微服务 Dockerfile

所有 Java 服务使用多阶段构建：

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY java-services/<service-name>/pom.xml java-services/<service-name>/
RUN mvn dependency:go-offline
COPY java-services/<service-name>/src java-services/<service-name>/src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/java-services/<service-name>/target/*.jar app.jar
HEALTHCHECK --interval=30s --timeout=3s CMD wget --spider http://localhost:<port>/actuator/health
EXPOSE <port>
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**特点:**
- 多阶段构建减小镜像体积
- 使用 Alpine Linux 基础镜像
- 内置健康检查
- JRE 而非 JDK（减小体积）

#### Python 微服务 Dockerfile

```dockerfile
FROM python:3.10-slim
WORKDIR /app
RUN apt-get update && apt-get install -y gcc g++ curl && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app/ ./app/
EXPOSE <port>
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:<port>/health
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "<port>"]
```

**特点:**
- 使用 slim 镜像减小体积
- 安装必要的编译工具
- 健康检查端点
- Uvicorn ASGI 服务器

#### 前端 Dockerfile

```dockerfile
# 构建阶段
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# 运行阶段
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK CMD wget --spider http://localhost/health
CMD ["nginx", "-g", "daemon off;"]
```

**特点:**
- 多阶段构建
- 生产环境优化的 Nginx 配置
- 静态资源缓存
- Gzip 压缩

### 服务依赖关系

```
基础设施层:
  ├─ Nacos (服务注册)
  ├─ Sentinel Dashboard (流量控制)
  ├─ RabbitMQ (消息队列)
  ├─ Zipkin (链路追踪)
  ├─ Prometheus (监控)
  └─ Grafana (可视化)

数据存储层:
  ├─ PostgreSQL (关系数据)
  ├─ Redis (缓存)
  ├─ ChromaDB (向量数据)
  ├─ Elasticsearch (日志)
  └─ MinIO (对象存储)

应用服务层:
  ├─ Gateway Service → Nacos, Sentinel, Redis
  ├─ Document Service → Nacos, PostgreSQL, Redis, MinIO
  ├─ Session Service → Nacos, PostgreSQL, Redis
  ├─ Auth Service → Nacos, PostgreSQL, Redis
  ├─ Monitor Service → Nacos, PostgreSQL, Elasticsearch
  ├─ Config Service → Nacos, PostgreSQL
  ├─ Document Processing → Nacos, ChromaDB, RabbitMQ
  ├─ RAG Query Service → Nacos, ChromaDB, Redis
  ├─ Embedding Service → Nacos, Redis
  └─ LLM Service → Nacos

前端层:
  └─ Frontend → Gateway Service
```

### 启动顺序

Docker Compose 会自动处理启动顺序，通过 `depends_on` 和健康检查确保：

1. 基础设施服务先启动（Nacos, PostgreSQL, Redis 等）
2. 等待基础设施服务健康检查通过
3. 启动应用服务（Java 和 Python 微服务）
4. 最后启动前端服务

## Nginx 负载均衡

### 启用 Nginx

```bash
# 生成 SSL 证书（开发环境）
bash infrastructure/nginx/generate-ssl-cert.sh

# 启动系统（启用 Nginx）
./start.sh --nginx
```

### Nginx 配置特性

- **HTTPS 支持**: 自动重定向 HTTP 到 HTTPS
- **负载均衡**: 使用最少连接算法
- **静态资源缓存**: 1 年缓存期
- **Gzip 压缩**: 减少传输数据量
- **安全头**: HSTS, X-Frame-Options 等
- **流式响应**: 支持 SSE (Server-Sent Events)
- **健康检查**: 自动检测后端服务状态
- **限流保护**: API 请求限流

### 配置文件位置

- 主配置: `infrastructure/nginx/nginx.conf`
- SSL 证书: `infrastructure/nginx/ssl/`
- 文档: `infrastructure/nginx/README.md`

### 访问地址

启用 Nginx 后：
- HTTPS: https://localhost
- HTTP: http://localhost (自动重定向到 HTTPS)

## 管理脚本

系统提供了一套完整的管理脚本，详见 [SCRIPTS.md](./SCRIPTS.md)。

### 核心脚本

| 脚本 | 功能 |
|------|------|
| `start.sh` / `start.bat` | 启动系统 |
| `stop.sh` / `stop.bat` | 停止系统 |
| `restart.sh` | 重启服务 |
| `status.sh` | 状态检查 |
| `logs.sh` | 查看日志 |
| `scale.sh` | 服务扩缩容 |

### 快速命令

```bash
# 启动
./start.sh

# 启动（启用 Nginx）
./start.sh --nginx

# 启动（重新构建）
./start.sh --build

# 检查状态
./status.sh

# 查看日志
./logs.sh -f gateway-service

# 扩展服务
./scale.sh rag-query-service 5

# 重启服务
./restart.sh gateway-service

# 停止
./stop.sh
```

## 服务扩缩容

### 支持扩缩容的服务

| 服务 | 默认副本 | 推荐范围 |
|------|---------|---------|
| document-service | 2 | 2-5 |
| session-service | 2 | 2-5 |
| document-processing-service | 2 | 2-10 |
| rag-query-service | 3 | 3-10 |
| embedding-service | 2 | 2-5 |
| llm-service | 2 | 2-5 |
| celery-worker | 2 | 2-10 |

### 扩缩容命令

```bash
# 扩展到 5 个副本
./scale.sh rag-query-service 5

# 缩减到 1 个副本
./scale.sh rag-query-service 1

# 查看当前副本数
docker-compose ps
```

### 自动扩缩容（Kubernetes）

如果部署到 Kubernetes，可以使用 HPA (Horizontal Pod Autoscaler):

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rag-query-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-query-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

## 监控和日志

### 监控系统

**Prometheus** (http://localhost:9090)
- 指标采集和存储
- 查询语言 PromQL
- 告警规则配置

**Grafana** (http://localhost:3001)
- 可视化仪表盘
- 告警通知
- 数据源集成

**Zipkin** (http://localhost:9411)
- 分布式链路追踪
- 性能分析
- 依赖关系图

### 日志管理

**查看日志:**
```bash
# 实时跟踪
./logs.sh -f gateway-service

# 查看最后 100 行
./logs.sh -n 100 document-service

# 查看所有服务
./logs.sh
```

**Elasticsearch + Kibana:**
- 日志聚合和搜索
- 日志分析和可视化
- 告警配置

### 关键指标

**服务健康:**
- 服务可用性
- 响应时间 (P50, P95, P99)
- 错误率
- QPS (每秒请求数)

**资源使用:**
- CPU 使用率
- 内存使用率
- 磁盘 I/O
- 网络流量

**业务指标:**
- 文档处理数量
- 查询请求数
- Token 消耗
- 用户活跃度

## 故障排查

### 常见问题

#### 1. 服务启动失败

```bash
# 查看日志
./logs.sh -n 200 <service-name>

# 检查依赖服务
./status.sh

# 重启服务
./restart.sh <service-name>
```

#### 2. 端口冲突

```bash
# 检查端口占用
netstat -ano | findstr :8080  # Windows
lsof -i :8080                 # Linux/Mac

# 修改端口映射
# 编辑 docker-compose.yml
# 重新启动
./stop.sh
./start.sh
```

#### 3. 内存不足

```bash
# 查看资源使用
docker stats

# 缩减副本数
./scale.sh rag-query-service 1
./scale.sh embedding-service 1

# 停止不必要的服务
docker-compose stop grafana
```

#### 4. 数据库连接失败

```bash
# 检查 PostgreSQL
docker-compose logs postgres

# 重启数据库
./restart.sh postgres

# 检查连接配置
cat .env | grep DATABASE
```

#### 5. Nacos 注册失败

```bash
# 检查 Nacos
curl http://localhost:8848/nacos/

# 查看服务日志
./logs.sh -f gateway-service

# 重启 Nacos
./restart.sh nacos
```

### 调试技巧

**进入容器:**
```bash
docker-compose exec gateway-service sh
docker-compose exec postgres psql -U postgres rag_db
```

**查看网络:**
```bash
docker network inspect rag-network
```

**查看卷:**
```bash
docker volume ls
docker volume inspect rag-ops-qa-assistant_postgres_data
```

**清理资源:**
```bash
# 清理未使用的资源
docker system prune -f

# 清理所有数据（警告：会删除数据）
./stop.sh -v
```

## 生产环境建议

### 安全配置

1. **使用真实的 SSL 证书**
   ```bash
   # Let's Encrypt
   sudo certbot certonly --standalone -d your-domain.com
   ```

2. **修改默认密码**
   - 数据库密码
   - Redis 密码
   - 管理界面密码
   - JWT 密钥

3. **配置防火墙**
   ```bash
   # 只开放必要端口
   ufw allow 80/tcp
   ufw allow 443/tcp
   ufw enable
   ```

4. **启用访问控制**
   - API Key 认证
   - IP 白名单
   - 速率限制

### 性能优化

1. **数据库优化**
   - 配置连接池
   - 添加索引
   - 定期 VACUUM

2. **缓存策略**
   - Redis 缓存热点数据
   - CDN 加速静态资源
   - 浏览器缓存

3. **服务扩展**
   - 根据负载动态扩缩容
   - 使用负载均衡
   - 配置服务降级

### 备份策略

1. **数据库备份**
   ```bash
   # 每日备份
   docker-compose exec postgres pg_dump -U postgres rag_db > backup_$(date +%Y%m%d).sql
   ```

2. **向量数据备份**
   ```bash
   docker cp chroma:/chroma/chroma ./chroma_backup_$(date +%Y%m%d)
   ```

3. **配置备份**
   ```bash
   tar -czf config_backup_$(date +%Y%m%d).tar.gz .env infrastructure/nacos/config/
   ```

### 监控告警

1. **配置告警规则**
   - 服务下线告警
   - 高错误率告警
   - 资源使用告警
   - 慢查询告警

2. **告警通知**
   - 邮件通知
   - 短信通知
   - Slack/钉钉通知

### 日志管理

1. **日志轮转**
   ```yaml
   # docker-compose.yml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

2. **日志聚合**
   - 使用 ELK Stack
   - 配置日志过滤
   - 设置日志保留期

### 高可用部署

1. **多节点部署**
   - 使用 Docker Swarm 或 Kubernetes
   - 配置服务副本
   - 实现故障转移

2. **数据库高可用**
   - PostgreSQL 主从复制
   - Redis Sentinel
   - 定期备份

3. **负载均衡**
   - Nginx 负载均衡
   - 云服务商负载均衡
   - DNS 负载均衡

## 参考资料

- [项目 README](./README.md)
- [安装指南](./SETUP.md)
- [脚本使用指南](./SCRIPTS.md)
- [Nginx 配置](./infrastructure/nginx/README.md)
- [基础设施快速开始](./infrastructure/QUICKSTART.md)
- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)

## 获取帮助

如有问题，请：
1. 查看日志: `./logs.sh -f <service-name>`
2. 检查状态: `./status.sh`
3. 查看文档: 本文档和相关 README
4. 提交 Issue: GitHub Issues

---

**版本**: 1.0.0  
**更新日期**: 2024-12-04  
**维护者**: RAG Ops Team
