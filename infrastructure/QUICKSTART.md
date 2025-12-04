# 基础设施快速启动指南

本指南帮助您快速启动和验证微服务基础设施。

## 前置条件

- Docker 20.10+
- Docker Compose 2.0+
- 至少 8GB 可用内存
- 至少 20GB 可用磁盘空间

## 快速启动

### 1. 启动基础设施

```bash
# 启动所有基础设施服务
docker-compose up -d nacos sentinel-dashboard rabbitmq zipkin prometheus grafana postgres redis chroma elasticsearch minio

# 等待所有服务健康检查通过（约 2-3 分钟）
docker-compose ps
```

### 2. 初始化配置

```bash
# 初始化 Nacos 配置
docker-compose up nacos-init

# 初始化 RabbitMQ 队列
docker-compose up rabbitmq-init
```

### 3. 验证服务

访问以下地址验证服务是否正常：

| 服务 | 地址 | 账号 |
|------|------|------|
| Nacos | http://localhost:8848/nacos | nacos / nacos |
| Sentinel | http://localhost:8858 | sentinel / sentinel |
| RabbitMQ | http://localhost:15672 | admin / admin123 |
| Zipkin | http://localhost:9411 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3001 | admin / admin |
| MinIO | http://localhost:9001 | admin / admin123456 |

## 验证步骤

### 1. 验证 Nacos

```bash
# 检查命名空间
curl http://localhost:8848/nacos/v1/console/namespaces

# 检查配置
curl "http://localhost:8848/nacos/v1/cs/configs?tenant=rag-system&dataId=common-config.yaml&group=DEFAULT_GROUP"
```

### 2. 验证 RabbitMQ

登录管理界面，检查以下内容：
- 虚拟主机 `rag-system` 已创建
- 交换机列表包含：dlx.exchange, document.exchange, batch.exchange, log.exchange
- 队列列表包含：document.processing.queue, batch.processing.queue 等

### 3. 验证 Prometheus

```bash
# 检查 Prometheus 目标
curl http://localhost:9090/api/v1/targets

# 检查告警规则
curl http://localhost:9090/api/v1/rules
```

### 4. 验证 Grafana

1. 登录 Grafana
2. 检查数据源：Configuration → Data Sources → Prometheus
3. 检查仪表盘：Dashboards → RAG System Overview

## 常见问题

### Q: Nacos 启动失败

**A**: 检查端口占用和日志

```bash
# 检查端口
netstat -an | grep 8848

# 查看日志
docker-compose logs nacos
```

### Q: RabbitMQ 队列未创建

**A**: 手动运行初始化脚本

```bash
docker-compose up rabbitmq-init
```

### Q: Prometheus 无法抓取指标

**A**: 确保目标服务已启动并暴露指标端点

```bash
# 测试服务指标端点
curl http://localhost:8080/actuator/prometheus
```

### Q: Grafana 无数据

**A**: 检查 Prometheus 数据源配置和查询语句

## 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷
docker-compose down -v
```

## 下一步

基础设施启动成功后，可以继续：

1. 启动 Java 微服务
2. 启动 Python 微服务
3. 启动前端服务

参见主项目的 README.md 和 SETUP.md 文档。

## 监控和维护

### 查看服务状态

```bash
docker-compose ps
```

### 查看服务日志

```bash
# 查看所有日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f nacos
docker-compose logs -f rabbitmq
```

### 重启服务

```bash
# 重启单个服务
docker-compose restart nacos

# 重启所有服务
docker-compose restart
```

### 扩展服务

```bash
# 扩展到 3 个实例（示例）
docker-compose up -d --scale document-service=3
```

## 资源使用

预期资源使用（空闲状态）：

- **CPU**: 2-4 核
- **内存**: 4-6 GB
- **磁盘**: 5-10 GB

生产环境建议：

- **CPU**: 8+ 核
- **内存**: 16+ GB
- **磁盘**: 100+ GB (SSD)

## 技术支持

如遇问题，请查看：

1. 服务日志：`docker-compose logs <service-name>`
2. 健康检查：`docker-compose ps`
3. 详细文档：`infrastructure/README.md`
