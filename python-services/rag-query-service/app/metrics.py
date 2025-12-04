"""Prometheus 指标模块"""
from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST


# 查询相关指标
query_total = Counter(
    "rag_query_total",
    "RAG 查询总数",
    ["status"]
)

query_duration = Histogram(
    "rag_query_duration_seconds",
    "RAG 查询耗时（秒）",
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0]
)

retrieval_duration = Histogram(
    "rag_retrieval_duration_seconds",
    "向量检索耗时（秒）",
    buckets=[0.01, 0.05, 0.1, 0.5, 1.0]
)

llm_generation_duration = Histogram(
    "rag_llm_generation_duration_seconds",
    "LLM 生成耗时（秒）",
    buckets=[0.5, 1.0, 2.0, 5.0, 10.0, 30.0]
)

# 检索结果指标
retrieval_results_count = Histogram(
    "rag_retrieval_results_count",
    "检索返回的结果数量",
    buckets=[0, 1, 3, 5, 10, 20]
)

# 缓存指标
cache_hit_total = Counter(
    "rag_cache_hit_total",
    "缓存命中次数"
)

cache_miss_total = Counter(
    "rag_cache_miss_total",
    "缓存未命中次数"
)

# 服务状态指标
chroma_connection_status = Gauge(
    "rag_chroma_connection_status",
    "ChromaDB 连接状态 (1=已连接, 0=未连接)"
)

redis_connection_status = Gauge(
    "rag_redis_connection_status",
    "Redis 连接状态 (1=已连接, 0=未连接)"
)

nacos_registration_status = Gauge(
    "rag_nacos_registration_status",
    "Nacos 注册状态 (1=已注册, 0=未注册)"
)

# 错误指标
error_total = Counter(
    "rag_error_total",
    "错误总数",
    ["error_type"]
)


def get_metrics():
    """获取 Prometheus 指标"""
    return generate_latest(), {"Content-Type": CONTENT_TYPE_LATEST}
