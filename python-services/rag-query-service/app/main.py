"""RAG Query Service - 主应用入口"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.logging_config import setup_logging
from app.nacos_client import nacos_registry
from app.chroma_client import chroma_client
from app.redis_client import redis_cache
from app.routes import router
from app.metrics import (
    get_metrics,
    chroma_connection_status,
    redis_connection_status,
    nacos_registration_status
)

# 配置日志
setup_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时执行
    logger.info("=" * 60)
    logger.info("RAG Query Service 启动中...")
    logger.info("=" * 60)
    
    # 连接 ChromaDB
    logger.info("连接 ChromaDB...")
    if chroma_client.connect():
        chroma_connection_status.set(1)
        count = chroma_client.get_collection_count()
        logger.info(f"✓ ChromaDB 连接成功，集合中有 {count} 个文档块")
    else:
        chroma_connection_status.set(0)
        logger.warning("✗ ChromaDB 连接失败，检索功能将不可用")
    
    # 连接 Redis
    logger.info("连接 Redis...")
    if redis_cache.connect():
        redis_connection_status.set(1)
        logger.info("✓ Redis 连接成功")
    else:
        redis_connection_status.set(0)
        logger.warning("✗ Redis 连接失败，缓存功能将不可用")
    
    # 注册到 Nacos
    logger.info("注册服务到 Nacos...")
    if nacos_registry.register():
        nacos_registration_status.set(1)
        logger.info("✓ Nacos 注册成功")
    else:
        nacos_registration_status.set(0)
        logger.warning("✗ Nacos 注册失败，服务发现功能将不可用")
    
    logger.info("=" * 60)
    logger.info(f"RAG Query Service 已启动")
    logger.info(f"服务地址: http://{settings.service_host}:{settings.service_port}")
    logger.info(f"API 文档: http://{settings.service_host}:{settings.service_port}/docs")
    logger.info(f"健康检查: http://{settings.service_host}:{settings.service_port}/health")
    logger.info(f"指标端点: http://{settings.service_host}:{settings.service_port}/metrics")
    logger.info("=" * 60)
    
    yield
    
    # 关闭时执行
    logger.info("RAG Query Service 关闭中...")
    
    # 从 Nacos 注销
    nacos_registry.deregister()
    nacos_registration_status.set(0)
    
    logger.info("RAG Query Service 已关闭")


# 创建 FastAPI 应用
app = FastAPI(
    title="RAG Query Service",
    description="RAG 查询服务 - 提供智能问答功能",
    version="1.0.0",
    lifespan=lifespan
)

# 配置 CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(router)


# Prometheus 指标端点
@app.get("/metrics")
async def metrics():
    """Prometheus 指标端点"""
    return get_metrics()


# 根路径
@app.get("/")
async def root():
    """根路径"""
    return {
        "service": settings.service_name,
        "version": "1.0.0",
        "status": "running",
        "collection": settings.chroma_collection_name
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.service_host,
        port=settings.service_port,
        reload=False
    )
