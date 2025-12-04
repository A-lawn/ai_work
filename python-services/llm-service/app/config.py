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
    
    # LLM Provider Selection
    LLM_PROVIDER: str = "openai"  # "openai" | "azure" | "local"
    
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
