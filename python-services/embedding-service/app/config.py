"""配置管理模块"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置"""
    
    # 服务配置
    service_name: str = "embedding-service"
    service_port: int = 9003
    service_host: str = "0.0.0.0"
    
    # Nacos 配置
    nacos_server: str = "localhost:8848"
    nacos_namespace: str = "rag-system"
    nacos_group: str = "DEFAULT_GROUP"
    
    # Redis 配置
    redis_url: str = "redis://localhost:6379/0"
    redis_cache_ttl: int = 3600  # 缓存过期时间（秒）
    
    # OpenAI 配置
    openai_api_key: Optional[str] = None
    openai_api_base: str = "https://api.openai.com/v1"
    openai_embedding_model: str = "text-embedding-ada-002"
    openai_timeout: int = 30
    
    # 本地模型配置（BGE）
    use_local_model: bool = False
    local_model_name: str = "BAAI/bge-base-zh-v1.5"
    local_model_device: str = "cpu"  # cpu 或 cuda
    
    # 嵌入配置
    embedding_dimension: int = 1536  # OpenAI ada-002 维度
    batch_size: int = 32  # 批量处理大小
    max_text_length: int = 8191  # OpenAI 最大 token 数
    
    # Zipkin 配置
    zipkin_endpoint: str = "http://localhost:9411/api/v2/spans"
    
    # Prometheus 配置
    enable_metrics: bool = True
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
