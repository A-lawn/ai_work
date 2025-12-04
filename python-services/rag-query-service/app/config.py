"""配置管理模块"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置"""
    
    # 服务配置
    service_name: str = "rag-query-service"
    service_port: int = 9002
    service_host: str = "0.0.0.0"
    
    # Nacos 配置
    nacos_server: str = "localhost:8848"
    nacos_namespace: str = "rag-system"
    nacos_group: str = "DEFAULT_GROUP"
    
    # ChromaDB 配置
    chroma_host: str = "localhost"
    chroma_port: int = 8000
    chroma_collection_name: str = "ops_knowledge_base"
    
    # Redis 配置
    redis_url: str = "redis://localhost:6379/0"
    redis_cache_ttl: int = 3600  # 查询结果缓存过期时间（秒）
    
    # 检索配置
    top_k: int = 5  # 返回的文档块数量
    similarity_threshold: float = 0.7  # 相似度阈值
    
    # Embedding Service 配置
    embedding_service_name: str = "embedding-service"
    embedding_service_url: Optional[str] = None  # 如果不使用服务发现，可以直接指定 URL
    
    # LLM Service 配置
    llm_service_name: str = "llm-service"
    llm_service_url: Optional[str] = None  # 如果不使用服务发现，可以直接指定 URL
    
    # 提示词配置
    prompt_template: str = """你是一个专业的运维助手。请基于以下上下文回答用户的问题。

上下文信息：
{context}

用户问题：{question}

请提供准确、详细的答案。如果上下文中没有相关信息，请明确说明。"""
    
    # Zipkin 配置
    zipkin_endpoint: str = "http://localhost:9411/api/v2/spans"
    
    # Prometheus 配置
    enable_metrics: bool = True
    
    # 超时配置
    embedding_timeout: int = 30
    llm_timeout: int = 60
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
