"""Document Processing Service - 文档处理服务主入口"""
import logging
import os
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from prometheus_client import make_asgi_app

from app.config import settings
from app.logging_config import setup_logging
from app.nacos_client import nacos_registry

# 设置日志
logger = setup_logging()

# 确保日志目录存在
os.makedirs("logs", exist_ok=True)

# 全局 RabbitMQ 消费者
rabbitmq_consumer = None


def process_message_callback(message: dict) -> bool:
    """
    RabbitMQ 消息处理回调
    
    Args:
        message: 消息内容
        
    Returns:
        处理是否成功
    """
    from app.document_processor import document_processor
    
    try:
        document_id = message.get("document_id")
        file_path = message.get("file_path")
        file_type = message.get("file_type")
        filename = message.get("filename")
        metadata = message.get("metadata")
        
        if not document_id or not file_path or not file_type:
            logger.error(f"消息缺少必要字段: {message}")
            return False
        
        # 异步处理文档
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        result = loop.run_until_complete(
            document_processor.process_document(
                document_id=document_id,
                file_path=file_path,
                file_type=file_type,
                filename=filename,
                metadata=metadata
            )
        )
        loop.close()
        
        return result["success"]
        
    except Exception as e:
        logger.error(f"处理消息异常: {str(e)}")
        return False


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期管理"""
    global rabbitmq_consumer
    
    from app.chroma_client import chroma_client
    from app.rabbitmq_consumer import RabbitMQConsumer
    
    # 启动时执行
    logger.info("启动 Document Processing Service...")
    
    # 连接 ChromaDB
    try:
        chroma_client.connect()
        logger.info("ChromaDB 连接成功")
    except Exception as e:
        logger.warning(f"ChromaDB 连接失败: {str(e)}")
    
    # 注册到 Nacos
    try:
        nacos_registry.register()
        logger.info("Nacos 服务注册成功")
    except Exception as e:
        logger.warning(f"Nacos 服务注册失败: {str(e)}")
    
    # 启动 RabbitMQ 消费者
    try:
        rabbitmq_consumer = RabbitMQConsumer(callback=process_message_callback)
        rabbitmq_consumer.start_consuming()
        logger.info("RabbitMQ 消费者启动成功")
    except Exception as e:
        logger.warning(f"RabbitMQ 消费者启动失败: {str(e)}")
    
    yield
    
    # 关闭时执行
    logger.info("关闭 Document Processing Service...")
    
    # 停止 RabbitMQ 消费者
    if rabbitmq_consumer:
        try:
            rabbitmq_consumer.stop_consuming()
            logger.info("RabbitMQ 消费者停止成功")
        except Exception as e:
            logger.warning(f"RabbitMQ 消费者停止失败: {str(e)}")
    
    # 从 Nacos 注销
    try:
        nacos_registry.deregister()
        logger.info("Nacos 服务注销成功")
    except Exception as e:
        logger.warning(f"Nacos 服务注销失败: {str(e)}")


# 创建 FastAPI 应用
app = FastAPI(
    title="Document Processing Service",
    description="文档处理服务 - 负责文档解析、分块和向量化",
    version="1.0.0",
    lifespan=lifespan
)

# CORS 配置
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 添加 Prometheus 指标端点
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)

# 导入路由
from app.routes import router
app.include_router(router)


@app.get("/health")
async def health_check():
    """健康检查端点"""
    from app.chroma_client import chroma_client
    
    # 检查各个组件状态
    chroma_connected = chroma_client.client is not None
    
    return {
        "status": "healthy",
        "service": settings.service_name,
        "version": "1.0.0",
        "components": {
            "chroma": chroma_connected,
            "rabbitmq": rabbitmq_consumer is not None and rabbitmq_consumer.is_running if rabbitmq_consumer else False
        }
    }


@app.get("/")
async def root():
    """根路径"""
    return {
        "service": "Document Processing Service",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "metrics": "/metrics",
            "docs": "/docs",
            "process": "/api/process-document",
            "delete": "/api/vectors/{document_id}"
        }
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        app,
        host=settings.service_host,
        port=settings.service_port,
        log_level="info"
    )
