"""向量检索服务"""
import logging
import time
from typing import List, Dict, Any, Optional
from app.chroma_client import chroma_client
from app.embedding_client import embedding_client
from app.config import settings
from app.metrics import retrieval_duration, retrieval_results_count

logger = logging.getLogger(__name__)


class RetrieverService:
    """向量检索服务"""
    
    def __init__(self):
        self.default_top_k = settings.top_k
        self.default_threshold = settings.similarity_threshold
    
    async def retrieve(
        self,
        question: str,
        top_k: Optional[int] = None,
        similarity_threshold: Optional[float] = None
    ) -> List[Dict[str, Any]]:
        """检索相关文档块
        
        Args:
            question: 用户问题
            top_k: 返回的文档块数量
            similarity_threshold: 相似度阈值
            
        Returns:
            检索结果列表
        """
        start_time = time.time()
        
        try:
            # 使用默认值
            k = top_k if top_k is not None else self.default_top_k
            threshold = (
                similarity_threshold 
                if similarity_threshold is not None 
                else self.default_threshold
            )
            
            # 1. 生成查询向量
            logger.info(f"生成查询向量: {question[:50]}...")
            query_embedding = await embedding_client.generate_embedding(question)
            
            if not query_embedding:
                logger.error("生成查询向量失败")
                return []
            
            # 2. 向量检索
            logger.info(f"执行向量检索 (top_k={k}, threshold={threshold})...")
            results = chroma_client.query(
                query_embeddings=[query_embedding],
                n_results=k
            )
            
            # 3. 处理检索结果
            retrieved_docs = self._process_results(results, threshold)
            
            # 4. 重排序（按相似度降序）
            retrieved_docs = self._rerank(retrieved_docs)
            
            # 记录指标
            duration = time.time() - start_time
            retrieval_duration.observe(duration)
            retrieval_results_count.observe(len(retrieved_docs))
            
            logger.info(
                f"检索完成，返回 {len(retrieved_docs)} 个结果，"
                f"耗时 {duration:.2f}秒"
            )
            
            return retrieved_docs
            
        except Exception as e:
            logger.error(f"检索失败: {str(e)}")
            return []
    
    def _process_results(
        self,
        results: Dict[str, Any],
        threshold: float
    ) -> List[Dict[str, Any]]:
        """处理检索结果
        
        Args:
            results: ChromaDB 查询结果
            threshold: 相似度阈值
            
        Returns:
            处理后的文档列表
        """
        processed_docs = []
        
        if not results or not results.get("ids"):
            return processed_docs
        
        # 提取结果
        ids = results["ids"][0]
        documents = results["documents"][0]
        metadatas = results["metadatas"][0]
        distances = results["distances"][0]
        
        for i in range(len(ids)):
            # 将距离转换为相似度分数（距离越小，相似度越高）
            # ChromaDB 使用 L2 距离，转换为 0-1 的相似度分数
            similarity_score = 1.0 / (1.0 + distances[i])
            
            # 过滤低于阈值的结果
            if similarity_score < threshold:
                logger.debug(
                    f"过滤低相似度结果: {similarity_score:.3f} < {threshold}"
                )
                continue
            
            metadata = metadatas[i] or {}
            
            doc = {
                "chunk_text": documents[i],
                "similarity_score": similarity_score,
                "document_id": metadata.get("document_id", "unknown"),
                "document_name": metadata.get("filename", "unknown"),
                "chunk_index": metadata.get("chunk_index", 0),
                "page_number": metadata.get("page_number"),
                "section": metadata.get("section")
            }
            
            processed_docs.append(doc)
        
        return processed_docs
    
    def _rerank(self, docs: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """重排序检索结果
        
        Args:
            docs: 文档列表
            
        Returns:
            重排序后的文档列表
        """
        # 按相似度分数降序排序
        return sorted(
            docs,
            key=lambda x: x["similarity_score"],
            reverse=True
        )


# 全局检索服务实例
retriever_service = RetrieverService()
