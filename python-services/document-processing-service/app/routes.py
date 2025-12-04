"""API 路由定义"""
import logging
from fastapi import APIRouter, HTTPException, status

from app.models import (
    ProcessDocumentRequest,
    ProcessDocumentResponse,
    DeleteVectorsResponse
)
from app.document_processor import document_processor

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["document-processing"])


@router.post("/process-document", response_model=ProcessDocumentResponse)
async def process_document(request: ProcessDocumentRequest):
    """
    处理文档 - 解析、分块、向量化并存储
    
    Args:
        request: 文档处理请求
        
    Returns:
        处理结果
    """
    try:
        logger.info(f"收到文档处理请求: {request.document_id}")
        
        # 处理文档
        result = await document_processor.process_document(
            document_id=request.document_id,
            file_path=request.file_path,
            file_type=request.file_type,
            filename=request.filename,
            metadata=request.metadata
        )
        
        # 构建响应
        response = ProcessDocumentResponse(
            success=result["success"],
            document_id=result["document_id"],
            chunk_count=result["chunk_count"],
            message=result["message"],
            error=result.get("error")
        )
        
        if not result["success"]:
            logger.warning(
                f"文档处理失败: {request.document_id}, "
                f"错误: {result.get('error')}"
            )
        
        return response
        
    except Exception as e:
        logger.error(f"处理文档请求异常: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"文档处理失败: {str(e)}"
        )


@router.delete("/vectors/{document_id}", response_model=DeleteVectorsResponse)
async def delete_vectors(document_id: str):
    """
    删除文档的所有向量
    
    Args:
        document_id: 文档 ID
        
    Returns:
        删除结果
    """
    try:
        logger.info(f"收到删除向量请求: {document_id}")
        
        # 删除向量
        result = document_processor.delete_document_vectors(document_id)
        
        # 构建响应
        response = DeleteVectorsResponse(
            success=result["success"],
            document_id=result["document_id"],
            deleted_count=result["deleted_count"],
            message=result["message"]
        )
        
        if not result["success"]:
            logger.warning(
                f"删除向量失败: {document_id}, "
                f"错误: {result.get('error')}"
            )
        
        return response
        
    except Exception as e:
        logger.error(f"删除向量请求异常: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"删除向量失败: {str(e)}"
        )
