# Nginx 负载均衡配置

本目录包含用于生产环境的 Nginx 负载均衡和反向代理配置。

## 功能特性

- **HTTPS 支持**: 自动将 HTTP 请求重定向到 HTTPS
- **负载均衡**: 使用最少连接算法分发请求到后端服务
- **静态资源缓存**: 优化静态文件的传输和缓存
- **Gzip 压缩**: 减少传输数据量
- **安全头**: 添加安全相关的 HTTP 头
- **流式响应支持**: 支持 Server-Sent Events (SSE)
- **健康检查**: 提供健康检查端点
- **限流保护**: API 请求限流和连接数限制

## 文件说明

- `nginx.conf`: 主配置文件，包含完整的负载均衡和 HTTPS 配置
- `generate-ssl-cert.sh`: 生成自签名 SSL 证书的脚本（用于开发/测试）
- `ssl/`: SSL 证书存储目录

## 快速开始

### 1. 生成 SSL 证书

**开发/测试环境**（使用自签名证书）:

```bash
# Linux/Mac
chmod +x infrastructure/nginx/generate-ssl-cert.sh
./infrastructure/nginx/generate-ssl-cert.sh

# Windows (Git Bash)
bash infrastructure/nginx/generate-ssl-cert.sh
```

**生产环境**（使用 Let's Encrypt）:

```bash
# 安装 certbot
sudo apt-get install certbot

# 获取证书
sudo certbot certonly --standalone -d your-domain.com

# 复制证书到项目目录
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem infrastructure/nginx/ssl/cert.pem
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem infrastructure/nginx/ssl/key.pem
```

### 2. 启动 Nginx 负载均衡器

使用 Docker Compose 启动完整的系统（包含 Nginx）:

```bash
docker-compose -f docker-compose.yml -f docker-compose.nginx.yml up -d
```

### 3. 访问系统

- **HTTPS**: https://localhost
- **HTTP**: http://localhost (自动重定向到 HTTPS)
- **健康检查**: http://localhost/health

## 配置说明

### 负载均衡算法

当前使用 `least_conn`（最少连接）算法:

```nginx
upstream gateway_backend {
    least_conn;
    server gateway-service:8080 max_fails=3 fail_timeout=30s;
}
```

可选的负载均衡算法:
- `round_robin` (默认): 轮询
- `least_conn`: 最少连接
- `ip_hash`: IP 哈希（会话保持）
- `hash $request_uri`: URL 哈希

### SSL/TLS 配置

支持 TLS 1.2 和 TLS 1.3:

```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers 'ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256...';
```

### 限流配置

API 请求限流（100 请求/秒）:

```nginx
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/s;
```

在需要限流的 location 中启用:

```nginx
location /api/ {
    limit_req zone=api_limit burst=20 nodelay;
    limit_conn conn_limit 10;
    ...
}
```

### 缓存配置

静态资源缓存 1 年:

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot|webp)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

### 流式响应配置

SSE (Server-Sent Events) 端点配置:

```nginx
location /api/v1/query/stream {
    proxy_buffering off;
    proxy_cache off;
    proxy_read_timeout 600s;
    ...
}
```

## 性能优化

### 连接保持

```nginx
upstream gateway_backend {
    keepalive 32;  # 保持 32 个空闲连接
}
```

### Gzip 压缩

```nginx
gzip on;
gzip_comp_level 6;
gzip_types text/plain text/css application/json application/javascript;
```

### 缓冲设置

```nginx
proxy_buffering on;
proxy_buffer_size 4k;
proxy_buffers 8 4k;
```

## 安全配置

### 安全头

```nginx
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header X-Frame-Options "SAMEORIGIN" always;
add_header X-Content-Type-Options "nosniff" always;
add_header X-XSS-Protection "1; mode=block" always;
```

### 请求大小限制

```nginx
client_max_body_size 100M;  # 最大上传文件大小
```

### IP 白名单（可选）

限制监控端点访问:

```nginx
location /actuator/ {
    allow 10.0.0.0/8;
    deny all;
    ...
}
```

## 监控和日志

### 访问日志

默认位置: `/var/log/nginx/access.log`

自定义日志格式:

```nginx
log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                '$status $body_bytes_sent "$http_referer" '
                '"$http_user_agent" "$http_x_forwarded_for"';
```

### 错误日志

默认位置: `/var/log/nginx/error.log`

### 健康检查

```bash
curl http://localhost/health
```

## 故障排查

### 查看 Nginx 日志

```bash
docker-compose logs nginx
```

### 测试配置文件

```bash
docker-compose exec nginx nginx -t
```

### 重新加载配置

```bash
docker-compose exec nginx nginx -s reload
```

### 常见问题

**1. SSL 证书错误**

确保证书文件存在且权限正确:

```bash
ls -la infrastructure/nginx/ssl/
```

**2. 502 Bad Gateway**

检查后端服务是否正常运行:

```bash
docker-compose ps
curl http://gateway-service:8080/actuator/health
```

**3. 连接超时**

调整超时设置:

```nginx
proxy_connect_timeout 60s;
proxy_read_timeout 300s;
```

## 生产环境建议

1. **使用真实的 SSL 证书**: 使用 Let's Encrypt 或购买商业证书
2. **启用访问日志分析**: 使用 GoAccess 或 ELK Stack
3. **配置 CDN**: 使用 CloudFlare 或 AWS CloudFront
4. **启用 HTTP/2**: 已在配置中启用
5. **定期更新证书**: 设置自动续期
6. **监控 Nginx 性能**: 使用 Prometheus + Grafana
7. **配置 DDoS 防护**: 使用 fail2ban 或云服务商的 DDoS 防护
8. **启用日志轮转**: 配置 logrotate

## 扩展配置

### 添加新的后端服务

```nginx
upstream new_service {
    least_conn;
    server new-service:8080;
}

location /new-api/ {
    proxy_pass http://new_service;
    ...
}
```

### 配置多域名

```nginx
server {
    listen 443 ssl http2;
    server_name domain1.com;
    ...
}

server {
    listen 443 ssl http2;
    server_name domain2.com;
    ...
}
```

### 启用 WebSocket

已在配置中启用:

```nginx
proxy_http_version 1.1;
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

## 参考资料

- [Nginx 官方文档](https://nginx.org/en/docs/)
- [Nginx 负载均衡](https://docs.nginx.com/nginx/admin-guide/load-balancer/)
- [Let's Encrypt](https://letsencrypt.org/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
