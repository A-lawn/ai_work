"""测试文本分块器"""
import pytest
from app.chunker import DocumentChunker


class TestDocumentChunker:
    """测试文档分块器"""
    
    def test_chunk_simple_text(self):
        """测试分块简单文本"""
        # Arrange
        chunker = DocumentChunker(chunk_size=100, chunk_overlap=10)
        text = "这是第一段内容。\n\n这是第二段内容。\n\n这是第三段内容。"
        
        # Act
        chunks = chunker.chunk_text(
            text=text,
            document_id="doc-123",
            filename="test.txt"
        )
        
        # Assert
        assert len(chunks) > 0
        assert all("chunk_text" in chunk for chunk in chunks)
        assert all("document_id" in chunk for chunk in chunks)
        assert chunks[0]["document_id"] == "doc-123"
    
    def test_chunk_empty_text(self):
        """测试分块空文本"""
        # Arrange
        chunker = DocumentChunker()
        
        # Act
        chunks = chunker.chunk_text(
            text="",
            document_id="doc-123",
            filename="empty.txt"
        )
        
        # Assert
        assert len(chunks) == 0
    
    def test_estimate_token_count(self):
        """测试估算 token 数量"""
        # Arrange
        chunker = DocumentChunker()
        text = "这是中文内容 and some English words"
        
        # Act
        token_count = chunker.estimate_token_count(text)
        
        # Assert
        assert token_count > 0
        assert isinstance(token_count, int)
