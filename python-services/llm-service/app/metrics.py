"""
Prometheus metrics for LLM Service
"""
from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
from fastapi import Response
from app.logging_config import get_logger

logger = get_logger(__name__)

# Request metrics
llm_requests_total = Counter(
    'llm_requests_total',
    'Total number of LLM generation requests',
    ['provider', 'model', 'status']
)

llm_request_duration_seconds = Histogram(
    'llm_request_duration_seconds',
    'LLM request duration in seconds',
    ['provider', 'model'],
    buckets=(0.5, 1.0, 2.0, 5.0, 10.0, 30.0, 60.0)
)

# Token metrics
llm_tokens_used_total = Counter(
    'llm_tokens_used_total',
    'Total number of tokens used',
    ['provider', 'model', 'type']  # type: prompt, completion, total
)

# Error metrics
llm_errors_total = Counter(
    'llm_errors_total',
    'Total number of LLM errors',
    ['provider', 'model', 'error_type']
)

# Active requests
llm_active_requests = Gauge(
    'llm_active_requests',
    'Number of active LLM requests',
    ['provider', 'model']
)

# Streaming metrics
llm_streaming_requests_total = Counter(
    'llm_streaming_requests_total',
    'Total number of streaming requests',
    ['provider', 'model']
)

# Cache metrics (if caching is implemented)
llm_cache_hits_total = Counter(
    'llm_cache_hits_total',
    'Total number of cache hits',
    ['provider', 'model']
)

llm_cache_misses_total = Counter(
    'llm_cache_misses_total',
    'Total number of cache misses',
    ['provider', 'model']
)


def get_metrics() -> Response:
    """
    Get Prometheus metrics
    
    Returns:
        Response with metrics in Prometheus format
    """
    metrics_data = generate_latest()
    return Response(content=metrics_data, media_type=CONTENT_TYPE_LATEST)
