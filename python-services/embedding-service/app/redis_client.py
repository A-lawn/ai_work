"""Redis 缓存客户端"""
import logging
import json
import hashlib
from typing import Optional, List
import redis
from app.config import settings

logger = logging.getLogger(__name__)


class RedisCache:
    """Redis 缓存管理器"""
    
    def __init__(self):
        self.client: Optional[redis.Redis] = None
        self.ttl = settings.redis_cache_ttl
        
    def connect(self):
        """连接到 Redis"""
        try:
            self.client = redis.from_url(
                settings.redis_url,
                decode_responses=False  # 保持二进制数据
            )
            # 测试连接
            self.client.ping()
            logger.info(f"成功连接到 Redis: {settings.redis_url}")
            return True
        except Exception as e:
            logger.error(f"连接 Redis 失败: {str(e)}")
            return False
    
    def _generate_cache_key(self, text: str, model: str) -> str:
        """生成缓存键"""
        # 使用文本和模型名称的哈希作为键
        content = f"{model}:{text}"
        return f"embedding:{hashlib.md5(content.encode()).hexdigest()}"
    
    def get_embedding(self, text: str, model: str) -> Optional[List[float]]:
        """从缓存获取向量"""
        if not self.client:
            return None
        
        try:
            key = self._generate_cache_key(text, model)
            cached_data = self.client.get(key)
            
            if cached_data:
                logger.debug(f"缓存命中: {key[:20]}...")
                return json.loads(cached_data)
            
            return None
        except Exception as e:
            logger.error(f"从缓存读取向量失败: {str(e)}")
            return None
    
    def set_embedding(self, text: str, model: str, embedding: List[float]) -> bool:
        """将向量存入缓存"""
        if not self.client:
            return False
        
        try:
            key = self._generate_cache_key(text, model)
            cached_data = json.dumps(embedding)
            self.client.setex(key, self.ttl, cached_data)
            logger.debug(f"向量已缓存: {key[:20]}...")
            return True
        except Exception as e:
            logger.error(f"缓存向量失败: {str(e)}")
            return False
    
    def get_batch_embeddings(
        self, 
        texts: List[str], 
        model: str
    ) -> List[Optional[List[float]]]:
        """批量从缓存获取向量"""
        if not self.client:
            return [None] * len(texts)
        
        try:
            keys = [self._generate_cache_key(text, model) for text in texts]
            cached_values = self.client.mget(keys)
            
            results = []
            for cached_data in cached_values:
                if cached_data:
                    results.append(json.loads(cached_data))
                else:
                    results.append(None)
            
            hit_count = sum(1 for r in results if r is not None)
            logger.debug(f"批量缓存命中: {hit_count}/{len(texts)}")
            
            return results
        except Exception as e:
            logger.error(f"批量读取缓存失败: {str(e)}")
            return [None] * len(texts)
    
    def set_batch_embeddings(
        self, 
        texts: List[str], 
        model: str, 
        embeddings: List[List[float]]
    ) -> bool:
        """批量将向量存入缓存"""
        if not self.client:
            return False
        
        try:
            pipeline = self.client.pipeline()
            for text, embedding in zip(texts, embeddings):
                key = self._generate_cache_key(text, model)
                cached_data = json.dumps(embedding)
                pipeline.setex(key, self.ttl, cached_data)
            
            pipeline.execute()
            logger.debug(f"批量缓存成功: {len(texts)} 个向量")
            return True
        except Exception as e:
            logger.error(f"批量缓存失败: {str(e)}")
            return False
    
    def clear_cache(self) -> bool:
        """清空所有嵌入向量缓存"""
        if not self.client:
            return False
        
        try:
            # 查找所有嵌入向量的键
            keys = self.client.keys("embedding:*")
            if keys:
                self.client.delete(*keys)
                logger.info(f"已清空 {len(keys)} 个缓存向量")
            return True
        except Exception as e:
            logger.error(f"清空缓存失败: {str(e)}")
            return False


# 全局 Redis 客户端实例
redis_cache = RedisCache()
