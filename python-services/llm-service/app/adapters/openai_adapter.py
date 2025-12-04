"""
OpenAI LLM Adapter
"""
import asyncio
from typing import AsyncIterator, Optional
import openai
from openai import AsyncOpenAI
from app.adapters.base import LLMAdapter
from app.models import GenerateRequest, GenerateResponse, TokenUsage
from app.config import settings
from app.logging_config import get_logger
import tiktoken

logger = get_logger(__name__)


class OpenAIAdapter(LLMAdapter):
    """Adapter for OpenAI API"""
    
    def __init__(
        self,
        model: str = None,
        api_key: str = None,
        api_base: str = None,
        timeout: int = None,
        max_retries: int = None
    ):
        """
        Initialize OpenAI adapter
        
        Args:
            model: Model name (default from settings)
            api_key: OpenAI API key (default from settings)
            api_base: API base URL (default from settings)
            timeout: Request timeout in seconds
            max_retries: Maximum number of retries
        """
        super().__init__(model or settings.OPENAI_MODEL)
        
        self.api_key = api_key or settings.OPENAI_API_KEY
        self.api_base = api_base or settings.OPENAI_API_BASE
        self.timeout = timeout or settings.OPENAI_TIMEOUT
        self.max_retries = max_retries or settings.MAX_RETRIES
        
        if not self.api_key:
            raise ValueError("OpenAI API key is required")
        
        # Initialize async client
        self.client = AsyncOpenAI(
            api_key=self.api_key,
            base_url=self.api_base,
            timeout=self.timeout,
            max_retries=self.max_retries
        )
        
        # Initialize tokenizer
        try:
            self.tokenizer = tiktoken.encoding_for_model(self.model)
        except KeyError:
            logger.warning(f"No tokenizer found for model {self.model}, using cl100k_base")
            self.tokenizer = tiktoken.get_encoding("cl100k_base")
        
        logger.info(f"OpenAI adapter initialized with model: {self.model}")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using OpenAI API
        
        Args:
            request: Generation request
            
        Returns:
            Generated response
        """
        self.validate_request(request)
        
        try:
            # Prepare parameters
            params = {
                "model": self.model,
                "messages": [{"role": "user", "content": request.prompt}],
                "max_tokens": request.max_tokens or settings.MAX_RESPONSE_TOKENS,
                "temperature": request.temperature if request.temperature is not None else settings.OPENAI_TEMPERATURE,
            }
            
            # Add optional parameters
            if request.top_p is not None:
                params["top_p"] = request.top_p
            if request.stop:
                params["stop"] = request.stop
            if request.presence_penalty is not None:
                params["presence_penalty"] = request.presence_penalty
            if request.frequency_penalty is not None:
                params["frequency_penalty"] = request.frequency_penalty
            
            # Call OpenAI API
            logger.info(f"Calling OpenAI API with model: {self.model}")
            response = await self.client.chat.completions.create(**params)
            
            # Extract response
            choice = response.choices[0]
            text = choice.message.content
            finish_reason = choice.finish_reason
            
            # Extract token usage
            usage = TokenUsage(
                prompt_tokens=response.usage.prompt_tokens,
                completion_tokens=response.usage.completion_tokens,
                total_tokens=response.usage.total_tokens
            )
            
            logger.info(
                f"OpenAI generation completed. Tokens used: {usage.total_tokens} "
                f"(prompt: {usage.prompt_tokens}, completion: {usage.completion_tokens})"
            )
            
            return GenerateResponse(
                text=text,
                model=self.model,
                provider="openai",
                usage=usage,
                finish_reason=finish_reason
            )
            
        except openai.APIError as e:
            logger.error(f"OpenAI API error: {e}")
            raise Exception(f"OpenAI API error: {str(e)}")
        except openai.RateLimitError as e:
            logger.error(f"OpenAI rate limit exceeded: {e}")
            raise Exception(f"Rate limit exceeded: {str(e)}")
        except openai.APIConnectionError as e:
            logger.error(f"OpenAI connection error: {e}")
            raise Exception(f"Connection error: {str(e)}")
        except Exception as e:
            logger.error(f"Unexpected error in OpenAI generation: {e}")
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
            # Prepare parameters
            params = {
                "model": self.model,
                "messages": [{"role": "user", "content": request.prompt}],
                "max_tokens": request.max_tokens or settings.MAX_RESPONSE_TOKENS,
                "temperature": request.temperature if request.temperature is not None else settings.OPENAI_TEMPERATURE,
                "stream": True
            }
            
            # Add optional parameters
            if request.top_p is not None:
                params["top_p"] = request.top_p
            if request.stop:
                params["stop"] = request.stop
            if request.presence_penalty is not None:
                params["presence_penalty"] = request.presence_penalty
            if request.frequency_penalty is not None:
                params["frequency_penalty"] = request.frequency_penalty
            
            # Call OpenAI API with streaming
            logger.info(f"Starting OpenAI streaming with model: {self.model}")
            stream = await self.client.chat.completions.create(**params)
            
            async for chunk in stream:
                if chunk.choices and chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content
            
            logger.info("OpenAI streaming completed")
            
        except Exception as e:
            logger.error(f"Error in OpenAI streaming: {e}")
            raise Exception(f"Streaming failed: {str(e)}")
    
    async def count_tokens(self, text: str) -> int:
        """
        Count tokens using tiktoken
        
        Args:
            text: Text to count tokens for
            
        Returns:
            Number of tokens
        """
        try:
            tokens = self.tokenizer.encode(text)
            return len(tokens)
        except Exception as e:
            logger.error(f"Error counting tokens: {e}")
            # Fallback: rough estimate (1 token ≈ 4 characters)
            return len(text) // 4
    
    async def health_check(self) -> bool:
        """
        Check if OpenAI API is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            # Try to list models as a health check
            models = await self.client.models.list()
            return True
        except Exception as e:
            logger.error(f"OpenAI health check failed: {e}")
            return False
