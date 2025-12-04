"""PDF 文档解析器"""
import logging
from typing import Dict, Any
from PyPDF2 import PdfReader
from app.parsers.base import DocumentParser

logger = logging.getLogger(__name__)


class PDFParser(DocumentParser):
    """PDF 文档解析器"""
    
    def parse(self, file_path: str) -> str:
        """
        解析 PDF 文档并提取文本
        
        Args:
            file_path: PDF 文件路径
            
        Returns:
            提取的文本内容
        """
        try:
            reader = PdfReader(file_path)
            text_content = []
            
            for page_num, page in enumerate(reader.pages, start=1):
                try:
                    text = page.extract_text()
                    if text.strip():
                        # 添加页码标记
                        text_content.append(f"\n[Page {page_num}]\n{text}")
                except Exception as e:
                    logger.warning(f"提取第 {page_num} 页失败: {str(e)}")
                    continue
            
            full_text = "\n".join(text_content)
            logger.info(f"成功解析 PDF: {file_path}, 共 {len(reader.pages)} 页")
            
            return full_text
            
        except Exception as e:
            logger.error(f"解析 PDF 失败: {file_path}, 错误: {str(e)}")
            raise Exception(f"PDF 解析失败: {str(e)}")
    
    def get_metadata(self, file_path: str) -> Dict[str, Any]:
        """
        获取 PDF 文档元数据
        
        Args:
            file_path: PDF 文件路径
            
        Returns:
            文档元数据
        """
        try:
            reader = PdfReader(file_path)
            metadata = reader.metadata or {}
            
            return {
                "page_count": len(reader.pages),
                "title": metadata.get("/Title", ""),
                "author": metadata.get("/Author", ""),
                "subject": metadata.get("/Subject", ""),
                "creator": metadata.get("/Creator", ""),
                "producer": metadata.get("/Producer", ""),
                "creation_date": str(metadata.get("/CreationDate", "")),
                "modification_date": str(metadata.get("/ModDate", ""))
            }
        except Exception as e:
            logger.warning(f"获取 PDF 元数据失败: {str(e)}")
            return {}
    
    @property
    def supported_extensions(self) -> list:
        """支持的文件扩展名"""
        return [".pdf"]
