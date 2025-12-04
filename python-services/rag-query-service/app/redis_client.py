"""Redis 缓存客户端"""
import logging
import json
import hashlib
from typing import Optional
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
                decode_responses=True
            )
            # 测试连接
            self.client.ping()
            logger.info(f"成功连接到 Redis: {settings.redis_url}")
            return True
        except Exception as e:
            logger.error(f"连接 Redis 失败: {str(e)}")
            return False
    
    def _generate_cache_key(self, question: str) -> str:
        """生成缓存键"""
        # 使用问题的哈希作为键
        return f"query:{hashlib.md5(question.encode()).hexdigest()}"
    
    def get_query_result(self, question: str) -> Optional[dict]:
        """从缓存获取查询结果"""
        if not self.client:
            return None
        
        try:
            key = self._generate_cache_key(question)
            cached_data = self.client.get(key)
            
            if cached_data:
                logger.debug(f"缓存命中: {key[:20]}...")
                return json.loads(cached_data)
            
            return None
        except Exception as e:
            logger.error(f"从缓存读取查询结果失败: {str(e)}")
            return None
    
    def set_query_result(self, question: str, result: dict) -> bool:
        """将查询结果存入缓存"""
        if not self.client:
            return False
        
        try:
            key = self._generate_cache_key(question)
            cached_data = json.dumps(result, ensure_ascii=False)
            self.client.setex(key, self.ttl, cached_data)
            logger.debug(f"查询结果已缓存: {key[:20]}...")
            return True
        except Exception as e:
            logger.error(f"缓存查询结果失败: {str(e)}")
            return False
    
    def clear_cache(self) -> bool:
        """清空所有查询结果缓存"""
        if not self.client:
            return False
        
        try:
            # 查找所有查询结果的键
            keys = self.client.keys("query:*")
            if keys:
                self.client.delete(*keys)
                logger.info(f"已清空 {len(keys)} 个缓存查询结果")
            return True
        except Exception as e:
            logger.error(f"清空缓存失败: {str(e)}")
            return False


# 全局 Redis 客户端实例
redis_cache = RedisCache()
