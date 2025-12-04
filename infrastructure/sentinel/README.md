# Sentinel 监控配置指南

## 概述

本目录包含 Sentinel Dashboard 的配置文件和规则定义。Sentinel 提供流量控制、熔断降级、系统保护等功能。

## 访问 Sentinel Dashboard

- **URL**: http://localhost:8858
- **用户名**: sentinel
- **密码**: sentinel

## 规则配置

### 1. 流控规则（Flow Control Rules）

流控规则用于限制 QPS 或并发线程数，防止系统过载。

#### 配置位置
- Nacos 配置中心: `SENTINEL_GROUP` 组
- 配置文件: `flow-rules-{service-name}.json`

#### 规则类型
- **QPS 限流**: 限制每秒请求数
- **并发线程数限流**: 限制同时处理的请求数

#### 示例配置
```json
{
  "resource": "/api/v1/query",
  "limitApp": "default",
  "grade": 1,
  "count": 100,
  "strategy": 0,
  "controlBehavior": 0
}
```

### 2. 熔断规则（Circuit Breaker Rules）

熔断规则用于在服务异常时自动降级，保护系统稳定性。

#### 熔断策略
- **慢调用比例**: 当慢调用比例超过阈值时熔断
- **异常比例**: 当异常比例超过阈值时熔断
- **异常数**: 当异常数超过阈值时熔断

#### 示例配置
```json
{
  "resource": "document-processing-service",
  "grade": 0,
  "count": 1.0,
  "timeWindow": 10,
  "minRequestAmount": 5,
  "slowRatioThreshold": 0.5
}
```

### 3. 系统保护规则（System Protection Rules）

系统保护规则从系统整体维度进行保护。

#### 保护维度
- **Load**: 系统负载
- **CPU 使用率**: CPU 使用百分比
- **平均 RT**: 平均响应时间
- **并发线程数**: 系统并发线程数
- **入口 QPS**: 系统入口 QPS

#### 示例配置
```json
{
  "highestSystemLoad": 10.0,
  "highestCpuUsage": 0.8,
  "avgRt": 3000,
  "maxThread": 500,
  "qps": 1000
}
```

### 4. 热点参数限流规则（Hot Parameter Rules）

热点参数限流针对特定参数值进行限流。

#### 示例配置
```json
{
  "resource": "/api/v1/documents/{id}",
  "grade": 1,
  "paramIdx": 0,
  "count": 10,
  "durationInSec": 1
}
```

## 规则持久化

所有规则都持久化到 Nacos 配置中心，确保重启后规则不丢失。

### Nacos 配置结构
```
命名空间: rag-system
分组: SENTINEL_GROUP
配置文件:
  - gateway-service-flow-rules
  - gateway-service-degrade-rules
  - document-service-flow-rules
  - document-service-degrade-rules
  - session-service-flow-rules
  - session-service-degrade-rules
  - auth-service-flow-rules
  - auth-service-degrade-rules
  - monitor-service-flow-rules
  - config-service-flow-rules
  - system-rules
```

## 监控指标

Sentinel 暴露以下监控指标：

- `sentinel_pass_qps`: 通过的 QPS
- `sentinel_block_qps`: 被限流的 QPS
- `sentinel_success_qps`: 成功的 QPS
- `sentinel_exception_qps`: 异常的 QPS
- `sentinel_rt`: 平均响应时间
- `sentinel_circuit_breaker_state`: 熔断器状态（0=关闭，1=打开，2=半开）

## 使用指南

### 1. 启动 Sentinel Dashboard

```bash
docker-compose up -d sentinel-dashboard
```

### 2. 访问控制台

打开浏览器访问 http://localhost:8858，使用用户名 `sentinel` 和密码 `sentinel` 登录。

### 3. 配置流控规则

1. 在左侧菜单选择对应的服务
2. 点击"流控规则"
3. 点击"新增流控规则"
4. 填写规则参数并保存

### 4. 配置熔断规则

1. 在左侧菜单选择对应的服务
2. 点击"熔断规则"
3. 点击"新增熔断规则"
4. 填写规则参数并保存

### 5. 验证规则持久化

规则配置后会自动持久化到 Nacos。可以在 Nacos 控制台查看：
- 访问 http://localhost:8848/nacos
- 进入"配置管理" -> "配置列表"
- 选择命名空间 `rag-system`
- 查看 `SENTINEL_GROUP` 组下的配置

## 推荐配置

### Gateway Service
- QPS 限流: 1000/s
- 并发线程数: 200
- 熔断阈值: 异常比例 > 50%

### Document Service
- QPS 限流: 500/s
- 并发线程数: 100
- 熔断阈值: 慢调用比例 > 50%（RT > 2s）

### Session Service
- QPS 限流: 800/s
- 并发线程数: 150
- 熔断阈值: 异常比例 > 40%

### RAG Query Service
- QPS 限流: 300/s
- 并发线程数: 50
- 熔断阈值: 慢调用比例 > 60%（RT > 5s）

## 故障排查

### 问题：规则不生效
- 检查服务是否正确连接到 Sentinel Dashboard
- 检查 Nacos 配置是否正确
- 查看服务日志中的 Sentinel 相关信息

### 问题：规则丢失
- 确认规则持久化配置正确
- 检查 Nacos 连接状态
- 查看 Sentinel Dashboard 日志

## 参考资料

- [Sentinel 官方文档](https://sentinelguard.io/zh-cn/docs/introduction.html)
- [Sentinel Dashboard 使用指南](https://github.com/alibaba/Sentinel/wiki/%E6%8E%A7%E5%88%B6%E5%8F%B0)
- [规则持久化](https://github.com/alibaba/Sentinel/wiki/%E5%8A%A8%E6%80%81%E8%A7%84%E5%88%99%E6%89%A9%E5%B1%95)
