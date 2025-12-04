"""配置管理模块"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置"""
    
    # 服务配置
    service_name: str = "document-processing-service"
    service_port: int = 9001
    service_host: str = "0.0.0.0"
    
    # Nacos 配置
    nacos_server: str = "localhost:8848"
    nacos_namespace: str = "rag-system"
    nacos_group: str = "DEFAULT_GROUP"
    
    # ChromaDB 配置
    chroma_host: str = "localhost"
    chroma_port: int = 8000
    
    # RabbitMQ 配置
    rabbitmq_url: str = "amqp://admin:admin@localhost:5672"
    rabbitmq_queue: str = "document.processing"
    rabbitmq_exchange: str = "document.exchange"
    rabbitmq_routing_key: str = "document.process"
    
    # Redis 配置 (用于 Celery 和缓存)
    redis_url: str = "redis://localhost:6379/0"
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0
    
    # Embedding Service 配置
    embedding_service_url: str = "http://localhost:9003"
    
    # 文档处理配置
    chunk_size: int = 512
    chunk_overlap: int = 50
    supported_formats: list = ["pdf", "docx", "txt", "md"]
    
    # Zipkin 配置
    zipkin_endpoint: str = "http://localhost:9411/api/v2/spans"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
