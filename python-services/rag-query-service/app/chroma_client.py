"""ChromaDB 客户端"""
import logging
from typing import Optional, List, Dict, Any
import chromadb
from chromadb.config import Settings as ChromaSettings
from app.config import settings

logger = logging.getLogger(__name__)


class ChromaDBClient:
    """ChromaDB 客户端管理器"""
    
    def __init__(self):
        self.client: Optional[chromadb.HttpClient] = None
        self.collection = None
        
    def connect(self) -> bool:
        """连接到 ChromaDB"""
        try:
            # 创建 HTTP 客户端
            self.client = chromadb.HttpClient(
                host=settings.chroma_host,
                port=settings.chroma_port,
                settings=ChromaSettings(
                    anonymized_telemetry=False
                )
            )
            
            # 测试连接
            self.client.heartbeat()
            
            # 获取或创建集合
            try:
                self.collection = self.client.get_collection(
                    name=settings.chroma_collection_name
                )
                logger.info(f"已连接到现有集合: {settings.chroma_collection_name}")
            except Exception:
                # 如果集合不存在，创建新集合
                self.collection = self.client.create_collection(
                    name=settings.chroma_collection_name,
                    metadata={"description": "运维知识库向量集合"}
                )
                logger.info(f"已创建新集合: {settings.chroma_collection_name}")
            
            logger.info(
                f"成功连接到 ChromaDB: {settings.chroma_host}:{settings.chroma_port}"
            )
            return True
            
        except Exception as e:
            logger.error(f"连接 ChromaDB 失败: {str(e)}")
            return False
    
    def query(
        self,
        query_embeddings: List[List[float]],
        n_results: int = 5,
        where: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """查询相似向量
        
        Args:
            query_embeddings: 查询向量列表
            n_results: 返回结果数量
            where: 元数据过滤条件
            
        Returns:
            查询结果字典
        """
        if not self.collection:
            raise RuntimeError("ChromaDB 未连接")
        
        try:
            results = self.collection.query(
                query_embeddings=query_embeddings,
                n_results=n_results,
                where=where,
                include=["documents", "metadatas", "distances"]
            )
            
            logger.debug(f"查询成功，返回 {len(results['ids'][0])} 个结果")
            return results
            
        except Exception as e:
            logger.error(f"查询向量失败: {str(e)}")
            raise
    
    def get_collection_count(self) -> int:
        """获取集合中的文档数量"""
        if not self.collection:
            return 0
        
        try:
            return self.collection.count()
        except Exception as e:
            logger.error(f"获取集合数量失败: {str(e)}")
            return 0
    
    def delete_by_document_id(self, document_id: str) -> bool:
        """根据文档 ID 删除向量
        
        Args:
            document_id: 文档 ID
            
        Returns:
            是否删除成功
        """
        if not self.collection:
            return False
        
        try:
            # 查询该文档的所有向量
            results = self.collection.get(
                where={"document_id": document_id}
            )
            
            if results and results["ids"]:
                # 删除所有匹配的向量
                self.collection.delete(ids=results["ids"])
                logger.info(f"已删除文档 {document_id} 的 {len(results['ids'])} 个向量")
                return True
            else:
                logger.warning(f"未找到文档 {document_id} 的向量")
                return False
                
        except Exception as e:
            logger.error(f"删除文档向量失败: {str(e)}")
            return False


# 全局 ChromaDB 客户端实例
chroma_client = ChromaDBClient()
