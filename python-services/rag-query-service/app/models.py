"""数据模型"""
from pydantic import BaseModel, Field
from typing import List, Optional


class QueryRequest(BaseModel):
    """查询请求"""
    question: str = Field(..., description="用户问题")
    session_history: Optional[List[dict]] = Field(
        default=None,
        description="会话历史（可选）"
    )
    top_k: Optional[int] = Field(
        default=None,
        description="返回的文档块数量（可选）"
    )
    similarity_threshold: Optional[float] = Field(
        default=None,
        description="相似度阈值（可选）"
    )
    stream: Optional[bool] = Field(
        default=False,
        description="是否使用流式响应"
    )


class Source(BaseModel):
    """引用来源"""
    chunk_text: str = Field(..., description="文档块文本")
    similarity_score: float = Field(..., description="相似度分数")
    document_id: str = Field(..., description="文档 ID")
    document_name: str = Field(..., description="文档名称")
    chunk_index: int = Field(..., description="文档块索引")
    page_number: Optional[int] = Field(None, description="页码")
    section: Optional[str] = Field(None, description="章节")


class QueryResponse(BaseModel):
    """查询响应"""
    answer: str = Field(..., description="生成的答案")
    sources: List[Source] = Field(..., description="引用来源列表")
    query_time: float = Field(..., description="查询耗时（秒）")


class HealthResponse(BaseModel):
    """健康检查响应"""
    status: str
    chroma_connected: bool
    redis_connected: bool
    nacos_registered: bool
    collection_count: int
