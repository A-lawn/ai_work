"""数据模型定义"""
from pydantic import BaseModel, Field
from typing import List, Optional


class EmbeddingRequest(BaseModel):
    """嵌入向量请求"""
    texts: List[str] = Field(..., description="待向量化的文本列表", min_length=1)
    model: Optional[str] = Field(None, description="使用的模型名称（可选）")
    use_cache: bool = Field(True, description="是否使用缓存")
    
    class Config:
        json_schema_extra = {
            "example": {
                "texts": [
                    "如何重启 Nginx 服务？",
                    "MySQL 数据库连接失败的常见原因"
                ],
                "model": "text-embedding-ada-002",
                "use_cache": True
            }
        }


class EmbeddingResponse(BaseModel):
    """嵌入向量响应"""
    embeddings: List[List[float]] = Field(..., description="向量列表")
    model: str = Field(..., description="使用的模型名称")
    dimension: int = Field(..., description="向量维度")
    cached_count: int = Field(0, description="从缓存获取的数量")
    total_count: int = Field(..., description="总数量")
    
    class Config:
        json_schema_extra = {
            "example": {
                "embeddings": [[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]],
                "model": "text-embedding-ada-002",
                "dimension": 1536,
                "cached_count": 1,
                "total_count": 2
            }
        }


class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str = Field(..., description="服务状态")
    service_name: str = Field(..., description="服务名称")
    version: str = Field(..., description="版本号")
    model_provider: str = Field(..., description="模型提供商")
    model_name: str = Field(..., description="模型名称")
    redis_connected: bool = Field(..., description="Redis 连接状态")
    nacos_registered: bool = Field(..., description="Nacos 注册状态")


class ModelSwitchRequest(BaseModel):
    """模型切换请求"""
    use_local_model: bool = Field(..., description="是否使用本地模型")
    model_name: Optional[str] = Field(None, description="模型名称")
    
    class Config:
        json_schema_extra = {
            "example": {
                "use_local_model": False,
                "model_name": "text-embedding-ada-002"
            }
        }


class CacheClearResponse(BaseModel):
    """缓存清空响应"""
    success: bool = Field(..., description="是否成功")
    message: str = Field(..., description="消息")
