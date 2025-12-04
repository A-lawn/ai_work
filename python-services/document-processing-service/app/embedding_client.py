"""Embedding Service 客户端"""
import logging
from typing import List
import httpx

from app.config import settings

logger = logging.getLogger(__name__)


class EmbeddingServiceClient:
    """Embedding Service 客户端"""
    
    def __init__(self):
        """初始化客户端"""
        self.base_url = settings.embedding_service_url
        self.timeout = 30.0
        
    async def generate_embeddings(self, texts: List[str]) -> List[List[float]]:
        """
        调用 Embedding Service 生成向量
        
        Args:
            texts: 文本列表
            
        Returns:
            向量列表
        """
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{self.base_url}/api/embeddings",
                    json={"texts": texts}
                )
                
                if response.status_code == 200:
                    result = response.json()
                    embeddings = result.get("embeddings", [])
                    logger.info(f"成功生成 {len(embeddings)} 个向量")
                    return embeddings
                else:
                    logger.error(
                        f"生成向量失败: status={response.status_code}, "
                        f"response={response.text}"
                    )
                    return []
                    
        except httpx.TimeoutException:
            logger.error("调用 Embedding Service 超时")
            return []
        except Exception as e:
            logger.error(f"调用 Embedding Service 失败: {str(e)}")
            return []
    
    async def health_check(self) -> bool:
        """
        检查 Embedding Service 健康状态
        
        Returns:
            是否健康
        """
        try:
            async with httpx.AsyncClient(timeout=5.0) as client:
                response = await client.get(f"{self.base_url}/health")
                return response.status_code == 200
        except:
            return False


# 全局 Embedding Service 客户端实例
embedding_client = EmbeddingServiceClient()
