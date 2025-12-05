"""
智谱 AI (GLM) LLM Adapter
智谱 AI 大模型服务
"""
from typing import AsyncIterator, Optional
import httpx
import time
import jwt
from app.adapters.base import LLMAdapter
from app.models import GenerateRequest, GenerateResponse, TokenUsage
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class ZhipuAdapter(LLMAdapter):
    """Adapter for Zhipu AI (智谱AI) GLM API"""
    
    def __init__(
        self,
        model: str = None,
        api_key: str = None,
        timeout: int = None
    ):
        """
        Initialize Zhipu adapter
        
        Args:
            model: Model name (e.g., glm-4, glm-4-flash, glm-3-turbo)
            api_key: Zhipu API key
            timeout: Request timeout in seconds
        """
        super().__init__(model or "glm-4")
        
        self.api_key = api_key or settings.ZHIPU_API_KEY
        self.timeout = timeout or 60
        self.api_base = "https://open.bigmodel.cn/api/paas/v4/chat/completions"
        
        if not self.api_key:
            raise ValueError("Zhipu API key is required")
        
        logger.info(f"Zhipu adapter initialized with model: {self.model}")
    
    def _generate_token(self) -> str:
        """Generate JWT token for authentication"""
        try:
            api_key, secret = self.api_key.split(".")
            payload = {
                "api_key": api_key,
                "exp": int(time.time()) + 3600,  # 1 hour expiration
                "timestamp": int(time.time())
            }
            return jwt.encode(payload, secret, algorithm="HS256")
        except Exception as e:
            logger.error(f"Error generating Zhipu token: {e}")
            raise ValueError("Invalid Zhipu API key format")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Zhipu API
        
        Args:
            request: Generation request
            
        Returns:
            Generated response
        """
        self.validate_request(request)
        
        try:
            # Generate authentication token
            token = self._generate_token()
            
            # Prepare request payload
            payload = {
                "model": self.model,
                "messages": [
                    {"role": "user", "content": request.prompt}
                ],
                "max_tokens": request.max_tokens or 2000,
                "temperature": request.temperature if request.temperature is not None else 0.7,
            }
            
            if request.top_p is not None:
                payload["top_p"] = request.top_p
            
            # Call Zhipu API
            logger.info(f"Calling Zhipu API with model: {self.model}")
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    self.api_base,
                    json=payload,
                    headers={
                        "Authorization": f"Bearer {token}",
                        "Content-Type": "application/json"
                    }
                )
                response.raise_for_status()
                result = response.json()
            
            # Extract response
            if "error" in result:
                raise Exception(f"Zhipu API error: {result['error'].get('message')}")
            
            choice = result["choices"][0]
            text = choice["message"]["content"]
            finish_reason = choice["finish_reason"]
            
            # Extract token usage
            usage_data = result.get("usage", {})
            usage = TokenUsage(
                prompt_tokens=usage_data.get("prompt_tokens", 0),
                completion_tokens=usage_data.get("completion_tokens", 0),
                total_tokens=usage_data.get("total_tokens", 0)
            )
            
            logger.info(
                f"Zhipu generation completed. Tokens used: {usage.total_tokens}"
            )
            
            return GenerateResponse(
                text=text,
                model=self.model,
                provider="zhipu",
                usage=usage,
                finish_reason=finish_reason
            )
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Zhipu API HTTP error: {e}")
            raise Exception(f"Zhipu API error: {str(e)}")
        except Exception as e:
            logger.error(f"Error in Zhipu generation: {e}")
            raise Exception(f"Generation failed: {str(e)}")
    
    async def generate_stream(self, request: GenerateRequest) -> AsyncIterator[str]:
        """
        Generate text with streaming
        
        Args:
            request: Generation request
            
        Yields:
            Text chunks
        """
        self.validate_request(request)
        
        try:
            # Generate authentication token
            token = self._generate_token()
            
            # Prepare request payload
            payload = {
                "model": self.model,
                "messages": [
                    {"role": "user", "content": request.prompt}
                ],
                "max_tokens": request.max_tokens or 2000,
                "temperature": request.temperature if request.temperature is not None else 0.7,
                "stream": True
            }
            
            if request.top_p is not None:
                payload["top_p"] = request.top_p
            
            # Call Zhipu API with streaming
            logger.info(f"Starting Zhipu streaming with model: {self.model}")
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                async with client.stream(
                    "POST",
                    self.api_base,
                    json=payload,
                    headers={
                        "Authorization": f"Bearer {token}",
                        "Content-Type": "application/json"
                    }
                ) as response:
                    response.raise_for_status()
                    async for line in response.aiter_lines():
                        if line.startswith("data:"):
                            import json
                            data_str = line[5:].strip()
                            if data_str and data_str != "[DONE]":
                                data = json.loads(data_str)
                                if "choices" in data and data["choices"]:
                                    delta = data["choices"][0].get("delta", {})
                                    content = delta.get("content", "")
                                    if content:
                                        yield content
            
            logger.info("Zhipu streaming completed")
            
        except Exception as e:
            logger.error(f"Error in Zhipu streaming: {e}")
            raise Exception(f"Streaming failed: {str(e)}")
    
    async def count_tokens(self, text: str) -> int:
        """
        Estimate token count (rough estimate)
        
        Args:
            text: Text to count tokens for
            
        Returns:
            Estimated number of tokens
        """
        # Rough estimate: Chinese ~1.5 chars/token, English ~4 chars/token
        chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
        other_chars = len(text) - chinese_chars
        return int(chinese_chars / 1.5 + other_chars / 4)
    
    async def health_check(self) -> bool:
        """
        Check if Zhipu API is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            token = self._generate_token()
            async with httpx.AsyncClient(timeout=10) as client:
                response = await client.get(
                    "https://open.bigmodel.cn/api/paas/v4/models",
                    headers={"Authorization": f"Bearer {token}"}
                )
                return response.status_code == 200
        except Exception as e:
            logger.error(f"Zhipu health check failed: {e}")
            return False
