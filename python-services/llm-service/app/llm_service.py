"""
LLM Service - Core business logic
"""
import time
from typing import AsyncIterator
from app.adapters import LLMAdapter, OpenAIAdapter, AzureOpenAIAdapter, LocalModelAdapter
from app.adapters.qwen_adapter import QwenAdapter
from app.adapters.zhipu_adapter import ZhipuAdapter
from app.adapters.baichuan_adapter import BaichuanAdapter
from app.models import GenerateRequest, GenerateResponse, LLMProvider
from app.config import settings
from app.logging_config import get_logger
from app.metrics import (
    llm_requests_total,
    llm_request_duration_seconds,
    llm_tokens_used_total,
    llm_errors_total,
    llm_active_requests,
    llm_streaming_requests_total
)

logger = get_logger(__name__)


class LLMService:
    """Service for LLM operations"""
    
    def __init__(self):
        """Initialize LLM service"""
        self.adapter: LLMAdapter = self._create_adapter()
        logger.info(f"LLM Service initialized with provider: {settings.LLM_PROVIDER}")
    
    def _create_adapter(self) -> LLMAdapter:
        """
        Create LLM adapter based on configuration
        
        Returns:
            LLM adapter instance
            
        Raises:
            ValueError: If provider is not supported
        """
        provider = settings.LLM_PROVIDER.lower()
        
        if provider == LLMProvider.OPENAI:
            return OpenAIAdapter(
                model=settings.OPENAI_MODEL,
                api_key=settings.OPENAI_API_KEY,
                api_base=settings.OPENAI_API_BASE,
                timeout=settings.OPENAI_TIMEOUT,
                max_retries=settings.MAX_RETRIES
            )
        elif provider == LLMProvider.AZURE:
            return AzureOpenAIAdapter(
                deployment=settings.AZURE_OPENAI_DEPLOYMENT,
                api_key=settings.AZURE_OPENAI_API_KEY,
                endpoint=settings.AZURE_OPENAI_ENDPOINT,
                api_version=settings.AZURE_OPENAI_API_VERSION,
                timeout=settings.OPENAI_TIMEOUT,
                max_retries=settings.MAX_RETRIES
            )
        elif provider == LLMProvider.LOCAL:
            return LocalModelAdapter(
                model=settings.LOCAL_MODEL_NAME,
                endpoint=settings.LOCAL_MODEL_ENDPOINT,
                timeout=settings.OPENAI_TIMEOUT,
                max_retries=settings.MAX_RETRIES
            )
        elif provider == "qwen":
            return QwenAdapter(
                model=settings.QWEN_MODEL,
                api_key=settings.QWEN_API_KEY,
                timeout=settings.OPENAI_TIMEOUT
            )
        elif provider == "zhipu":
            return ZhipuAdapter(
                model=settings.ZHIPU_MODEL,
                api_key=settings.ZHIPU_API_KEY,
                timeout=settings.OPENAI_TIMEOUT
            )
        elif provider == "baichuan":
            return BaichuanAdapter(
                model=settings.BAICHUAN_MODEL,
                api_key=settings.BAICHUAN_API_KEY,
                timeout=settings.OPENAI_TIMEOUT
            )
        else:
            raise ValueError(f"Unsupported LLM provider: {provider}")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using LLM
        
        Args:
            request: Generation request
            
        Returns:
            Generated response
            
        Raises:
            Exception: If generation fails
        """
        provider = settings.LLM_PROVIDER
        model = self.adapter.get_model_name()
        
        # Update active requests metric
        llm_active_requests.labels(provider=provider, model=model).inc()
        
        start_time = time.time()
        
        try:
            # Generate response
            logger.info(f"Generating response with {provider}/{model}")
            response = await self.adapter.generate(request)
            
            # Record metrics
            duration = time.time() - start_time
            llm_request_duration_seconds.labels(
                provider=provider,
                model=model
            ).observe(duration)
            
            llm_requests_total.labels(
                provider=provider,
                model=model,
                status="success"
            ).inc()
            
            # Record token usage
            llm_tokens_used_total.labels(
                provider=provider,
                model=model,
                type="prompt"
            ).inc(response.usage.prompt_tokens)
            
            llm_tokens_used_total.labels(
                provider=provider,
                model=model,
                type="completion"
            ).inc(response.usage.completion_tokens)
            
            llm_tokens_used_total.labels(
                provider=provider,
                model=model,
                type="total"
            ).inc(response.usage.total_tokens)
            
            logger.info(
                f"Generation completed in {duration:.2f}s. "
                f"Tokens: {response.usage.total_tokens}"
            )
            
            return response
            
        except Exception as e:
            # Record error metrics
            duration = time.time() - start_time
            
            llm_requests_total.labels(
                provider=provider,
                model=model,
                status="error"
            ).inc()
            
            llm_errors_total.labels(
                provider=provider,
                model=model,
                error_type=type(e).__name__
            ).inc()
            
            logger.error(f"Generation failed after {duration:.2f}s: {e}")
            raise
            
        finally:
            # Decrement active requests
            llm_active_requests.labels(provider=provider, model=model).dec()
    
    async def generate_stream(self, request: GenerateRequest) -> AsyncIterator[str]:
        """
        Generate text with streaming
        
        Args:
            request: Generation request
            
        Yields:
            Text chunks
            
        Raises:
            Exception: If generation fails
        """
        provider = settings.LLM_PROVIDER
        model = self.adapter.get_model_name()
        
        # Update metrics
        llm_active_requests.labels(provider=provider, model=model).inc()
        llm_streaming_requests_total.labels(provider=provider, model=model).inc()
        
        start_time = time.time()
        
        try:
            logger.info(f"Starting streaming generation with {provider}/{model}")
            
            async for chunk in self.adapter.generate_stream(request):
                yield chunk
            
            # Record success metrics
            duration = time.time() - start_time
            llm_request_duration_seconds.labels(
                provider=provider,
                model=model
            ).observe(duration)
            
            llm_requests_total.labels(
                provider=provider,
                model=model,
                status="success"
            ).inc()
            
            logger.info(f"Streaming completed in {duration:.2f}s")
            
        except Exception as e:
            # Record error metrics
            duration = time.time() - start_time
            
            llm_requests_total.labels(
                provider=provider,
                model=model,
                status="error"
            ).inc()
            
            llm_errors_total.labels(
                provider=provider,
                model=model,
                error_type=type(e).__name__
            ).inc()
            
            logger.error(f"Streaming failed after {duration:.2f}s: {e}")
            raise
            
        finally:
            # Decrement active requests
            llm_active_requests.labels(provider=provider, model=model).dec()
    
    async def count_tokens(self, text: str) -> int:
        """
        Count tokens in text
        
        Args:
            text: Text to count tokens for
            
        Returns:
            Number of tokens
        """
        try:
            return await self.adapter.count_tokens(text)
        except Exception as e:
            logger.error(f"Error counting tokens: {e}")
            # Fallback to rough estimate
            return len(text) // 3
    
    async def health_check(self) -> bool:
        """
        Check if LLM provider is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            return await self.adapter.health_check()
        except Exception as e:
            logger.error(f"Health check failed: {e}")
            return False
    
    def get_provider_info(self) -> dict:
        """
        Get current provider information
        
        Returns:
            Provider information
        """
        return {
            "provider": settings.LLM_PROVIDER,
            "model": self.adapter.get_model_name(),
            "max_tokens": settings.MAX_RESPONSE_TOKENS,
            "temperature": settings.OPENAI_TEMPERATURE
        }


# Global LLM service instance
llm_service = LLMService()
