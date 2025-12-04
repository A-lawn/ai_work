"""
Data models for LLM Service
"""
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from enum import Enum


class LLMProvider(str, Enum):
    """LLM provider types"""
    OPENAI = "openai"
    AZURE = "azure"
    LOCAL = "local"


class GenerateRequest(BaseModel):
    """Request model for LLM generation"""
    prompt: str = Field(..., description="The prompt to send to the LLM")
    max_tokens: Optional[int] = Field(None, description="Maximum tokens to generate")
    temperature: Optional[float] = Field(None, description="Sampling temperature (0.0 to 2.0)")
    top_p: Optional[float] = Field(None, description="Nucleus sampling parameter")
    stream: bool = Field(False, description="Whether to stream the response")
    stop: Optional[List[str]] = Field(None, description="Stop sequences")
    presence_penalty: Optional[float] = Field(None, description="Presence penalty (-2.0 to 2.0)")
    frequency_penalty: Optional[float] = Field(None, description="Frequency penalty (-2.0 to 2.0)")
    
    class Config:
        json_schema_extra = {
            "example": {
                "prompt": "你是一个专业的运维助手。请回答以下问题：如何排查服务器CPU使用率过高的问题？",
                "max_tokens": 500,
                "temperature": 0.7,
                "stream": False
            }
        }


class TokenUsage(BaseModel):
    """Token usage information"""
    prompt_tokens: int = Field(..., description="Number of tokens in the prompt")
    completion_tokens: int = Field(..., description="Number of tokens in the completion")
    total_tokens: int = Field(..., description="Total number of tokens used")


class GenerateResponse(BaseModel):
    """Response model for LLM generation"""
    text: str = Field(..., description="Generated text")
    model: str = Field(..., description="Model used for generation")
    provider: str = Field(..., description="LLM provider")
    usage: TokenUsage = Field(..., description="Token usage information")
    finish_reason: Optional[str] = Field(None, description="Reason for completion")
    
    class Config:
        json_schema_extra = {
            "example": {
                "text": "排查服务器CPU使用率过高的问题，可以按照以下步骤进行...",
                "model": "gpt-4",
                "provider": "openai",
                "usage": {
                    "prompt_tokens": 50,
                    "completion_tokens": 200,
                    "total_tokens": 250
                },
                "finish_reason": "stop"
            }
        }


class StreamChunk(BaseModel):
    """Streaming response chunk"""
    delta: str = Field(..., description="Text delta for this chunk")
    finish_reason: Optional[str] = Field(None, description="Reason for completion (only in last chunk)")


class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Service status")
    provider: str = Field(..., description="Current LLM provider")
    model: str = Field(..., description="Current model")
    provider_available: bool = Field(..., description="Whether the LLM provider is available")
    message: Optional[str] = Field(None, description="Additional status message")


class ErrorResponse(BaseModel):
    """Error response model"""
    error: str = Field(..., description="Error type")
    message: str = Field(..., description="Error message")
    details: Optional[Dict[str, Any]] = Field(None, description="Additional error details")
