"""测试文档解析器"""
import pytest
from app.parsers.text_parser import TextParser
from app.parsers.markdown_parser import MarkdownParser


class TestTextParser:
    """测试文本解析器"""
    
    def test_parse_simple_text(self, tmp_path):
        """测试解析简单文本文件"""
        # Arrange
        test_file = tmp_path / "test.txt"
        test_content = "这是测试内容\n第二行内容"
        test_file.write_text(test_content, encoding="utf-8")
        
        parser = TextParser()
        
        # Act
        result = parser.parse(str(test_file))
        
        # Assert
        assert result == test_content
        assert "测试内容" in result
    
    def test_parse_empty_file(self, tmp_path):
        """测试解析空文件"""
        # Arrange
        test_file = tmp_path / "empty.txt"
        test_file.write_text("", encoding="utf-8")
        
        parser = TextParser()
        
        # Act
        result = parser.parse(str(test_file))
        
        # Assert
        assert result == ""


class TestMarkdownParser:
    """测试 Markdown 解析器"""
    
    def test_parse_markdown_with_headers(self, tmp_path):
        """测试解析带标题的 Markdown"""
        # Arrange
        test_file = tmp_path / "test.md"
        test_content = "# 标题\n\n这是内容"
        test_file.write_text(test_content, encoding="utf-8")
        
        parser = MarkdownParser()
        
        # Act
        result = parser.parse(str(test_file))
        
        # Assert
        assert "标题" in result
        assert "内容" in result
    
    def test_supported_extensions(self):
        """测试支持的文件扩展名"""
        parser = MarkdownParser()
        assert ".md" in parser.supported_extensions
