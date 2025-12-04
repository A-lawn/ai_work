"""纯文本文档解析器"""
import logging
import os
from typing import Dict, Any
from app.parsers.base import DocumentParser

logger = logging.getLogger(__name__)


class TextParser(DocumentParser):
    """纯文本文档解析器"""
    
    def parse(self, file_path: str) -> str:
        """
        解析纯文本文档
        
        Args:
            file_path: 文本文件路径
            
        Returns:
            文本内容
        """
        try:
            # 尝试多种编码
            encodings = ['utf-8', 'gbk', 'gb2312', 'latin-1']
            
            for encoding in encodings:
                try:
                    with open(file_path, 'r', encoding=encoding) as f:
                        content = f.read()
                    logger.info(
                        f"成功解析文本文件: {file_path}, "
                        f"编码: {encoding}, 长度: {len(content)}"
                    )
                    return content
                except UnicodeDecodeError:
                    continue
            
            # 如果所有编码都失败，使用二进制模式读取并尝试解码
            with open(file_path, 'rb') as f:
                content = f.read()
            
            # 尝试解码为 UTF-8，忽略错误
            text = content.decode('utf-8', errors='ignore')
            logger.warning(f"使用容错模式解析文本文件: {file_path}")
            
            return text
            
        except Exception as e:
            logger.error(f"解析文本文件失败: {file_path}, 错误: {str(e)}")
            raise Exception(f"文本文件解析失败: {str(e)}")
    
    def get_metadata(self, file_path: str) -> Dict[str, Any]:
        """
        获取文本文档元数据
        
        Args:
            file_path: 文本文件路径
            
        Returns:
            文档元数据
        """
        try:
            stat = os.stat(file_path)
            
            # 读取文件计算行数
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    line_count = sum(1 for _ in f)
            except:
                line_count = 0
            
            return {
                "file_size": stat.st_size,
                "line_count": line_count,
                "created": str(stat.st_ctime),
                "modified": str(stat.st_mtime)
            }
        except Exception as e:
            logger.warning(f"获取文本文件元数据失败: {str(e)}")
            return {}
    
    @property
    def supported_extensions(self) -> list:
        """支持的文件扩展名"""
        return [".txt", ".log", ".csv"]
