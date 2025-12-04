"""Embedding Service - 主应用入口"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.logging_config import setup_logging
from app.nacos_client import nacos_registry
from app.redis_client import redis_cache
from app.embedding_generator import embedding_service
from app.routes import router
from app.metrics import get_metrics, redis_connection_status, nacos_registration_status

# 配置日志
setup_logging()
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    # 启动时执行
    logger.info("=" * 60)
    logger.info("Embedding Service 启动中...")
    logger.info("=" * 60)
    
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
    
    # 初始化嵌入服务
    logger.info("初始化嵌入服务...")
    try:
        model_name = embedding_service.get_model_name()
        provider = embedding_service.get_provider()
        dimension = embedding_service.get_dimension()
        logger.info(f"✓ 嵌入服务初始化成功")
        logger.info(f"  - 提供商: {provider}")
        logger.info(f"  - 模型: {model_name}")
        logger.info(f"  - 维度: {dimension}")
    except Exception as e:
        logger.error(f"✗ 嵌入服务初始化失败: {str(e)}")
        raise
    
    logger.info("=" * 60)
    logger.info(f"Embedding Service 已启动")
    logger.info(f"服务地址: http://{settings.service_host}:{settings.service_port}")
    logger.info(f"API 文档: http://{settings.service_host}:{settings.service_port}/docs")
    logger.info(f"健康检查: http://{settings.service_host}:{settings.service_port}/health")
    logger.info(f"指标端点: http://{settings.service_host}:{settings.service_port}/metrics")
    logger.info("=" * 60)
    
    yield
    
    # 关闭时执行
    logger.info("Embedding Service 关闭中...")
    
    # 从 Nacos 注销
    nacos_registry.deregister()
    nacos_registration_status.set(0)
    
    logger.info("Embedding Service 已关闭")


# 创建 FastAPI 应用
app = FastAPI(
    title="Embedding Service",
    description="嵌入模型服务 - 提供文本向量化功能",
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
        "model_provider": embedding_service.get_provider(),
        "model_name": embedding_service.get_model_name(),
        "dimension": embedding_service.get_dimension()
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.service_host,
        port=settings.service_port,
        reload=False
    )
