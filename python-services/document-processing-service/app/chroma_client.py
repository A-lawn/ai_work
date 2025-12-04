"""ChromaDB 客户端模块"""
import logging
from typing import List, Dict, Any, Optional
import chromadb
from chromadb.config import Settings as ChromaSettings

from app.config import settings

logger = logging.getLogger(__name__)


class ChromaDBClient:
    """ChromaDB 客户端封装"""
    
    def __init__(self):
        """初始化 ChromaDB 客户端"""
        self.client: Optional[chromadb.Client] = None
        self.collection = None
        self.collection_name = "ops_knowledge_base"
        
    def connect(self) -> bool:
        """
        连接到 ChromaDB
        
        Returns:
            连接是否成功
        """
        try:
            # 创建 ChromaDB 客户端
            self.client = chromadb.HttpClient(
                host=settings.chroma_host,
                port=settings.chroma_port,
                settings=ChromaSettings(
                    anonymized_telemetry=False
                )
            )
            
            # 获取或创建集合
            try:
                self.collection = self.client.get_collection(
                    name=self.collection_name
                )
                logger.info(f"获取已存在的集合: {self.collection_name}")
            except:
                self.collection = self.client.create_collection(
                    name=self.collection_name,
                    metadata={"description": "运维知识库向量集合"}
                )
                logger.info(f"创建新集合: {self.collection_name}")
            
            logger.info(
                f"成功连接到 ChromaDB: "
                f"{settings.chroma_host}:{settings.chroma_port}"
            )
            return True
            
        except Exception as e:
            logger.error(f"连接 ChromaDB 失败: {str(e)}")
            return False
    
    def add_documents(
        self,
        documents: List[str],
        embeddings: List[List[float]],
        metadatas: List[Dict[str, Any]],
        ids: List[str]
    ) -> bool:
        """
        添加文档到 ChromaDB
        
        Args:
            documents: 文档文本列表
            embeddings: 向量列表
            metadatas: 元数据列表
            ids: 文档 ID 列表
            
        Returns:
            是否添加成功
        """
        if not self.collection:
            if not self.connect():
                return False
        
        try:
            self.collection.add(
                documents=documents,
                embeddings=embeddings,
                metadatas=metadatas,
                ids=ids
            )
            
            logger.info(f"成功添加 {len(documents)} 个文档到 ChromaDB")
            return True
            
        except Exception as e:
            logger.error(f"添加文档到 ChromaDB 失败: {str(e)}")
            return False
    
    def delete_by_document_id(self, document_id: str) -> int:
        """
        根据文档 ID 删除所有相关的向量
        
        Args:
            document_id: 文档 ID
            
        Returns:
            删除的向量数量
        """
        if not self.collection:
            if not self.connect():
                return 0
        
        try:
            # 查询该文档的所有向量
            results = self.collection.get(
                where={"document_id": document_id}
            )
            
            if not results or not results.get('ids'):
                logger.warning(f"未找到文档 {document_id} 的向量")
                return 0
            
            # 删除向量
            ids_to_delete = results['ids']
            self.collection.delete(ids=ids_to_delete)
            
            deleted_count = len(ids_to_delete)
            logger.info(f"成功删除文档 {document_id} 的 {deleted_count} 个向量")
            
            return deleted_count
            
        except Exception as e:
            logger.error(f"删除文档向量失败: {str(e)}")
            return 0
    
    def query(
        self,
        query_embeddings: List[List[float]],
        n_results: int = 5,
        where: Dict[str, Any] = None
    ) -> Dict[str, Any]:
        """
        查询相似向量
        
        Args:
            query_embeddings: 查询向量
            n_results: 返回结果数量
            where: 过滤条件
            
        Returns:
            查询结果
        """
        if not self.collection:
            if not self.connect():
                return {}
        
        try:
            results = self.collection.query(
                query_embeddings=query_embeddings,
                n_results=n_results,
                where=where
            )
            
            return results
            
        except Exception as e:
            logger.error(f"查询向量失败: {str(e)}")
            return {}
    
    def get_collection_stats(self) -> Dict[str, Any]:
        """
        获取集合统计信息
        
        Returns:
            统计信息
        """
        if not self.collection:
            if not self.connect():
                return {}
        
        try:
            count = self.collection.count()
            
            return {
                "collection_name": self.collection_name,
                "document_count": count
            }
            
        except Exception as e:
            logger.error(f"获取集合统计信息失败: {str(e)}")
            return {}


# 全局 ChromaDB 客户端实例
chroma_client = ChromaDBClient()
