"""
百川智能 (Baichuan) LLM Adapter
百川智能大模型服务
"""
from typing import AsyncIterator, Optional
import httpx
from app.adapters.base import LLMAdapter
from app.models import GenerateRequest, GenerateResponse, TokenUsage
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class BaichuanAdapter(LLMAdapter):
    """Adapter for Baichuan (百川智能) API"""
    
    def __init__(
        self,
        model: str = None,
        api_key: str = None,
        timeout: int = None
    ):
        """
        Initialize Baichuan adapter
        
        Args:
            model: Model name (e.g., Baichuan2-Turbo, Baichuan2-Turbo-192k)
            api_key: Baichuan API key
            timeout: Request timeout in seconds
        """
        super().__init__(model or "Baichuan2-Turbo")
        
        self.api_key = api_key or settings.BAICHUAN_API_KEY
        self.timeout = timeout or 60
        self.api_base = "https://api.baichuan-ai.com/v1/chat/completions"
        
        if not self.api_key:
            raise ValueError("Baichuan API key is required")
        
        logger.info(f"Baichuan adapter initialized with model: {self.model}")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Baichuan API
        
        Args:
            request: Generation request
            
        Returns:
            Generated response
        """
        self.validate_request(request)
        
        try:
            # Prepare request payload (OpenAI-compatible format)
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
            
            # Call Baichuan API
            logger.info(f"Calling Baichuan API with model: {self.model}")
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
            if "error" in result:
                raise Exception(f"Baichuan API error: {result['error'].get('message')}")
            
            choice = result["choices"][0]
            text = choice["message"]["content"]
            finish_reason = choice.get("finish_reason", "stop")
            
            # Extract token usage
            usage_data = result.get("usage", {})
            usage = TokenUsage(
                prompt_tokens=usage_data.get("prompt_tokens", 0),
                completion_tokens=usage_data.get("completion_tokens", 0),
                total_tokens=usage_data.get("total_tokens", 0)
            )
            
            logger.info(
                f"Baichuan generation completed. Tokens used: {usage.total_tokens}"
            )
            
            return GenerateResponse(
                text=text,
                model=self.model,
                provider="baichuan",
                usage=usage,
                finish_reason=finish_reason
            )
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Baichuan API HTTP error: {e}")
            raise Exception(f"Baichuan API error: {str(e)}")
        except Exception as e:
            logger.error(f"Error in Baichuan generation: {e}")
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
                "messages": [
                    {"role": "user", "content": request.prompt}
                ],
                "max_tokens": request.max_tokens or 2000,
                "temperature": request.temperature if request.temperature is not None else 0.7,
                "stream": True
            }
            
            if request.top_p is not None:
                payload["top_p"] = request.top_p
            
            # Call Baichuan API with streaming
            logger.info(f"Starting Baichuan streaming with model: {self.model}")
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                async with client.stream(
                    "POST",
                    self.api_base,
                    json=payload,
                    headers={
                        "Authorization": f"Bearer {self.api_key}",
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
            
            logger.info("Baichuan streaming completed")
            
        except Exception as e:
            logger.error(f"Error in Baichuan streaming: {e}")
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
        Check if Baichuan API is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            async with httpx.AsyncClient(timeout=10) as client:
                response = await client.get(
                    "https://api.baichuan-ai.com",
                    headers={"Authorization": f"Bearer {self.api_key}"}
                )
                return response.status_code in [200, 401, 403]  # API is reachable
        except Exception as e:
            logger.error(f"Baichuan health check failed: {e}")
            return False
