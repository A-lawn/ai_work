"""数据模型定义"""
from typing import Optional, Dict, Any
from pydantic import BaseModel, Field


class ProcessDocumentRequest(BaseModel):
    """文档处理请求"""
    document_id: str = Field(..., description="文档 ID")
    file_path: str = Field(..., description="文件路径")
    file_type: str = Field(..., description="文件类型")
    filename: Optional[str] = Field(None, description="文件名")
    metadata: Optional[Dict[str, Any]] = Field(None, description="额外元数据")


class ProcessDocumentResponse(BaseModel):
    """文档处理响应"""
    success: bool = Field(..., description="是否成功")
    document_id: str = Field(..., description="文档 ID")
    chunk_count: int = Field(0, description="分块数量")
    message: str = Field("", description="处理消息")
    error: Optional[str] = Field(None, description="错误信息")


class DeleteVectorsResponse(BaseModel):
    """删除向量响应"""
    success: bool = Field(..., description="是否成功")
    document_id: str = Field(..., description="文档 ID")
    deleted_count: int = Field(0, description="删除的向量数量")
    message: str = Field("", description="处理消息")
