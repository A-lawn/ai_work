"""
LLM Service - FastAPI Application
Provides LLM generation capabilities with support for multiple providers
"""
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import time
from app.config import settings
from app.logging_config import setup_logging, get_logger
from app.nacos_client import nacos_client
from app.routes import router
from app.metrics import get_metrics

# Setup logging
setup_logging(settings.SERVICE_NAME)
logger = get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager for startup and shutdown events
    """
    # Startup
    logger.info(f"Starting {settings.SERVICE_NAME}...")
    logger.info(f"LLM Provider: {settings.LLM_PROVIDER}")
    logger.info(f"Service Port: {settings.SERVICE_PORT}")
    
    # Register with Nacos
    if settings.NACOS_SERVER:
        success = nacos_client.register_service()
        if success:
            logger.info("Service registered with Nacos successfully")
        else:
            logger.warning("Failed to register with Nacos, continuing without service discovery")
    
    # Setup tracing if enabled
    if settings.ENABLE_TRACING:
        try:
            from opentelemetry import trace
            from opentelemetry.exporter.zipkin.json import ZipkinExporter
            from opentelemetry.sdk.trace import TracerProvider
            from opentelemetry.sdk.trace.export import BatchSpanProcessor
            
            zipkin_exporter = ZipkinExporter(endpoint=settings.ZIPKIN_ENDPOINT)
            trace.set_tracer_provider(TracerProvider())
            trace.get_tracer_provider().add_span_processor(
                BatchSpanProcessor(zipkin_exporter)
            )
            logger.info("Zipkin tracing configured successfully")
        except Exception as e:
            logger.warning(f"Failed to setup tracing: {e}")
    
    logger.info(f"{settings.SERVICE_NAME} started successfully")
    
    yield
    
    # Shutdown
    logger.info(f"Shutting down {settings.SERVICE_NAME}...")
    
    # Deregister from Nacos
    if settings.NACOS_SERVER:
        nacos_client.deregister_service()
    
    logger.info(f"{settings.SERVICE_NAME} shutdown complete")


# Create FastAPI application
app = FastAPI(
    title="LLM Service",
    description="Large Language Model service for RAG system with multi-provider support",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all incoming requests"""
    start_time = time.time()
    
    # Log request
    logger.info(f"Request: {request.method} {request.url.path}")
    
    # Process request
    try:
        response = await call_next(request)
        
        # Log response
        duration = time.time() - start_time
        logger.info(
            f"Response: {request.method} {request.url.path} "
            f"Status: {response.status_code} Duration: {duration:.3f}s"
        )
        
        return response
        
    except Exception as e:
        duration = time.time() - start_time
        logger.error(
            f"Request failed: {request.method} {request.url.path} "
            f"Error: {str(e)} Duration: {duration:.3f}s"
        )
        raise


# Exception handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler"""
    logger.error(f"Unhandled exception: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={
            "error": "InternalServerError",
            "message": "An internal server error occurred",
            "details": {"error": str(exc)}
        }
    )


# Include routers
app.include_router(router)


# Metrics endpoint
@app.get("/metrics", include_in_schema=False)
async def metrics():
    """Prometheus metrics endpoint"""
    return get_metrics()


# Root endpoint
@app.get("/", tags=["Root"])
async def root():
    """Root endpoint"""
    return {
        "service": settings.SERVICE_NAME,
        "version": "1.0.0",
        "status": "running",
        "provider": settings.LLM_PROVIDER
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.SERVICE_HOST,
        port=settings.SERVICE_PORT,
        reload=False,
        log_level="info"
    )
