"""
Local Model LLM Adapter (OpenAI-compatible interface)
"""
from typing import AsyncIterator
import httpx
from app.adapters.base import LLMAdapter
from app.models import GenerateRequest, GenerateResponse, TokenUsage
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class LocalModelAdapter(LLMAdapter):
    """
    Adapter for local models with OpenAI-compatible API
    Works with vLLM, LocalAI, Ollama, etc.
    """
    
    def __init__(
        self,
        model: str = None,
        endpoint: str = None,
        timeout: int = None,
        max_retries: int = None
    ):
        """
        Initialize local model adapter
        
        Args:
            model: Model name
            endpoint: Local model endpoint URL
            timeout: Request timeout in seconds
            max_retries: Maximum number of retries
        """
        super().__init__(model or settings.LOCAL_MODEL_NAME)
        
        self.endpoint = endpoint or settings.LOCAL_MODEL_ENDPOINT
        self.timeout = timeout or settings.OPENAI_TIMEOUT
        self.max_retries = max_retries or settings.MAX_RETRIES
        
        if not self.endpoint:
            raise ValueError("Local model endpoint is required")
        
        # Ensure endpoint ends with /v1 for OpenAI compatibility
        if not self.endpoint.endswith("/v1"):
            self.endpoint = f"{self.endpoint}/v1"
        
        # Initialize HTTP client
        self.client = httpx.AsyncClient(
            base_url=self.endpoint,
            timeout=self.timeout
        )
        
        logger.info(f"Local model adapter initialized: {self.model} at {self.endpoint}")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using local model
        
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
                "messages": [{"role": "user", "content": request.prompt}],
                "max_tokens": request.max_tokens or settings.MAX_RESPONSE_TOKENS,
                "temperature": request.temperature if request.temperature is not None else settings.OPENAI_TEMPERATURE,
            }
            
            # Add optional parameters
            if request.top_p is not None:
                payload["top_p"] = request.top_p
            if request.stop:
                payload["stop"] = request.stop
            if request.presence_penalty is not None:
                payload["presence_penalty"] = request.presence_penalty
            if request.frequency_penalty is not None:
                payload["frequency_penalty"] = request.frequency_penalty
            
            # Call local model API
            logger.info(f"Calling local model: {self.model}")
            response = await self.client.post(
                "/chat/completions",
                json=payload
            )
            response.raise_for_status()
            
            # Parse response
            data = response.json()
            
            # Extract response text
            choice = data["choices"][0]
            text = choice["message"]["content"]
            finish_reason = choice.get("finish_reason")
            
            # Extract token usage (if available)
            usage_data = data.get("usage", {})
            usage = TokenUsage(
                prompt_tokens=usage_data.get("prompt_tokens", 0),
                completion_tokens=usage_data.get("completion_tokens", 0),
                total_tokens=usage_data.get("total_tokens", 0)
            )
            
            # If usage not provided, estimate
            if usage.total_tokens == 0:
                usage.prompt_tokens = await self.count_tokens(request.prompt)
                usage.completion_tokens = await self.count_tokens(text)
                usage.total_tokens = usage.prompt_tokens + usage.completion_tokens
            
            logger.info(
                f"Local model generation completed. Tokens used: {usage.total_tokens} "
                f"(prompt: {usage.prompt_tokens}, completion: {usage.completion_tokens})"
            )
            
            return GenerateResponse(
                text=text,
                model=self.model,
                provider="local",
                usage=usage,
                finish_reason=finish_reason
            )
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Local model HTTP error: {e}")
            raise Exception(f"Local model HTTP error: {str(e)}")
        except httpx.ConnectError as e:
            logger.error(f"Cannot connect to local model: {e}")
            raise Exception(f"Connection error: {str(e)}")
        except Exception as e:
            logger.error(f"Unexpected error in local model generation: {e}")
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
                "messages": [{"role": "user", "content": request.prompt}],
                "max_tokens": request.max_tokens or settings.MAX_RESPONSE_TOKENS,
                "temperature": request.temperature if request.temperature is not None else settings.OPENAI_TEMPERATURE,
                "stream": True
            }
            
            # Add optional parameters
            if request.top_p is not None:
                payload["top_p"] = request.top_p
            if request.stop:
                payload["stop"] = request.stop
            
            # Call local model API with streaming
            logger.info(f"Starting local model streaming: {self.model}")
            
            async with self.client.stream(
                "POST",
                "/chat/completions",
                json=payload
            ) as response:
                response.raise_for_status()
                
                async for line in response.aiter_lines():
                    if line.startswith("data: "):
                        data_str = line[6:]  # Remove "data: " prefix
                        
                        if data_str.strip() == "[DONE]":
                            break
                        
                        try:
                            import json
                            data = json.loads(data_str)
                            
                            if "choices" in data and len(data["choices"]) > 0:
                                delta = data["choices"][0].get("delta", {})
                                content = delta.get("content")
                                
                                if content:
                                    yield content
                        except json.JSONDecodeError:
                            continue
            
            logger.info("Local model streaming completed")
            
        except Exception as e:
            logger.error(f"Error in local model streaming: {e}")
            raise Exception(f"Streaming failed: {str(e)}")
    
    async def count_tokens(self, text: str) -> int:
        """
        Count tokens (rough estimate for local models)
        
        Args:
            text: Text to count tokens for
            
        Returns:
            Estimated number of tokens
        """
        # Rough estimate: 1 token ≈ 4 characters for English
        # For Chinese: 1 token ≈ 1.5 characters
        # Use a conservative estimate
        return len(text) // 3
    
    async def health_check(self) -> bool:
        """
        Check if local model is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            # Try to get models list
            response = await self.client.get("/models")
            response.raise_for_status()
            return True
        except Exception as e:
            logger.error(f"Local model health check failed: {e}")
            return False
    
    async def close(self):
        """Close the HTTP client"""
        await self.client.aclose()
