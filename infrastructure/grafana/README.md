# Grafana 仪表盘配置指南

## 概述

Grafana 是系统的可视化监控平台，提供丰富的仪表盘展示 Prometheus 采集的指标数据。

## 访问 Grafana

- **URL**: http://localhost:3001
- **用户名**: admin
- **密码**: admin

首次登录后建议修改默认密码。

## 仪表盘列表

系统预配置了以下仪表盘：

### 1. 服务健康状态监控
**文件**: `service-health-dashboard.json`

**功能**:
- 服务在线状态实时监控
- 服务实例数量统计
- 各服务 QPS 趋势
- 服务响应时间 (P95) 监控
- 服务错误率监控

**适用场景**:
- 快速了解系统整体健康状况
- 发现服务下线或异常
- 监控服务负载情况

### 2. QPS 和响应时间监控
**文件**: `qps-response-time-dashboard.json`

**功能**:
- 总体 QPS 统计
- 平均响应时间、P95、P99 响应时间
- 各服务 QPS 趋势对比
- 各服务响应时间分布 (P50, P95, P99)
- Top 10 高 QPS 端点
- Top 10 慢响应端点

**适用场景**:
- 性能分析和优化
- 识别性能瓶颈
- 容量规划

### 3. Sentinel 流控和熔断监控
**文件**: `sentinel-dashboard.json`

**功能**:
- Sentinel 通过/限流/异常 QPS 统计
- 限流拒绝率监控
- 各服务流控情况对比
- 熔断器状态实时监控
- Sentinel 平均响应时间
- 各资源限流统计
- Sentinel 成功率监控

**适用场景**:
- 监控流量控制效果
- 熔断降级状态跟踪
- 调整限流策略依据

### 4. 资源使用监控
**文件**: `resource-usage-dashboard.json`

**功能**:
- CPU 使用率监控
- JVM 内存使用和使用率
- GC 暂停时间和次数
- 线程数监控
- 数据库连接池状态
- Python 进程内存使用
- Python 进程打开文件数

**适用场景**:
- 资源使用分析
- 内存泄漏检测
- GC 性能优化
- 容量规划

### 5. 业务指标监控
**文件**: `business-metrics-dashboard.json`

**功能**:
- 文档处理速率和趋势
- 文档处理耗时分布
- RAG 查询速率和趋势
- RAG 查询耗时分布
- LLM 调用统计和耗时
- LLM Token 消耗趋势
- 向量生成统计
- 业务成功率汇总

**适用场景**:
- 业务运营监控
- 成本分析 (Token 消耗)
- 用户体验优化

## 仪表盘配置

### 自动加载

所有仪表盘配置文件位于 `dashboards/json/` 目录，Grafana 启动时会自动加载。

### 数据源配置

Prometheus 数据源已自动配置：
- **名称**: Prometheus
- **类型**: Prometheus
- **URL**: http://prometheus:9090
- **访问模式**: Proxy

配置文件: `datasources/prometheus.yml`

### 刷新间隔

所有仪表盘默认刷新间隔为 30 秒，可在仪表盘右上角调整。

## 使用指南

### 1. 访问仪表盘

1. 打开浏览器访问 http://localhost:3001
2. 使用 admin/admin 登录
3. 点击左侧菜单 "Dashboards" -> "Browse"
4. 选择要查看的仪表盘

### 2. 时间范围选择

在仪表盘右上角可以选择时间范围：
- Last 5 minutes
- Last 15 minutes
- Last 30 minutes
- Last 1 hour
- Last 3 hours
- Last 6 hours
- Last 12 hours
- Last 24 hours
- Custom range

### 3. 刷新控制

- 点击右上角刷新按钮手动刷新
- 设置自动刷新间隔 (5s, 10s, 30s, 1m, 5m, 15m, 30m, 1h)
- 暂停自动刷新

### 4. 面板交互

- **缩放**: 在图表上拖动选择区域进行缩放
- **查看详情**: 点击面板标题 -> "View"
- **编辑**: 点击面板标题 -> "Edit"
- **分享**: 点击面板标题 -> "Share"
- **导出数据**: 点击面板标题 -> "Inspect" -> "Data"

### 5. 变量过滤

部分仪表盘支持变量过滤，可在顶部选择：
- 服务名称
- 时间范围
- 环境

### 6. 告警配置

部分面板已配置告警规则：
- 高错误率告警
- 高 CPU 使用率告警
- 高内存使用率告警

告警触发时会在面板上显示红色标记。

## 自定义仪表盘

### 创建新仪表盘

1. 点击左侧菜单 "+" -> "Dashboard"
2. 点击 "Add new panel"
3. 选择可视化类型 (Graph, Stat, Table, etc.)
4. 配置查询 (使用 PromQL)
5. 调整面板设置
6. 保存仪表盘

### 导入仪表盘

1. 点击左侧菜单 "+" -> "Import"
2. 上传 JSON 文件或输入仪表盘 ID
3. 选择数据源
4. 点击 "Import"

### 导出仪表盘

1. 打开要导出的仪表盘
2. 点击右上角设置图标 -> "JSON Model"
3. 复制 JSON 内容或点击 "Save JSON to file"

## 常用 PromQL 查询

### 服务健康
```promql
# 服务在线状态
up

# 服务实例数
count(up == 1) by (job)
```

### QPS 和响应时间
```promql
# QPS
rate(http_server_requests_seconds_count[5m])

# P95 响应时间
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# 平均响应时间
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

### 错误率
```promql
# 5xx 错误率
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])
```

### 资源使用
```promql
# CPU 使用率
process_cpu_usage

# 内存使用率
jvm_memory_used_bytes / jvm_memory_max_bytes

# GC 时间
rate(jvm_gc_pause_seconds_sum[5m])
```

### Sentinel 指标
```promql
# 通过 QPS
rate(sentinel_pass_qps[5m])

# 限流 QPS
rate(sentinel_block_qps[5m])

# 限流拒绝率
rate(sentinel_block_qps[5m]) / (rate(sentinel_pass_qps[5m]) + rate(sentinel_block_qps[5m]))
```

### 业务指标
```promql
# 文档处理速率
rate(document_processing_total[5m])

# RAG 查询速率
rate(rag_query_total[5m])

# LLM Token 消耗
rate(llm_token_usage[5m])
```

## 告警集成

### 配置告警通知

1. 点击左侧菜单 "Alerting" -> "Notification channels"
2. 点击 "Add channel"
3. 选择通知类型 (Email, Slack, Webhook, etc.)
4. 配置通知参数
5. 测试并保存

### 配置告警规则

1. 编辑面板
2. 切换到 "Alert" 标签
3. 点击 "Create Alert"
4. 配置告警条件
5. 选择通知渠道
6. 保存

## 性能优化

### 查询优化
- 使用合适的时间范围
- 避免过于复杂的 PromQL 查询
- 使用 recording rules 预计算常用查询

### 面板优化
- 限制面板数量 (建议每个仪表盘不超过 20 个面板)
- 使用合适的刷新间隔
- 避免在单个面板中显示过多时间序列

### 数据源优化
- 配置合适的查询超时时间
- 使用查询缓存
- 限制返回的数据点数量

## 故障排查

### 问题：仪表盘无数据
- 检查 Prometheus 数据源配置
- 检查 Prometheus 是否正常采集数据
- 检查 PromQL 查询语法
- 检查时间范围选择

### 问题：仪表盘加载慢
- 减少面板数量
- 优化 PromQL 查询
- 增加刷新间隔
- 缩短时间范围

### 问题：告警不触发
- 检查告警规则配置
- 检查通知渠道配置
- 查看 Grafana 日志
- 测试通知渠道

## 最佳实践

### 仪表盘设计
- 按功能分类创建仪表盘
- 重要指标放在顶部
- 使用统一的配色方案
- 添加清晰的标题和说明

### 查询优化
- 使用标签过滤减少数据量
- 使用 rate() 而不是 irate() 获得更平滑的曲线
- 使用 sum() by (label) 进行聚合
- 避免使用 count() 统计大量时间序列

### 告警配置
- 设置合理的告警阈值
- 避免告警风暴
- 配置告警抑制规则
- 定期审查和调整告警规则

## 参考资料

- [Grafana 官方文档](https://grafana.com/docs/)
- [PromQL 查询语言](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana 仪表盘最佳实践](https://grafana.com/docs/grafana/latest/best-practices/)
- [Grafana 告警配置](https://grafana.com/docs/grafana/latest/alerting/)
