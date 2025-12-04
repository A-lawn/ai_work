"""API 路由"""
import logging
from fastapi import APIRouter, HTTPException
from fastapi.responses import StreamingResponse
from app.models import QueryRequest, QueryResponse, HealthResponse
from app.query_engine import query_engine
from app.chroma_client import chroma_client
from app.redis_client import redis_cache
from app.nacos_client import nacos_registry

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["RAG Query"])


@router.post("/query", response_model=QueryResponse)
async def query(request: QueryRequest):
    """RAG 查询接口（同步模式）
    
    Args:
        request: 查询请求
        
    Returns:
        查询响应
    """
    try:
        # 如果请求流式响应，返回错误
        if request.stream:
            raise HTTPException(
                status_code=400,
                detail="请使用流式查询接口进行流式查询"
            )
        
        # 执行查询
        result = await query_engine.query(
            question=request.question,
            session_history=request.session_history,
            top_k=request.top_k,
            similarity_threshold=request.similarity_threshold
        )
        
        return QueryResponse(**result)
        
    except Exception as e:
        logger.error(f"查询失败: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/query/stream")
async def query_stream(request: QueryRequest):
    """RAG 查询接口（流式模式）
    
    Args:
        request: 查询请求
        
    Returns:
        流式响应
    """
    try:
        # 创建流式生成器
        async def generate():
            async for chunk in query_engine.query_stream(
                question=request.question,
                session_history=request.session_history,
                top_k=request.top_k,
                similarity_threshold=request.similarity_threshold
            ):
                yield chunk
        
        return StreamingResponse(
            generate(),
            media_type="text/event-stream"
        )
        
    except Exception as e:
        logger.error(f"流式查询失败: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health", response_model=HealthResponse)
async def health():
    """健康检查接口
    
    Returns:
        健康状态
    """
    chroma_connected = chroma_client.collection is not None
    redis_connected = redis_cache.client is not None
    nacos_registered = nacos_registry.registered
    collection_count = chroma_client.get_collection_count()
    
    status = "healthy" if chroma_connected else "unhealthy"
    
    return HealthResponse(
        status=status,
        chroma_connected=chroma_connected,
        redis_connected=redis_connected,
        nacos_registered=nacos_registered,
        collection_count=collection_count
    )
