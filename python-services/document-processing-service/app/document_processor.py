"""文档处理核心逻辑"""
import logging
import os
import asyncio
from typing import Dict, Any, Optional

from app.parsers.parser_factory import ParserFactory
from app.chunker import DocumentChunker
from app.embedding_client import embedding_client
from app.chroma_client import chroma_client

logger = logging.getLogger(__name__)


class DocumentProcessor:
    """文档处理器 - 整合解析、分块、向量化流程"""
    
    def __init__(self):
        """初始化文档处理器"""
        self.chunker = DocumentChunker()
        
    async def process_document(
        self,
        document_id: str,
        file_path: str,
        file_type: str,
        filename: str = None,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        处理文档的完整流程
        
        Args:
            document_id: 文档 ID
            file_path: 文件路径
            file_type: 文件类型
            filename: 文件名
            metadata: 额外元数据
            
        Returns:
            处理结果
        """
        try:
            logger.info(f"开始处理文档: {document_id}, 文件: {file_path}")
            
            # 1. 验证文件存在
            if not os.path.exists(file_path):
                raise FileNotFoundError(f"文件不存在: {file_path}")
            
            # 2. 获取解析器
            parser = ParserFactory.get_parser(file_path)
            if not parser:
                raise ValueError(f"不支持的文件格式: {file_type}")
            
            # 3. 解析文档
            logger.info(f"解析文档: {document_id}")
            text_content = parser.parse(file_path)
            
            if not text_content or not text_content.strip():
                raise ValueError("文档内容为空")
            
            # 获取文档元数据
            doc_metadata = parser.get_metadata(file_path)
            
            # 4. 文本分块
            logger.info(f"分块文档: {document_id}")
            chunks = self.chunker.chunk_text(
                text=text_content,
                document_id=document_id,
                filename=filename or os.path.basename(file_path),
                metadata={
                    **(metadata or {}),
                    **doc_metadata
                }
            )
            
            if not chunks:
                raise ValueError("文档分块失败，未生成任何分块")
            
            logger.info(f"文档 {document_id} 生成 {len(chunks)} 个分块")
            
            # 5. 生成向量
            logger.info(f"生成向量: {document_id}")
            chunk_texts = [chunk["chunk_text"] for chunk in chunks]
            embeddings = await embedding_client.generate_embeddings(chunk_texts)
            
            if not embeddings or len(embeddings) != len(chunks):
                raise ValueError(
                    f"向量生成失败: 期望 {len(chunks)} 个向量，"
                    f"实际 {len(embeddings)} 个"
                )
            
            # 6. 存储到 ChromaDB
            logger.info(f"存储向量到 ChromaDB: {document_id}")
            
            # 准备数据
            ids = [f"{document_id}_chunk_{i}" for i in range(len(chunks))]
            documents = chunk_texts
            metadatas = [
                {
                    "document_id": chunk["document_id"],
                    "filename": chunk["filename"],
                    "chunk_index": chunk["chunk_index"],
                    "chunk_size": chunk["chunk_size"],
                    **{k: v for k, v in chunk.items() 
                       if k not in ["chunk_text", "document_id", "filename", 
                                   "chunk_index", "chunk_size"]}
                }
                for chunk in chunks
            ]
            
            # 存储到 ChromaDB
            success = chroma_client.add_documents(
                documents=documents,
                embeddings=embeddings,
                metadatas=metadatas,
                ids=ids
            )
            
            if not success:
                raise Exception("存储向量到 ChromaDB 失败")
            
            logger.info(f"文档处理完成: {document_id}, 分块数: {len(chunks)}")
            
            return {
                "success": True,
                "document_id": document_id,
                "chunk_count": len(chunks),
                "message": "文档处理成功"
            }
            
        except FileNotFoundError as e:
            logger.error(f"文件不存在: {str(e)}")
            return {
                "success": False,
                "document_id": document_id,
                "chunk_count": 0,
                "message": "文件不存在",
                "error": str(e)
            }
        except ValueError as e:
            logger.error(f"文档处理验证失败: {str(e)}")
            return {
                "success": False,
                "document_id": document_id,
                "chunk_count": 0,
                "message": "文档处理验证失败",
                "error": str(e)
            }
        except Exception as e:
            logger.error(f"文档处理失败: {document_id}, 错误: {str(e)}")
            return {
                "success": False,
                "document_id": document_id,
                "chunk_count": 0,
                "message": "文档处理失败",
                "error": str(e)
            }
    
    def delete_document_vectors(self, document_id: str) -> Dict[str, Any]:
        """
        删除文档的所有向量
        
        Args:
            document_id: 文档 ID
            
        Returns:
            删除结果
        """
        try:
            logger.info(f"删除文档向量: {document_id}")
            
            deleted_count = chroma_client.delete_by_document_id(document_id)
            
            return {
                "success": True,
                "document_id": document_id,
                "deleted_count": deleted_count,
                "message": f"成功删除 {deleted_count} 个向量"
            }
            
        except Exception as e:
            logger.error(f"删除文档向量失败: {document_id}, 错误: {str(e)}")
            return {
                "success": False,
                "document_id": document_id,
                "deleted_count": 0,
                "message": "删除向量失败",
                "error": str(e)
            }
    
    def process_document_sync(
        self,
        document_id: str,
        file_path: str,
        file_type: str,
        filename: str = None,
        metadata: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        同步处理文档（用于 Celery 任务）
        
        Args:
            document_id: 文档 ID
            file_path: 文件路径
            file_type: 文件类型
            filename: 文件名
            metadata: 额外元数据
            
        Returns:
            处理结果
        """
        # 创建新的事件循环
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            return loop.run_until_complete(
                self.process_document(document_id, file_path, file_type, filename, metadata)
            )
        finally:
            loop.close()


# 全局文档处理器实例
document_processor = DocumentProcessor()
