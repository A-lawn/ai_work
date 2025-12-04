# 配置说明文档

## 1. 环境变量配置

### 1.1 核心配置

```bash
# 应用环境
ENVIRONMENT=production          # development, staging, production
LOG_LEVEL=INFO                 # DEBUG, INFO, WARN, ERROR

# OpenAI 配置（必填）
OPENAI_API_KEY=sk-xxx          # OpenAI API Key
OPENAI_API_BASE=https://api.openai.com/v1

# Azure OpenAI 配置（可选）
AZURE_OPENAI_API_KEY=xxx
AZURE_OPENAI_ENDPOINT=https://xxx.openai.azure.com/
AZURE_OPENAI_DEPLOYMENT_NAME=gpt-4

# 本地模型配置（可选）
LOCAL_LLM_ENDPOINT=http://localhost:8000/v1
LOCAL_EMBEDDING_ENDPOINT=http://localhost:8001/v1
```

### 1.2 数据库配置

```bash
# PostgreSQL
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
POSTGRES_DB=rag_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<strong-password>

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=<strong-password>
REDIS_DB=0

# Elasticsearch
ELASTICSEARCH_HOST=elasticsearch
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_USERNAME=elastic
ELASTICSEARCH_PASSWORD=<strong-password>

# ChromaDB
CHROMA_HOST=chroma
CHROMA_PORT=8000

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=<strong-password>
MINIO_BUCKET=documents
```

### 1.3 微服务配置

```bash
# Nacos
NACOS_SERVER=nacos:8848
NACOS_NAMESPACE=rag-system
NACOS_GROUP=DEFAULT_GROUP
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

# Sentinel
SENTINEL_DASHBOARD=sentinel-dashboard:8858

# RabbitMQ
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=admin
RABBITMQ_PASSWORD=<strong-password>
RABBITMQ_VHOST=/

# Zipkin
ZIPKIN_BASE_URL=http://zipkin:9411
```

### 1.4 应用配置

```bash
# 文档处理
MAX_FILE_SIZE=52428800          # 50MB
SUPPORTED_FORMATS=pdf,docx,txt,md
CHUNK_SIZE=512
CHUNK_OVERLAP=50

# 检索配置
TOP_K=5
SIMILARITY_THRESHOLD=0.7
ENABLE_RERANKING=true

# LLM 配置
LLM_PROVIDER=openai             # openai, azure, local
LLM_MODEL=gpt-4
LLM_TEMPERATURE=0.7
LLM_MAX_TOKENS=1000
LLM_TIMEOUT=60

# 嵌入模型配置
EMBEDDING_PROVIDER=openai       # openai, local
EMBEDDING_MODEL=text-embedding-ada-002
EMBEDDING_DIMENSION=1536

# 安全配置
JWT_SECRET=<random-secret-key>
JWT_EXPIRATION=86400            # 24 hours
API_KEY_SALT=<random-salt>
```

## 2. Nacos 配置

### 2.1 公共配置 (common-config.yaml)

位置: `infrastructure/nacos/config/common-config.yaml`

```yaml
# 数据库配置
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/rag_db
    username: postgres
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  # Redis 配置
  redis:
    host: redis
    port: 6379
    password: ${REDIS_PASSWORD}
    database: 0
  
  # RabbitMQ 配置
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: admin
    password: ${RABBITMQ_PASSWORD}

# Sentinel 配置
spring:
  cloud:
    sentinel:
      transport:
        dashboard: sentinel-dashboard:8858
      datasource:
        ds1:
          nacos:
            server-addr: nacos:8848
            dataId: ${spring.application.name}-flow-rules
            groupId: SENTINEL_GROUP
            rule-type: flow

# Zipkin 配置
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: "*"
  metrics:
    export:
      prometheus:
        enabled: true
```

### 2.2 服务专属配置

每个服务都有独立的配置文件，例如 `gateway-service.yaml`:

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      routes:
        - id: document-service
          uri: lb://document-service
          predicates:
            - Path=/api/v1/documents/**
          filters:
            - StripPrefix=0
        
        - id: session-service
          uri: lb://session-service
          predicates:
            - Path=/api/v1/sessions/**,/api/v1/query/**
          filters:
            - StripPrefix=0

# 限流配置
spring:
  cloud:
    sentinel:
      filter:
        enabled: true
      scg:
        fallback:
          mode: response
          response-status: 429
          response-body: '{"code": 1003, "message": "Too many requests"}'
```

## 3. Sentinel 规则配置

### 3.1 流控规则

在 Sentinel Dashboard 中配置，或通过 Nacos 配置：

```json
[
  {
    "resource": "/api/v1/documents/upload",
    "limitApp": "default",
    "grade": 1,
    "count": 10,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "/api/v1/query",
    "limitApp": "default",
    "grade": 1,
    "count": 60,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

**参数说明**:
- `resource`: 资源名称（接口路径）
- `grade`: 限流阈值类型（0-线程数，1-QPS）
- `count`: 限流阈值
- `strategy`: 流控模式（0-直接，1-关联，2-链路）
- `controlBehavior`: 流控效果（0-快速失败，1-Warm Up，2-排队等待）

### 3.2 熔断规则

```json
[
  {
    "resource": "DocumentProcessingClient#processDocument",
    "grade": 0,
    "count": 0.5,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.5
  }
]
```

**参数说明**:
- `grade`: 熔断策略（0-慢调用比例，1-异常比例，2-异常数）
- `count`: 阈值
- `timeWindow`: 熔断时长（秒）
- `minRequestAmount`: 最小请求数
- `slowRatioThreshold`: 慢调用比例阈值

### 3.3 系统保护规则

```json
[
  {
    "highestSystemLoad": 8.0,
    "avgRt": 3000,
    "maxThread": 100,
    "qps": 1000,
    "highestCpuUsage": 0.8
  }
]
```

## 4. 应用配置文件

### 4.1 application.yml (Java 服务)

```yaml
spring:
  application:
    name: gateway-service
  
  profiles:
    active: ${ENVIRONMENT:development}
  
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:rag-system}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:rag-system}
        group: ${NACOS_GROUP:DEFAULT_GROUP}
        file-extension: yaml
        shared-configs:
          - data-id: common-config.yaml
            refresh: true

server:
  port: ${SERVER_PORT:8080}

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.rag.ops: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 4.2 config.py (Python 服务)

```python
import os
from pydantic import BaseSettings

class Settings(BaseSettings):
    # 应用配置
    APP_NAME: str = "document-processing-service"
    APP_VERSION: str = "1.0.0"
    ENVIRONMENT: str = os.getenv("ENVIRONMENT", "development")
    LOG_LEVEL: str = os.getenv("LOG_LEVEL", "INFO")
    
    # Nacos 配置
    NACOS_SERVER: str = os.getenv("NACOS_SERVER", "localhost:8848")
    NACOS_NAMESPACE: str = os.getenv("NACOS_NAMESPACE", "rag-system")
    NACOS_GROUP: str = os.getenv("NACOS_GROUP", "DEFAULT_GROUP")
    
    # ChromaDB 配置
    CHROMA_HOST: str = os.getenv("CHROMA_HOST", "localhost")
    CHROMA_PORT: int = int(os.getenv("CHROMA_PORT", "8000"))
    
    # RabbitMQ 配置
    RABBITMQ_URL: str = os.getenv("RABBITMQ_URL", "amqp://admin:admin123@localhost:5672/")
    
    # OpenAI 配置
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    OPENAI_API_BASE: str = os.getenv("OPENAI_API_BASE", "https://api.openai.com/v1")
    
    # 文档处理配置
    CHUNK_SIZE: int = int(os.getenv("CHUNK_SIZE", "512"))
    CHUNK_OVERLAP: int = int(os.getenv("CHUNK_OVERLAP", "50"))
    
    class Config:
        case_sensitive = True

settings = Settings()
```

## 5. Docker Compose 配置

### 5.1 服务配置示例

```yaml
services:
  gateway-service:
    image: rag-ops/gateway-service:latest
    build:
      context: ./java-services/gateway-service
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - NACOS_SERVER=nacos:8848
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    depends_on:
      - nacos
      - postgres
      - redis
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
      replicas: 2
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "10"
    networks:
      - rag-network

networks:
  rag-network:
    driver: bridge

volumes:
  postgres_data:
  redis_data:
  chroma_data:
```

## 6. Nginx 配置

### 6.1 nginx.conf

```nginx
upstream gateway {
    least_conn;
    server gateway-service:8080 max_fails=3 fail_timeout=30s;
}

server {
    listen 80;
    listen 443 ssl http2;
    server_name your-domain.com;
    
    # SSL 配置
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    
    # 重定向 HTTP 到 HTTPS
    if ($scheme != "https") {
        return 301 https://$server_name$request_uri;
    }
    
    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲配置
        proxy_buffering off;
        proxy_request_buffering off;
    }
    
    # SSE 支持
    location /api/v1/query/stream {
        proxy_pass http://gateway;
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;
        proxy_buffering off;
        proxy_cache off;
    }
    
    # 文件上传大小限制
    client_max_body_size 50M;
    
    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
}
```

## 7. Prometheus 配置

### 7.1 prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - alertmanager:9093

rule_files:
  - "/etc/prometheus/alerting_rules.yml"

scrape_configs:
  # Java 服务
  - job_name: 'gateway-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['gateway-service:8080']
  
  - job_name: 'document-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['document-service:8081']
  
  # Python 服务
  - job_name: 'document-processing-service'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['document-processing-service:9001']
  
  # 基础设施
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
  
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

## 8. 配置最佳实践

### 8.1 安全配置

1. **使用强密码**: 所有数据库和服务密码至少 16 位
2. **加密敏感配置**: 使用 Nacos 配置加密功能
3. **定期轮换密钥**: JWT Secret 和 API Key Salt 定期更换
4. **最小权限原则**: 数据库用户只授予必要权限

### 8.2 性能配置

1. **JVM 参数优化**:
```bash
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

2. **连接池配置**:
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

3. **Redis 缓存配置**:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1 hour
```

### 8.3 监控配置

1. **启用所有 Actuator 端点**
2. **配置 Prometheus 指标采集**
3. **设置合理的告警阈值**
4. **配置日志级别和轮转**

### 8.4 高可用配置

1. **服务多实例部署**: 至少 2 个实例
2. **数据库主从复制**: PostgreSQL 主从配置
3. **Redis 哨兵模式**: 高可用 Redis 集群
4. **负载均衡**: Nginx 或 HAProxy

## 9. 配置验证

### 9.1 验证脚本

```bash
#!/bin/bash

echo "验证配置..."

# 检查环境变量
if [ -z "$OPENAI_API_KEY" ]; then
    echo "错误: OPENAI_API_KEY 未设置"
    exit 1
fi

# 检查 Nacos 连接
curl -f http://localhost:8848/nacos/ || {
    echo "错误: 无法连接到 Nacos"
    exit 1
}

# 检查数据库连接
docker exec postgres pg_isready -U postgres || {
    echo "错误: 无法连接到 PostgreSQL"
    exit 1
}

echo "配置验证通过"
```

### 9.2 配置检查清单

- [ ] 所有环境变量已设置
- [ ] 数据库密码已修改
- [ ] OpenAI API Key 已配置
- [ ] Nacos 配置已导入
- [ ] Sentinel 规则已配置
- [ ] SSL 证书已配置（生产环境）
- [ ] 防火墙规则已配置
- [ ] 日志轮转已配置
- [ ] 备份策略已配置
- [ ] 监控告警已配置
