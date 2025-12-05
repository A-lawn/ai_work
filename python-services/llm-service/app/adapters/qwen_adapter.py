"""
通义千问 (Qwen/Tongyi) LLM Adapter
阿里云大模型服务
"""
from typing import AsyncIterator, Optional
import httpx
from app.adapters.base import LLMAdapter
from app.models import GenerateRequest, GenerateResponse, TokenUsage
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class QwenAdapter(LLMAdapter):
    """Adapter for Alibaba Qwen (通义千问) API"""
    
    def __init__(
        self,
        model: str = None,
        api_key: str = None,
        timeout: int = None
    ):
        """
        Initialize Qwen adapter
        
        Args:
            model: Model name (e.g., qwen-turbo, qwen-plus, qwen-max)
            api_key: Qwen API key (DashScope API key)
            timeout: Request timeout in seconds
        """
        super().__init__(model or "qwen-turbo")
        
        self.api_key = api_key or settings.QWEN_API_KEY
        self.timeout = timeout or 60
        self.api_base = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation"
        
        if not self.api_key:
            raise ValueError("Qwen API key is required")
        
        logger.info(f"Qwen adapter initialized with model: {self.model}")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Qwen API
        
        Args:
            request: Generation request
            
        Returns:
            Generated response
        """
        self.validate_request(request)
        
        try:
            # Prepare request payload
            payload = {
                "model": self.model,
                "input": {
                    "messages": [
                        {"role": "user", "content": request.prompt}
                    ]
                },
                "parameters": {
                    "max_tokens": request.max_tokens or 2000,
                    "temperature": request.temperature if request.temperature is not None else 0.7,
                    "result_format": "message"
                }
            }
            
            if request.top_p is not None:
                payload["parameters"]["top_p"] = request.top_p
            
            # Call Qwen API
            logger.info(f"Calling Qwen API with model: {self.model}")
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(
                    self.api_base,
                    json=payload,
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json"
                    }
                )
                response.raise_for_status()
                result = response.json()
            
            # Extract response
            if result.get("code"):
                raise Exception(f"Qwen API error: {result.get('message')}")
            
            output = result["output"]
            text = output["choices"][0]["message"]["content"]
            finish_reason = output["choices"][0]["finish_reason"]
            
            # Extract token usage
            usage_data = result.get("usage", {})
            usage = TokenUsage(
                prompt_tokens=usage_data.get("input_tokens", 0),
                completion_tokens=usage_data.get("output_tokens", 0),
                total_tokens=usage_data.get("total_tokens", 0)
            )
            
            logger.info(
                f"Qwen generation completed. Tokens used: {usage.total_tokens}"
            )
            
            return GenerateResponse(
                text=text,
                model=self.model,
                provider="qwen",
                usage=usage,
                finish_reason=finish_reason
            )
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Qwen API HTTP error: {e}")
            raise Exception(f"Qwen API error: {str(e)}")
        except Exception as e:
            logger.error(f"Error in Qwen generation: {e}")
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
            # Prepare request payload
            payload = {
                "model": self.model,
                "input": {
                    "messages": [
                        {"role": "user", "content": request.prompt}
                    ]
                },
                "parameters": {
                    "max_tokens": request.max_tokens or 2000,
                    "temperature": request.temperature if request.temperature is not None else 0.7,
                    "result_format": "message",
                    "incremental_output": True
                }
            }
            
            if request.top_p is not None:
                payload["parameters"]["top_p"] = request.top_p
            
            # Call Qwen API with streaming
            logger.info(f"Starting Qwen streaming with model: {self.model}")
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                async with client.stream(
                    "POST",
                    self.api_base,
                    json=payload,
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
                        "Content-Type": "application/json",
                        "X-DashScope-SSE": "enable"
                    }
                ) as response:
                    response.raise_for_status()
                    async for line in response.aiter_lines():
                        if line.startswith("data:"):
                            import json
                            data = json.loads(line[5:].strip())
                            if "output" in data:
                                content = data["output"]["choices"][0]["message"]["content"]
                                if content:
                                    yield content
            
            logger.info("Qwen streaming completed")
            
        except Exception as e:
            logger.error(f"Error in Qwen streaming: {e}")
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
        Check if Qwen API is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                response = await client.get(
                    "https://dashscope.aliyuncs.com",
                    headers={"Authorization": f"Bearer {self.api_key}"}
                )
                return response.status_code in [200, 401, 403]  # API is reachable
        except Exception as e:
            logger.error(f"Qwen health check failed: {e}")
            return False
