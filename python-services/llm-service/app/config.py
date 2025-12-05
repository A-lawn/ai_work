"""
Configuration settings for LLM Service
"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings"""
    
    # Service Configuration
    SERVICE_NAME: str = "llm-service"
    SERVICE_PORT: int = 9004
    SERVICE_HOST: str = "0.0.0.0"
    
    # Nacos Configuration
    NACOS_SERVER: str = "localhost:8848"
    NACOS_NAMESPACE: str = "rag-system"
    NACOS_GROUP: str = "DEFAULT_GROUP"
    
    # OpenAI Configuration
    OPENAI_API_KEY: Optional[str] = None
    OPENAI_API_BASE: str = "https://api.openai.com/v1"
    OPENAI_MODEL: str = "gpt-4"
    OPENAI_TEMPERATURE: float = 0.7
    OPENAI_MAX_TOKENS: int = 1000
    OPENAI_TIMEOUT: int = 60
    
    # Azure OpenAI Configuration
    AZURE_OPENAI_API_KEY: Optional[str] = None
    AZURE_OPENAI_ENDPOINT: Optional[str] = None
    AZURE_OPENAI_DEPLOYMENT: str = "gpt-4"
    AZURE_OPENAI_API_VERSION: str = "2023-12-01-preview"
    
    # Local Model Configuration
    LOCAL_MODEL_ENDPOINT: Optional[str] = None
    LOCAL_MODEL_NAME: str = "local-model"
    
    # 通义千问 (Qwen) Configuration
    QWEN_API_KEY: Optional[str] = None
    QWEN_MODEL: str = "qwen-turbo"  # qwen-turbo, qwen-plus, qwen-max
    
    # 智谱 AI (GLM) Configuration
    ZHIPU_API_KEY: Optional[str] = None
    ZHIPU_MODEL: str = "glm-4"  # glm-4, glm-4-flash, glm-3-turbo
    
    # 百川智能 (Baichuan) Configuration
    BAICHUAN_API_KEY: Optional[str] = None
    BAICHUAN_MODEL: str = "Baichuan2-Turbo"  # Baichuan2-Turbo, Baichuan2-Turbo-192k
    
    # 文心一言 (ERNIE) Configuration
    ERNIE_API_KEY: Optional[str] = None
    ERNIE_SECRET_KEY: Optional[str] = None
    ERNIE_MODEL: str = "ernie-bot-turbo"  # ernie-bot-turbo, ernie-bot-4
    
    # 讯飞星火 (Spark) Configuration
    SPARK_APP_ID: Optional[str] = None
    SPARK_API_KEY: Optional[str] = None
    SPARK_API_SECRET: Optional[str] = None
    SPARK_MODEL: str = "spark-3.5"  # spark-3.5, spark-pro
    
    # MiniMax Configuration
    MINIMAX_API_KEY: Optional[str] = None
    MINIMAX_GROUP_ID: Optional[str] = None
    MINIMAX_MODEL: str = "abab5.5-chat"  # abab5.5-chat, abab6-chat
    
    # LLM Provider Selection
    # Supported: openai, azure, local, qwen, zhipu, baichuan, ernie, spark, minimax
    LLM_PROVIDER: str = "openai"
    
    # Token Configuration
    MAX_CONTEXT_TOKENS: int = 4000
    MAX_RESPONSE_TOKENS: int = 1000
    
    # Retry Configuration
    MAX_RETRIES: int = 3
    RETRY_DELAY: float = 1.0
    
    # Zipkin Configuration
    ZIPKIN_ENDPOINT: str = "http://localhost:9411/api/v2/spans"
    ENABLE_TRACING: bool = True
    
    # Prometheus Configuration
    ENABLE_METRICS: bool = True
    
    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
