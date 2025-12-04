"""Prometheus 指标收集"""
import logging
from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
from fastapi import Response

logger = logging.getLogger(__name__)

# 定义指标
embedding_requests_total = Counter(
    'embedding_requests_total',
    'Total number of embedding requests',
    ['model', 'status']
)

embedding_texts_total = Counter(
    'embedding_texts_total',
    'Total number of texts embedded',
    ['model']
)

embedding_cache_hits_total = Counter(
    'embedding_cache_hits_total',
    'Total number of cache hits',
    ['model']
)

embedding_cache_misses_total = Counter(
    'embedding_cache_misses_total',
    'Total number of cache misses',
    ['model']
)

embedding_duration_seconds = Histogram(
    'embedding_duration_seconds',
    'Time spent generating embeddings',
    ['model'],
    buckets=[0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 30.0]
)

embedding_batch_size = Histogram(
    'embedding_batch_size',
    'Size of embedding batches',
    ['model'],
    buckets=[1, 5, 10, 20, 50, 100, 200]
)

redis_connection_status = Gauge(
    'redis_connection_status',
    'Redis connection status (1=connected, 0=disconnected)'
)

nacos_registration_status = Gauge(
    'nacos_registration_status',
    'Nacos registration status (1=registered, 0=not registered)'
)


def get_metrics() -> Response:
    """获取 Prometheus 指标"""
    return Response(
        content=generate_latest(),
        media_type=CONTENT_TYPE_LATEST
    )
