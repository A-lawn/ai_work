# 安装和部署指南

## 目录

1. [环境要求](#1-环境要求)
2. [本地开发环境搭建](#2-本地开发环境搭建)
3. [Docker Compose 部署](#3-docker-compose-部署)
4. [生产环境部署](#4-生产环境部署)
5. [配置说明](#5-配置说明)
6. [验证部署](#6-验证部署)
7. [常见问题](#7-常见问题)

## 1. 环境要求

### 1.1 硬件要求

**最低配置**:
- CPU: 4 核
- 内存: 16 GB
- 磁盘: 100 GB

**推荐配置**:
- CPU: 8 核
- 内存: 32 GB
- 磁盘: 500 GB SSD

### 1.2 软件要求

**Docker Compose 部署**:
- Docker 20.10+
- Docker Compose 2.0+
- Git

**本地开发**:
- Java 17+
- Maven 3.8+
- Python 3.10+
- Node.js 18+
- PostgreSQL 15+
- Redis 7+

### 1.3 网络要求

- 能够访问 OpenAI API（或配置代理）
- 能够访问 Docker Hub（或配置镜像加速）
- 开放必要的端口（见端口列表）

### 1.4 端口列表

**应用服务**:
- 80: Nginx (前端)
- 443: Nginx (HTTPS)
- 8080: Gateway Service
- 8081: Document Service
- 8082: Session Service
- 8083: Auth Service
- 8084: Monitor Service
- 8085: Config Service
- 9001: Document Processing Service
- 9002: RAG Query Service
- 9003: Embedding Service
- 9004: LLM Service

**基础设施**:
- 5432: PostgreSQL
- 6379: Redis
- 8848: Nacos
- 8858: Sentinel Dashboard
- 5672: RabbitMQ
- 15672: RabbitMQ Management
- 9411: Zipkin
- 9090: Prometheus
- 3001: Grafana
- 9000: MinIO API
- 9001: MinIO Console
- 9200: Elasticsearch
- 8001: ChromaDB

## 2. 本地开发环境搭建

### 2.1 克隆项目

```bash
git clone <repository-url>
cd rag-ops-qa-assistant
```

### 2.2 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，配置必要的环境变量：

```bash
# OpenAI 配置（必填）
OPENAI_API_KEY=sk-your-api-key-here
OPENAI_API_BASE=https://api.openai.com/v1

# Azure OpenAI 配置（可选）
AZURE_OPENAI_API_KEY=your-azure-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/

# 数据库配置
POSTGRES_DB=rag_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# Redis 配置
REDIS_PASSWORD=

# MinIO 配置
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=admin123456

# RabbitMQ 配置
RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=admin123
```

### 2.3 启动基础设施

使用 Docker Compose 启动基础设施服务：

```bash
# 只启动基础设施服务
docker-compose up -d postgres redis nacos sentinel-dashboard rabbitmq zipkin prometheus grafana minio elasticsearch chroma
```

等待所有服务启动完成（约 2-3 分钟）。

### 2.4 初始化数据库

```bash
# 连接到 PostgreSQL
docker exec -it rag-ops-qa-assistant-postgres-1 psql -U postgres -d rag_db

# 执行初始化脚本
\i /docker-entrypoint-initdb.d/init.sql

# 退出
\q
```

### 2.5 配置 Nacos

访问 Nacos 控制台: http://localhost:8848/nacos

用户名/密码: nacos/nacos

导入配置文件：
1. 进入"配置管理" → "配置列表"
2. 点击"导入配置"
3. 选择 `infrastructure/nacos/config/` 目录下的所有 YAML 文件
4. 确认导入

### 2.6 启动 Java 服务

#### 方式一：使用 Maven

```bash
# 构建父项目
mvn clean install -DskipTests

# 启动各个服务（在不同的终端窗口）
cd java-services/gateway-service && mvn spring-boot:run
cd java-services/document-service && mvn spring-boot:run
cd java-services/session-service && mvn spring-boot:run
cd java-services/auth-service && mvn spring-boot:run
cd java-services/monitor-service && mvn spring-boot:run
cd java-services/config-service && mvn spring-boot:run
```

#### 方式二：使用 IDE

在 IntelliJ IDEA 或 Eclipse 中：
1. 导入项目为 Maven 项目
2. 等待依赖下载完成
3. 运行各个服务的 Application 主类

### 2.7 启动 Python 服务

每个 Python 服务都需要独立的虚拟环境：

#### Document Processing Service

```bash
cd python-services/document-processing-service

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 启动服务
uvicorn app.main:app --reload --port 9001

# 启动 RabbitMQ 消费者（新终端）
python -m app.rabbitmq_consumer

# 启动 Celery Worker（新终端）
celery -A app.celery_app worker --loglevel=info
```

#### RAG Query Service

```bash
cd python-services/rag-query-service

python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

uvicorn app.main:app --reload --port 9002
```

#### Embedding Service

```bash
cd python-services/embedding-service

python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

uvicorn app.main:app --reload --port 9003
```

#### LLM Service

```bash
cd python-services/llm-service

python -m venv venv
source venv/bin/activate
pip install -r requirements.txt

uvicorn app.main:app --reload --port 9004
```

### 2.8 启动前端

```bash
cd frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端将在 http://localhost:5173 启动。

### 2.9 验证本地环境

访问以下地址验证服务是否正常：

- Nacos: http://localhost:8848/nacos
- Gateway: http://localhost:8080/actuator/health
- 前端: http://localhost:5173

## 3. Docker Compose 部署

### 3.1 快速启动

```bash
# 克隆项目
git clone <repository-url>
cd rag-ops-qa-assistant

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件，配置 OPENAI_API_KEY

# 启动所有服务
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

### 3.2 分步启动

如果需要分步启动，可以按以下顺序：

```bash
# 1. 启动基础设施
docker-compose up -d postgres redis nacos sentinel-dashboard rabbitmq zipkin prometheus grafana minio elasticsearch chroma

# 等待 2-3 分钟，确保基础设施启动完成

# 2. 初始化 Nacos 配置
./infrastructure/nacos/init-nacos.sh

# 3. 启动 Java 服务
docker-compose up -d gateway-service document-service session-service auth-service monitor-service config-service

# 4. 启动 Python 服务
docker-compose up -d document-processing-service rag-query-service embedding-service llm-service celery-worker

# 5. 启动前端
docker-compose up -d frontend nginx
```

### 3.3 使用启动脚本

项目提供了便捷的启动脚本：

**Linux/Mac**:
```bash
# 启动所有服务
./start.sh

# 停止所有服务
./stop.sh

# 重启服务
./restart.sh

# 查看服务状态
./status.sh

# 查看日志
./logs.sh [service-name]

# 扩展服务
./scale.sh [service-name] [replicas]
```

**Windows**:
```cmd
# 启动所有服务
start.bat

# 停止所有服务
stop.bat
```

### 3.4 服务健康检查

等待所有服务启动完成（约 5-10 分钟），然后验证：

```bash
# 检查所有服务状态
docker-compose ps

# 验证基础设施
curl http://localhost:8848/nacos/  # Nacos
curl http://localhost:9411/        # Zipkin
curl http://localhost:9090/        # Prometheus

# 验证 Java 服务
curl http://localhost:8080/actuator/health  # Gateway
curl http://localhost:8081/actuator/health  # Document Service
curl http://localhost:8082/actuator/health  # Session Service
curl http://localhost:8083/actuator/health  # Auth Service
curl http://localhost:8084/actuator/health  # Monitor Service
curl http://localhost:8085/actuator/health  # Config Service

# 验证 Python 服务
curl http://localhost:9001/health  # Document Processing
curl http://localhost:9002/health  # RAG Query
curl http://localhost:9003/health  # Embedding
curl http://localhost:9004/health  # LLM

# 验证前端
curl http://localhost/
```

### 3.5 访问控制台

- 前端应用: http://localhost
- Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)
- Sentinel 控制台: http://localhost:8858 (sentinel/sentinel)
- RabbitMQ 管理界面: http://localhost:15672 (admin/admin123)
- Zipkin 链路追踪: http://localhost:9411
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001 (admin/admin)
- MinIO 控制台: http://localhost:9001 (admin/admin123456)

## 4. 生产环境部署

### 4.1 准备工作

**服务器要求**:
- 操作系统: Ubuntu 20.04+ / CentOS 8+ / RHEL 8+
- 已安装 Docker 和 Docker Compose
- 已配置防火墙规则
- 已配置域名和 SSL 证书

**安全加固**:
1. 修改所有默认密码
2. 配置防火墙，只开放必要端口
3. 启用 HTTPS
4. 配置日志轮转
5. 定期备份数据

### 4.2 配置 HTTPS

#### 生成 SSL 证书

**使用 Let's Encrypt**:
```bash
# 安装 certbot
sudo apt-get install certbot

# 生成证书
sudo certbot certonly --standalone -d your-domain.com
```

**使用自签名证书（测试）**:
```bash
cd infrastructure/nginx
./generate-ssl-cert.sh your-domain.com
```

#### 配置 Nginx

编辑 `docker-compose.nginx.yml`:

```yaml
nginx:
  image: nginx:alpine
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - ./infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf
    - ./infrastructure/nginx/ssl:/etc/nginx/ssl
    - /etc/letsencrypt:/etc/letsencrypt  # Let's Encrypt 证书
```

启动 Nginx:
```bash
docker-compose -f docker-compose.yml -f docker-compose.nginx.yml up -d
```

### 4.3 配置环境变量

生产环境的 `.env` 文件示例：

```bash
# 应用配置
ENVIRONMENT=production
LOG_LEVEL=INFO

# 数据库配置（使用强密码）
POSTGRES_DB=rag_db
POSTGRES_USER=rag_user
POSTGRES_PASSWORD=<strong-password>

# Redis 配置
REDIS_PASSWORD=<strong-password>

# MinIO 配置
MINIO_ROOT_USER=admin
MINIO_ROOT_PASSWORD=<strong-password>

# RabbitMQ 配置
RABBITMQ_DEFAULT_USER=admin
RABBITMQ_DEFAULT_PASS=<strong-password>

# OpenAI 配置
OPENAI_API_KEY=<your-api-key>
OPENAI_API_BASE=https://api.openai.com/v1

# 安全配置
JWT_SECRET=<random-secret-key>
API_KEY_SALT=<random-salt>

# 资源限制
MAX_FILE_SIZE=52428800  # 50MB
MAX_UPLOAD_FILES=100
```

### 4.4 配置资源限制

编辑 `docker-compose.yml`，为每个服务添加资源限制：

```yaml
services:
  gateway-service:
    # ...
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
      replicas: 2
```

### 4.5 配置数据持久化

确保所有数据卷都正确配置：

```yaml
volumes:
  postgres_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/postgres
  
  redis_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/redis
  
  # ... 其他数据卷
```

### 4.6 配置日志

配置日志驱动和日志轮转：

```yaml
services:
  gateway-service:
    # ...
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "10"
```

### 4.7 启动生产环境

```bash
# 拉取最新镜像
docker-compose pull

# 启动服务
docker-compose up -d

# 验证服务
docker-compose ps
docker-compose logs -f

# 检查健康状态
./infrastructure/verify-infrastructure.sh
```

### 4.8 配置监控告警

#### Prometheus 告警规则

编辑 `infrastructure/prometheus/alerting_rules.yml`：

```yaml
groups:
  - name: service_alerts
    interval: 30s
    rules:
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
      
      - alert: HighErrorRate
        expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High error rate on {{ $labels.service }}"
```

#### Grafana 仪表盘

1. 访问 Grafana: http://your-domain:3001
2. 添加 Prometheus 数据源
3. 导入仪表盘模板（位于 `infrastructure/grafana/dashboards/`）

### 4.9 配置备份

#### 数据库备份脚本

创建 `backup.sh`:

```bash
#!/bin/bash

BACKUP_DIR="/backup/postgres"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份 PostgreSQL
docker exec rag-ops-qa-assistant-postgres-1 pg_dump -U postgres rag_db > $BACKUP_DIR/rag_db_$DATE.sql

# 压缩备份
gzip $BACKUP_DIR/rag_db_$DATE.sql

# 删除 7 天前的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

echo "Backup completed: rag_db_$DATE.sql.gz"
```

配置定时任务：

```bash
# 编辑 crontab
crontab -e

# 每天凌晨 2 点执行备份
0 2 * * * /path/to/backup.sh
```

## 5. 配置说明

### 5.1 Nacos 配置

详见 [CONFIG.md](CONFIG.md) 文档。

### 5.2 Sentinel 配置

详见 [OPERATIONS.md](OPERATIONS.md) 文档。

### 5.3 环境变量说明

详见 [CONFIG.md](CONFIG.md) 文档。

## 6. 验证部署

### 6.1 基础验证

```bash
# 运行验证脚本
./infrastructure/verify-infrastructure.sh

# 验证文档上传
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -H "X-API-Key: your-api-key" \
  -F "file=@test.pdf"

# 验证查询
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key" \
  -d '{"question": "测试问题"}'
```

### 6.2 性能测试

使用 Apache Bench 进行简单的性能测试：

```bash
# 测试 Gateway 吞吐量
ab -n 1000 -c 10 http://localhost:8080/actuator/health

# 测试查询接口
ab -n 100 -c 5 -p query.json -T application/json \
  -H "X-API-Key: your-api-key" \
  http://localhost:8080/api/v1/query
```

### 6.3 端到端测试

```bash
# 运行端到端测试脚本
./verify-task-14.sh
./verify-batch-processing.sh
```

## 7. 常见问题

### 7.1 服务启动失败

**问题**: 服务无法连接到 Nacos

**解决方案**:
1. 确认 Nacos 已启动: `docker-compose ps nacos`
2. 检查 Nacos 日志: `docker-compose logs nacos`
3. 验证网络连接: `docker network ls`
4. 重启服务: `docker-compose restart [service-name]`

### 7.2 内存不足

**问题**: 容器因内存不足被 OOM Killer 杀死

**解决方案**:
1. 增加 Docker 内存限制
2. 调整服务资源配置
3. 减少服务副本数
4. 优化 JVM 参数

### 7.3 端口冲突

**问题**: 端口已被占用

**解决方案**:
1. 查看端口占用: `netstat -tulpn | grep [port]`
2. 修改 docker-compose.yml 中的端口映射
3. 停止冲突的服务

### 7.4 数据库连接失败

**问题**: 服务无法连接到 PostgreSQL

**解决方案**:
1. 确认 PostgreSQL 已启动
2. 检查数据库配置（用户名、密码、数据库名）
3. 验证网络连接
4. 检查防火墙规则

### 7.5 OpenAI API 调用失败

**问题**: 无法调用 OpenAI API

**解决方案**:
1. 验证 API Key 是否正确
2. 检查网络连接（可能需要代理）
3. 查看 LLM Service 日志
4. 尝试使用 Azure OpenAI 或本地模型

### 7.6 向量检索失败

**问题**: ChromaDB 查询失败

**解决方案**:
1. 确认 ChromaDB 已启动
2. 检查文档是否已成功处理
3. 验证 Embedding Service 是否可用
4. 查看 Document Processing Service 日志

### 7.7 前端无法访问后端

**问题**: 前端调用 API 失败

**解决方案**:
1. 检查 Gateway 是否正常运行
2. 验证 CORS 配置
3. 检查 API Key 是否正确
4. 查看浏览器控制台错误信息

## 8. 升级指南

### 8.1 升级步骤

```bash
# 1. 备份数据
./backup.sh

# 2. 拉取最新代码
git pull origin main

# 3. 拉取最新镜像
docker-compose pull

# 4. 停止服务
docker-compose down

# 5. 启动服务
docker-compose up -d

# 6. 验证升级
./infrastructure/verify-infrastructure.sh
```

### 8.2 回滚步骤

```bash
# 1. 停止服务
docker-compose down

# 2. 切换到旧版本
git checkout <old-version-tag>

# 3. 恢复数据（如果需要）
./restore.sh <backup-file>

# 4. 启动服务
docker-compose up -d
```

## 9. 卸载

```bash
# 停止并删除所有容器
docker-compose down

# 删除所有数据卷（警告：会删除所有数据）
docker-compose down -v

# 删除所有镜像
docker-compose down --rmi all

# 删除项目目录
cd ..
rm -rf rag-ops-qa-assistant
```

## 10. 技术支持

如遇到问题，请：
1. 查看日志: `docker-compose logs [service-name]`
2. 查看文档: [README.md](../README.md)
3. 提交 Issue: <repository-url>/issues
4. 联系技术支持

## 附录

### A. 完整的端口列表

见 [1.4 端口列表](#14-端口列表)

### B. 环境变量完整列表

见 [CONFIG.md](CONFIG.md)

### C. 服务依赖关系

```
Gateway → Document Service → Document Processing Service → Embedding Service
                                                          → ChromaDB
       → Session Service → RAG Query Service → Embedding Service
                                             → LLM Service
                                             → ChromaDB
       → Auth Service
       → Config Service → LLM Service
       → Monitor Service
```
