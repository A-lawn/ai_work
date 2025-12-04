"""Markdown 文档解析器"""
import logging
import os
from typing import Dict, Any
import markdown
from app.parsers.base import DocumentParser

logger = logging.getLogger(__name__)


class MarkdownParser(DocumentParser):
    """Markdown 文档解析器"""
    
    def parse(self, file_path: str) -> str:
        """
        解析 Markdown 文档
        
        Args:
            file_path: Markdown 文件路径
            
        Returns:
            提取的文本内容（保留结构）
        """
        try:
            # 读取 Markdown 文件
            with open(file_path, 'r', encoding='utf-8') as f:
                md_content = f.read()
            
            # 保留原始 Markdown 格式（更适合 RAG）
            # 如果需要转换为纯文本，可以使用 markdown 库转换为 HTML 再提取文本
            logger.info(
                f"成功解析 Markdown: {file_path}, "
                f"长度: {len(md_content)}"
            )
            
            return md_content
            
        except Exception as e:
            logger.error(f"解析 Markdown 失败: {file_path}, 错误: {str(e)}")
            raise Exception(f"Markdown 解析失败: {str(e)}")
    
    def get_metadata(self, file_path: str) -> Dict[str, Any]:
        """
        获取 Markdown 文档元数据
        
        Args:
            file_path: Markdown 文件路径
            
        Returns:
            文档元数据
        """
        try:
            stat = os.stat(file_path)
            
            # 读取文件统计信息
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
                line_count = content.count('\n') + 1
                
                # 统计标题数量
                heading_count = content.count('\n#')
                
                # 统计代码块数量
                code_block_count = content.count('```') // 2
            
            return {
                "file_size": stat.st_size,
                "line_count": line_count,
                "heading_count": heading_count,
                "code_block_count": code_block_count,
                "created": str(stat.st_ctime),
                "modified": str(stat.st_mtime)
            }
        except Exception as e:
            logger.warning(f"获取 Markdown 元数据失败: {str(e)}")
            return {}
    
    @property
    def supported_extensions(self) -> list:
        """支持的文件扩展名"""
        return [".md", ".markdown"]
