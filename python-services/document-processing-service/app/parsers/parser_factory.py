"""文档解析器工厂"""
import logging
from typing import Optional
from pathlib import Path

from app.parsers.base import DocumentParser
from app.parsers.pdf_parser import PDFParser
from app.parsers.docx_parser import DOCXParser
from app.parsers.text_parser import TextParser
from app.parsers.markdown_parser import MarkdownParser

logger = logging.getLogger(__name__)


class ParserFactory:
    """文档解析器工厂类"""
    
    # 解析器注册表
    _parsers = {
        ".pdf": PDFParser,
        ".docx": DOCXParser,
        ".doc": DOCXParser,
        ".txt": TextParser,
        ".log": TextParser,
        ".csv": TextParser,
        ".md": MarkdownParser,
        ".markdown": MarkdownParser
    }
    
    @classmethod
    def get_parser(cls, file_path: str) -> Optional[DocumentParser]:
        """
        根据文件扩展名获取对应的解析器
        
        Args:
            file_path: 文件路径
            
        Returns:
            文档解析器实例，如果不支持则返回 None
        """
        file_ext = Path(file_path).suffix.lower()
        
        parser_class = cls._parsers.get(file_ext)
        if parser_class:
            logger.info(f"为文件 {file_path} 选择解析器: {parser_class.__name__}")
            return parser_class()
        
        logger.warning(f"不支持的文件格式: {file_ext}")
        return None
    
    @classmethod
    def is_supported(cls, file_path: str) -> bool:
        """
        检查文件格式是否支持
        
        Args:
            file_path: 文件路径
            
        Returns:
            是否支持该文件格式
        """
        file_ext = Path(file_path).suffix.lower()
        return file_ext in cls._parsers
    
    @classmethod
    def get_supported_extensions(cls) -> list:
        """
        获取所有支持的文件扩展名
        
        Returns:
            支持的文件扩展名列表
        """
        return list(cls._parsers.keys())
