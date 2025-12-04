"""
Azure OpenAI LLM Adapter
"""
from typing import AsyncIterator
from openai import AsyncAzureOpenAI
import openai
from app.adapters.base import LLMAdapter
from app.models import GenerateRequest, GenerateResponse, TokenUsage
from app.config import settings
from app.logging_config import get_logger
import tiktoken

logger = get_logger(__name__)


class AzureOpenAIAdapter(LLMAdapter):
    """Adapter for Azure OpenAI Service"""
    
    def __init__(
        self,
        deployment: str = None,
        api_key: str = None,
        endpoint: str = None,
        api_version: str = None,
        timeout: int = None,
        max_retries: int = None
    ):
        """
        Initialize Azure OpenAI adapter
        
        Args:
            deployment: Azure deployment name
            api_key: Azure API key
            endpoint: Azure endpoint URL
            api_version: API version
            timeout: Request timeout in seconds
            max_retries: Maximum number of retries
        """
        super().__init__(deployment or settings.AZURE_OPENAI_DEPLOYMENT)
        
        self.api_key = api_key or settings.AZURE_OPENAI_API_KEY
        self.endpoint = endpoint or settings.AZURE_OPENAI_ENDPOINT
        self.api_version = api_version or settings.AZURE_OPENAI_API_VERSION
        self.timeout = timeout or settings.OPENAI_TIMEOUT
        self.max_retries = max_retries or settings.MAX_RETRIES
        
        if not self.api_key or not self.endpoint:
            raise ValueError("Azure OpenAI API key and endpoint are required")
        
        # Initialize async client
        self.client = AsyncAzureOpenAI(
            api_key=self.api_key,
            azure_endpoint=self.endpoint,
            api_version=self.api_version,
            timeout=self.timeout,
            max_retries=self.max_retries
        )
        
        # Initialize tokenizer
        try:
            self.tokenizer = tiktoken.encoding_for_model("gpt-4")
        except KeyError:
            logger.warning("Using cl100k_base tokenizer for Azure")
            self.tokenizer = tiktoken.get_encoding("cl100k_base")
        
        logger.info(f"Azure OpenAI adapter initialized with deployment: {self.model}")
    
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text using Azure OpenAI
        
        Args:
            request: Generation request
            
        Returns:
            Generated response
        """
        self.validate_request(request)
        
        try:
            # Prepare parameters
            params = {
                "model": self.model,  # This is the deployment name in Azure
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
            
            # Call Azure OpenAI API
            logger.info(f"Calling Azure OpenAI with deployment: {self.model}")
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
                f"Azure OpenAI generation completed. Tokens used: {usage.total_tokens} "
                f"(prompt: {usage.prompt_tokens}, completion: {usage.completion_tokens})"
            )
            
            return GenerateResponse(
                text=text,
                model=self.model,
                provider="azure",
                usage=usage,
                finish_reason=finish_reason
            )
            
        except openai.APIError as e:
            logger.error(f"Azure OpenAI API error: {e}")
            raise Exception(f"Azure OpenAI API error: {str(e)}")
        except openai.RateLimitError as e:
            logger.error(f"Azure OpenAI rate limit exceeded: {e}")
            raise Exception(f"Rate limit exceeded: {str(e)}")
        except openai.APIConnectionError as e:
            logger.error(f"Azure OpenAI connection error: {e}")
            raise Exception(f"Connection error: {str(e)}")
        except Exception as e:
            logger.error(f"Unexpected error in Azure OpenAI generation: {e}")
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
            
            # Call Azure OpenAI API with streaming
            logger.info(f"Starting Azure OpenAI streaming with deployment: {self.model}")
            stream = await self.client.chat.completions.create(**params)
            
            async for chunk in stream:
                if chunk.choices and chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content
            
            logger.info("Azure OpenAI streaming completed")
            
        except Exception as e:
            logger.error(f"Error in Azure OpenAI streaming: {e}")
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
            # Fallback: rough estimate
            return len(text) // 4
    
    async def health_check(self) -> bool:
        """
        Check if Azure OpenAI is available
        
        Returns:
            True if available, False otherwise
        """
        try:
            # Try a simple completion as health check
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": "test"}],
                max_tokens=1
            )
            return True
        except Exception as e:
            logger.error(f"Azure OpenAI health check failed: {e}")
            return False
