"""测试向量检索服务"""
import pytest
from unittest.mock import Mock, patch, AsyncMock
from app.retriever import RetrieverService


class TestRetrieverService:
    """测试向量检索服务"""
    
    @pytest.mark.asyncio
    async def test_retrieve_with_results(self):
        """测试检索返回结果"""
        # Arrange
        retriever = RetrieverService()
        
        mock_results = {
            "ids": [["id1", "id2"]],
            "documents": [["文档1内容", "文档2内容"]],
            "metadatas": [[
                {"document_id": "doc1", "filename": "test1.txt"},
                {"document_id": "doc2", "filename": "test2.txt"}
            ]],
            "distances": [[0.1, 0.2]]
        }
        
        with patch("app.retriever.embedding_client.generate_embedding", 
                   new_callable=AsyncMock) as mock_embed, \
             patch("app.retriever.chroma_client.query") as mock_query:
            
            mock_embed.return_value = [0.1] * 384
            mock_query.return_value = mock_results
            
            # Act
            results = await retriever.retrieve("测试问题", top_k=2)
            
            # Assert
            assert len(results) == 2
            assert results[0]["chunk_text"] == "文档1内容"
            assert "similarity_score" in results[0]
    
    @pytest.mark.asyncio
    async def test_retrieve_empty_results(self):
        """测试检索无结果"""
        # Arrange
        retriever = RetrieverService()
        
        with patch("app.retriever.embedding_client.generate_embedding",
                   new_callable=AsyncMock) as mock_embed, \
             patch("app.retriever.chroma_client.query") as mock_query:
            
            mock_embed.return_value = [0.1] * 384
            mock_query.return_value = {"ids": [[]], "documents": [[]], 
                                      "metadatas": [[]], "distances": [[]]}
            
            # Act
            results = await retriever.retrieve("测试问题")
            
            # Assert
            assert len(results) == 0
    
    def test_rerank_by_similarity(self):
        """测试按相似度重排序"""
        # Arrange
        retriever = RetrieverService()
        docs = [
            {"chunk_text": "doc1", "similarity_score": 0.5},
            {"chunk_text": "doc2", "similarity_score": 0.9},
            {"chunk_text": "doc3", "similarity_score": 0.7}
        ]
        
        # Act
        reranked = retriever._rerank(docs)
        
        # Assert
        assert reranked[0]["similarity_score"] == 0.9
        assert reranked[1]["similarity_score"] == 0.7
        assert reranked[2]["similarity_score"] == 0.5
