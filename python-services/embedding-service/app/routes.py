"""API 路由定义"""
import logging
import time
from typing import List
from fastapi import APIRouter, HTTPException, status
from app.models import (
    EmbeddingRequest,
    EmbeddingResponse,
    HealthResponse,
    ModelSwitchRequest,
    CacheClearResponse
)
from app.embedding_generator import embedding_service
from app.redis_client import redis_cache
from app.nacos_client import nacos_registry
from app.config import settings
from app.metrics import (
    embedding_requests_total,
    embedding_texts_total,
    embedding_cache_hits_total,
    embedding_cache_misses_total,
    embedding_duration_seconds,
    embedding_batch_size
)

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post("/api/embeddings", response_model=EmbeddingResponse)
async def generate_embeddings(request: EmbeddingRequest):
    """
    生成嵌入向量
    
    批量将文本转换为向量表示，支持缓存加速
    """
    start_time = time.time()
    model_name = request.model or embedding_service.get_model_name()
    
    try:
        logger.info(f"收到嵌入请求: {len(request.texts)} 个文本")
        
        # 记录批量大小
        embedding_batch_size.labels(model=model_name).observe(len(request.texts))
        
        embeddings = []
        cached_count = 0
        texts_to_generate = []
        text_indices = []
        
        # 如果启用缓存，先尝试从缓存获取
        if request.use_cache and redis_cache.client:
            cached_embeddings = redis_cache.get_batch_embeddings(
                request.texts, 
                model_name
            )
            
            for i, (text, cached_emb) in enumerate(zip(request.texts, cached_embeddings)):
                if cached_emb is not None:
                    embeddings.append(cached_emb)
                    cached_count += 1
                    embedding_cache_hits_total.labels(model=model_name).inc()
                else:
                    embeddings.append(None)  # 占位符
                    texts_to_generate.append(text)
                    text_indices.append(i)
                    embedding_cache_misses_total.labels(model=model_name).inc()
        else:
            texts_to_generate = request.texts
            text_indices = list(range(len(request.texts)))
            embeddings = [None] * len(request.texts)
        
        # 生成未缓存的向量
        if texts_to_generate:
            logger.info(f"生成 {len(texts_to_generate)} 个新向量")
            new_embeddings = embedding_service.generate_embeddings(texts_to_generate)
            
            # 填充结果
            for idx, emb in zip(text_indices, new_embeddings):
                embeddings[idx] = emb
            
            # 缓存新生成的向量
            if request.use_cache and redis_cache.client:
                redis_cache.set_batch_embeddings(
                    texts_to_generate,
                    model_name,
                    new_embeddings
                )
        
        # 记录指标
        duration = time.time() - start_time
        embedding_duration_seconds.labels(model=model_name).observe(duration)
        embedding_requests_total.labels(model=model_name, status='success').inc()
        embedding_texts_total.labels(model=model_name).inc(len(request.texts))
        
        logger.info(
            f"嵌入生成完成: {len(embeddings)} 个向量, "
            f"缓存命中: {cached_count}, 耗时: {duration:.2f}s"
        )
        
        return EmbeddingResponse(
            embeddings=embeddings,
            model=model_name,
            dimension=embedding_service.get_dimension(),
            cached_count=cached_count,
            total_count=len(embeddings)
        )
        
    except Exception as e:
        logger.error(f"生成嵌入向量失败: {str(e)}")
        embedding_requests_total.labels(model=model_name, status='error').inc()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"生成嵌入向量失败: {str(e)}"
        )


@router.get("/health", response_model=HealthResponse)
async def health_check():
    """
    健康检查
    
    返回服务状态和配置信息
    """
    redis_connected = redis_cache.client is not None
    nacos_registered = nacos_registry.client is not None
    
    return HealthResponse(
        status="healthy",
        service_name=settings.service_name,
        version="1.0.0",
        model_provider=embedding_service.get_provider(),
        model_name=embedding_service.get_model_name(),
        redis_connected=redis_connected,
        nacos_registered=nacos_registered
    )


@router.post("/api/model/switch")
async def switch_model(request: ModelSwitchRequest):
    """
    切换嵌入模型
    
    支持在 OpenAI 和本地模型之间切换
    """
    try:
        logger.info(f"切换模型请求: {request}")
        
        embedding_service.switch_model(
            use_local=request.use_local_model,
            model_name=request.model_name
        )
        
        # 清空缓存（因为不同模型的向量不兼容）
        if redis_cache.client:
            redis_cache.clear_cache()
        
        return {
            "success": True,
            "message": f"已切换到模型: {embedding_service.get_model_name()}",
            "provider": embedding_service.get_provider(),
            "dimension": embedding_service.get_dimension()
        }
        
    except Exception as e:
        logger.error(f"切换模型失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"切换模型失败: {str(e)}"
        )


@router.post("/api/cache/clear", response_model=CacheClearResponse)
async def clear_cache():
    """
    清空向量缓存
    
    清空 Redis 中所有缓存的嵌入向量
    """
    try:
        if not redis_cache.client:
            return CacheClearResponse(
                success=False,
                message="Redis 未连接"
            )
        
        redis_cache.clear_cache()
        
        return CacheClearResponse(
            success=True,
            message="缓存已清空"
        )
        
    except Exception as e:
        logger.error(f"清空缓存失败: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"清空缓存失败: {str(e)}"
        )
