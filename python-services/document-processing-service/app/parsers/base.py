"""文档解析器基类"""
from abc import ABC, abstractmethod
from typing import Dict, Any


class DocumentParser(ABC):
    """文档解析器抽象基类"""
    
    @abstractmethod
    def parse(self, file_path: str) -> str:
        """
        解析文档并提取文本内容
        
        Args:
            file_path: 文档文件路径
            
        Returns:
            提取的文本内容
            
        Raises:
            Exception: 解析失败时抛出异常
        """
        pass
    
    @abstractmethod
    def get_metadata(self, file_path: str) -> Dict[str, Any]:
        """
        获取文档元数据
        
        Args:
            file_path: 文档文件路径
            
        Returns:
            文档元数据字典
        """
        pass
    
    @property
    @abstractmethod
    def supported_extensions(self) -> list:
        """返回支持的文件扩展名列表"""
        pass
