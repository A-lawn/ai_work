"""LLM Service 客户端"""
import logging
from typing import Optional, AsyncIterator
import httpx
from app.config import settings
from app.nacos_client import nacos_registry

logger = logging.getLogger(__name__)


class LLMClient:
    """LLM Service 客户端"""
    
    def __init__(self):
        self.timeout = settings.llm_timeout
        
    def _get_service_url(self) -> Optional[str]:
        """获取 LLM Service URL"""
        # 如果配置了直接 URL，使用配置的 URL
        if settings.llm_service_url:
            return settings.llm_service_url
        
        # 否则通过 Nacos 服务发现
        instance = nacos_registry.get_service_instance(
            settings.llm_service_name
        )
        
        if instance:
            return instance["url"]
        
        logger.error("无法获取 LLM Service 地址")
        return None
    
    async def generate(
        self,
        prompt: str,
        temperature: float = 0.7,
        max_tokens: int = 1000
    ) -> Optional[str]:
        """生成答案（同步模式）
        
        Args:
            prompt: 提示词
            temperature: 温度参数
            max_tokens: 最大 token 数
            
        Returns:
            生成的答案，失败返回 None
        """
        service_url = self._get_service_url()
        if not service_url:
            return None
        
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    f"{service_url}/api/generate",
                    json={
                        "prompt": prompt,
                        "temperature": temperature,
                        "max_tokens": max_tokens,
                        "stream": False
                    }
                )
                response.raise_for_status()
                
                data = response.json()
                if data.get("response"):
                    logger.debug(f"成功生成答案，长度: {len(data['response'])}")
                    return data["response"]
                
                return None
                
        except httpx.TimeoutException:
            logger.error(f"调用 LLM Service 超时")
            return None
        except httpx.HTTPStatusError as e:
            logger.error(f"调用 LLM Service 失败: {e.response.status_code}")
            return None
        except Exception as e:
            logger.error(f"生成答案时发生错误: {str(e)}")
            return None
    
    async def generate_stream(
        self,
        prompt: str,
        temperature: float = 0.7,
        max_tokens: int = 1000
    ) -> AsyncIterator[str]:
        """生成答案（流式模式）
        
        Args:
            prompt: 提示词
            temperature: 温度参数
            max_tokens: 最大 token 数
            
        Yields:
            生成的文本片段
        """
        service_url = self._get_service_url()
        if not service_url:
            yield "错误：无法连接到 LLM Service"
            return
        
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                async with client.stream(
                    "POST",
                    f"{service_url}/api/generate",
                    json={
                        "prompt": prompt,
                        "temperature": temperature,
                        "max_tokens": max_tokens,
                        "stream": True
                    }
                ) as response:
                    response.raise_for_status()
                    
                    async for chunk in response.aiter_text():
                        if chunk:
                            yield chunk
                            
        except httpx.TimeoutException:
            logger.error(f"调用 LLM Service 超时")
            yield "错误：LLM Service 响应超时"
        except httpx.HTTPStatusError as e:
            logger.error(f"调用 LLM Service 失败: {e.response.status_code}")
            yield f"错误：LLM Service 返回错误 {e.response.status_code}"
        except Exception as e:
            logger.error(f"流式生成答案时发生错误: {str(e)}")
            yield f"错误：{str(e)}"


# 全局 LLM 客户端实例
llm_client = LLMClient()
