"""
Base LLM Adapter interface
"""
from abc import ABC, abstractmethod
from typing import Optional, List, AsyncIterator
from app.models import GenerateRequest, GenerateResponse, TokenUsage


class LLMAdapter(ABC):
    """
    Abstract base class for LLM adapters
    All LLM providers must implement this interface
    """
    
    def __init__(self, model: str, **kwargs):
        """
        Initialize the adapter
        
        Args:
            model: Model name/identifier
            **kwargs: Additional provider-specific configuration
        """
        self.model = model
        self.config = kwargs
    
    @abstractmethod
    async def generate(self, request: GenerateRequest) -> GenerateResponse:
        """
        Generate text synchronously
        
        Args:
            request: Generation request
            
        Returns:
            Generated response with text and metadata
            
        Raises:
            Exception: If generation fails
        """
        pass
    
    @abstractmethod
    async def generate_stream(self, request: GenerateRequest) -> AsyncIterator[str]:
        """
        Generate text with streaming
        
        Args:
            request: Generation request
            
        Yields:
            Text chunks as they are generated
            
        Raises:
            Exception: If generation fails
        """
        pass
    
    @abstractmethod
    async def count_tokens(self, text: str) -> int:
        """
        Count tokens in text
        
        Args:
            text: Text to count tokens for
            
        Returns:
            Number of tokens
        """
        pass
    
    @abstractmethod
    async def health_check(self) -> bool:
        """
        Check if the LLM provider is available
        
        Returns:
            True if provider is healthy, False otherwise
        """
        pass
    
    def get_model_name(self) -> str:
        """
        Get the model name
        
        Returns:
            Model name
        """
        return self.model
    
    def validate_request(self, request: GenerateRequest) -> None:
        """
        Validate generation request
        
        Args:
            request: Request to validate
            
        Raises:
            ValueError: If request is invalid
        """
        if not request.prompt or not request.prompt.strip():
            raise ValueError("Prompt cannot be empty")
        
        if request.max_tokens is not None and request.max_tokens <= 0:
            raise ValueError("max_tokens must be positive")
        
        if request.temperature is not None and not (0.0 <= request.temperature <= 2.0):
            raise ValueError("temperature must be between 0.0 and 2.0")
        
        if request.top_p is not None and not (0.0 <= request.top_p <= 1.0):
            raise ValueError("top_p must be between 0.0 and 1.0")
