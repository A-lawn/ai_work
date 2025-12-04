"""
API routes for LLM Service
"""
from fastapi import APIRouter, HTTPException, status
from fastapi.responses import StreamingResponse
from app.models import (
    GenerateRequest,
    GenerateResponse,
    HealthResponse,
    ErrorResponse
)
from app.llm_service import llm_service
from app.logging_config import get_logger
import json

logger = get_logger(__name__)

router = APIRouter(prefix="/api", tags=["LLM"])


@router.post(
    "/generate",
    response_model=GenerateResponse,
    summary="Generate text using LLM",
    description="Generate text based on the provided prompt. Supports both synchronous and streaming modes."
)
async def generate_text(request: GenerateRequest):
    """
    Generate text using LLM
    
    Args:
        request: Generation request with prompt and parameters
        
    Returns:
        Generated response with text and metadata
        
    Raises:
        HTTPException: If generation fails
    """
    try:
        # Check if streaming is requested
        if request.stream:
            # Return streaming response
            return StreamingResponse(
                stream_generator(request),
                media_type="text/event-stream"
            )
        
        # Synchronous generation
        logger.info("Received generation request")
        response = await llm_service.generate(request)
        
        return response
        
    except ValueError as e:
        logger.error(f"Validation error: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"Generation failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Generation failed: {str(e)}"
        )


async def stream_generator(request: GenerateRequest):
    """
    Generator for streaming responses
    
    Args:
        request: Generation request
        
    Yields:
        Server-Sent Events formatted chunks
    """
    try:
        logger.info("Starting streaming generation")
        
        async for chunk in llm_service.generate_stream(request):
            # Format as Server-Sent Events
            data = json.dumps({"delta": chunk}, ensure_ascii=False)
            yield f"data: {data}\n\n"
        
        # Send completion event
        yield f"data: {json.dumps({'delta': '', 'finish_reason': 'stop'})}\n\n"
        yield "data: [DONE]\n\n"
        
    except Exception as e:
        logger.error(f"Streaming failed: {e}")
        error_data = json.dumps({"error": str(e)}, ensure_ascii=False)
        yield f"data: {error_data}\n\n"


@router.post(
    "/count-tokens",
    summary="Count tokens in text",
    description="Count the number of tokens in the provided text"
)
async def count_tokens(text: str):
    """
    Count tokens in text
    
    Args:
        text: Text to count tokens for
        
    Returns:
        Token count
    """
    try:
        token_count = await llm_service.count_tokens(text)
        return {
            "text_length": len(text),
            "token_count": token_count
        }
    except Exception as e:
        logger.error(f"Token counting failed: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Token counting failed: {str(e)}"
        )


@router.get(
    "/health",
    response_model=HealthResponse,
    summary="Health check",
    description="Check if the LLM service and provider are available"
)
async def health_check():
    """
    Health check endpoint
    
    Returns:
        Health status
    """
    try:
        provider_available = await llm_service.health_check()
        provider_info = llm_service.get_provider_info()
        
        return HealthResponse(
            status="healthy" if provider_available else "degraded",
            provider=provider_info["provider"],
            model=provider_info["model"],
            provider_available=provider_available,
            message="LLM provider is available" if provider_available else "LLM provider is not available"
        )
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return HealthResponse(
            status="unhealthy",
            provider="unknown",
            model="unknown",
            provider_available=False,
            message=f"Health check failed: {str(e)}"
        )


@router.get(
    "/info",
    summary="Get service information",
    description="Get information about the current LLM provider and configuration"
)
async def get_info():
    """
    Get service information
    
    Returns:
        Service and provider information
    """
    try:
        provider_info = llm_service.get_provider_info()
        return {
            "service": "llm-service",
            "version": "1.0.0",
            **provider_info
        }
    except Exception as e:
        logger.error(f"Failed to get info: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to get info: {str(e)}"
        )
