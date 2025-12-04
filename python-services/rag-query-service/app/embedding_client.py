"""Embedding Service 客户端"""
import logging
from typing import List, Optional
import httpx
from app.config import settings
from app.nacos_client import nacos_registry

logger = logging.getLogger(__name__)


class EmbeddingClient:
    """Embedding Service 客户端"""
    
    def __init__(self):
        self.timeout = settings.embedding_timeout
        
    def _get_service_url(self) -> Optional[str]:
        """获取 Embedding Service URL"""
        # 如果配置了直接 URL，使用配置的 URL
        if settings.embedding_service_url:
            return settings.embedding_service_url
        
        # 否则通过 Nacos 服务发现
        instance = nacos_registry.get_service_instance(
            settings.embedding_service_name
        )
        
        if instance:
            return instance["url"]
        
        logger.error("无法获取 Embedding Service 地址")
        return None
    
    async def generate_embedding(self, text: str) -> Optional[List[float]]:
        """生成单个文本的向量
        
        Args:
            text: 输入文本
            
        Returns:
            向量列表，失败返回 None
        """
        service_url = self._get_service_url()
        if not service_url:
            return None
        
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{service_url}/api/embeddings",
                    json={"texts": [text]}
                )
                response.raise_for_status()
                
                data = response.json()
                if data.get("embeddings") and len(data["embeddings"]) > 0:
                    logger.debug(f"成功生成向量，维度: {len(data['embeddings'][0])}")
                    return data["embeddings"][0]
                
                return None
                
        except httpx.TimeoutException:
            logger.error(f"调用 Embedding Service 超时")
            return None
        except httpx.HTTPStatusError as e:
            logger.error(f"调用 Embedding Service 失败: {e.response.status_code}")
            return None
        except Exception as e:
            logger.error(f"生成向量时发生错误: {str(e)}")
            return None
    
    async def generate_embeddings(self, texts: List[str]) -> Optional[List[List[float]]]:
        """批量生成文本向量
        
        Args:
            texts: 输入文本列表
            
        Returns:
            向量列表，失败返回 None
        """
        service_url = self._get_service_url()
        if not service_url:
            return None
        
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{service_url}/api/embeddings",
                    json={"texts": texts}
                )
                response.raise_for_status()
                
                data = response.json()
                if data.get("embeddings"):
                    logger.debug(f"成功生成 {len(data['embeddings'])} 个向量")
                    return data["embeddings"]
                
                return None
                
        except httpx.TimeoutException:
            logger.error(f"调用 Embedding Service 超时")
            return None
        except httpx.HTTPStatusError as e:
            logger.error(f"调用 Embedding Service 失败: {e.response.status_code}")
            return None
        except Exception as e:
            logger.error(f"批量生成向量时发生错误: {str(e)}")
            return None


# 全局 Embedding 客户端实例
embedding_client = EmbeddingClient()
