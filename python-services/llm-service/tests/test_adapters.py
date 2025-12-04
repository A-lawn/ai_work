"""测试 LLM 适配器"""
import pytest
from unittest.mock import Mock, patch
from app.adapters.base import BaseLLMAdapter


class MockLLMAdapter(BaseLLMAdapter):
    """Mock LLM 适配器用于测试"""
    
    def generate(self, prompt: str, **kwargs) -> str:
        return f"Response to: {prompt}"
    
    def generate_stream(self, prompt: str, **kwargs):
        yield "Response "
        yield "to: "
        yield prompt


class TestBaseLLMAdapter:
    """测试基础 LLM 适配器"""
    
    def test_generate_returns_string(self):
        """测试生成返回字符串"""
        # Arrange
        adapter = MockLLMAdapter()
        
        # Act
        result = adapter.generate("测试提示词")
        
        # Assert
        assert isinstance(result, str)
        assert "测试提示词" in result
    
    def test_generate_stream_yields_chunks(self):
        """测试流式生成返回块"""
        # Arrange
        adapter = MockLLMAdapter()
        
        # Act
        chunks = list(adapter.generate_stream("测试"))
        
        # Assert
        assert len(chunks) > 0
        assert all(isinstance(chunk, str) for chunk in chunks)
        assert "测试" in "".join(chunks)
