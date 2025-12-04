"""文本分块器模块"""
import logging
import re
from typing import List, Dict, Any
from langchain.text_splitter import RecursiveCharacterTextSplitter

from app.config import settings

logger = logging.getLogger(__name__)


class DocumentChunker:
    """文档分块器 - 使用 LangChain 的 RecursiveCharacterTextSplitter"""
    
    def __init__(
        self,
        chunk_size: int = None,
        chunk_overlap: int = None
    ):
        """
        初始化文档分块器
        
        Args:
            chunk_size: 分块大小（tokens）
            chunk_overlap: 分块重叠大小（tokens）
        """
        self.chunk_size = chunk_size or settings.chunk_size
        self.chunk_overlap = chunk_overlap or settings.chunk_overlap
        
        # 创建 LangChain 文本分割器
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=self.chunk_size,
            chunk_overlap=self.chunk_overlap,
            length_function=len,
            separators=[
                "\n\n",  # 段落分隔
                "\n",    # 行分隔
                "。",    # 中文句号
                "！",    # 中文感叹号
                "？",    # 中文问号
                ".",     # 英文句号
                "!",     # 英文感叹号
                "?",     # 英文问号
                ";",     # 分号
                ",",     # 逗号
                " ",     # 空格
                ""       # 字符级别
            ]
        )
        
        logger.info(
            f"初始化文档分块器: chunk_size={self.chunk_size}, "
            f"chunk_overlap={self.chunk_overlap}"
        )
    
    def chunk_text(
        self,
        text: str,
        document_id: str,
        filename: str,
        metadata: Dict[str, Any] = None
    ) -> List[Dict[str, Any]]:
        """
        将文本分割成多个块
        
        Args:
            text: 要分割的文本
            document_id: 文档 ID
            filename: 文件名
            metadata: 额外的元数据
            
        Returns:
            分块结果列表，每个元素包含文本和元数据
        """
        if not text or not text.strip():
            logger.warning(f"文档 {document_id} 的文本为空")
            return []
        
        try:
            # 使用 LangChain 分割文本
            chunks = self.text_splitter.split_text(text)
            
            # 构建分块结果
            chunk_results = []
            for idx, chunk_text in enumerate(chunks):
                if not chunk_text.strip():
                    continue
                
                # 提取结构信息（页码、章节等）
                structure_info = self._extract_structure_info(chunk_text)
                
                chunk_data = {
                    "chunk_index": idx,
                    "chunk_text": chunk_text.strip(),
                    "document_id": document_id,
                    "filename": filename,
                    "chunk_size": len(chunk_text),
                    **structure_info
                }
                
                # 添加额外元数据
                if metadata:
                    chunk_data.update(metadata)
                
                chunk_results.append(chunk_data)
            
            logger.info(
                f"文档 {document_id} 分块完成: "
                f"原始长度={len(text)}, 分块数={len(chunk_results)}"
            )
            
            return chunk_results
            
        except Exception as e:
            logger.error(f"文本分块失败: {str(e)}")
            raise Exception(f"文本分块失败: {str(e)}")
    
    def _extract_structure_info(self, text: str) -> Dict[str, Any]:
        """
        从文本中提取结构信息（页码、章节等）
        
        Args:
            text: 文本内容
            
        Returns:
            结构信息字典
        """
        info = {}
        
        # 提取页码信息
        page_pattern = r'\[Page (\d+)\]'
        page_match = re.search(page_pattern, text)
        if page_match:
            info["page_number"] = int(page_match.group(1))
        
        # 提取标题信息
        heading_pattern = r'\[(Heading \d+)\]'
        heading_match = re.search(heading_pattern, text)
        if heading_match:
            info["section"] = heading_match.group(1)
        
        # 提取表格信息
        table_pattern = r'\[Table (\d+)\]'
        table_match = re.search(table_pattern, text)
        if table_match:
            info["table_number"] = int(table_match.group(1))
        
        return info
    
    def estimate_token_count(self, text: str) -> int:
        """
        估算文本的 token 数量
        
        Args:
            text: 文本内容
            
        Returns:
            估算的 token 数量
        """
        # 简单估算：中文按字符数，英文按单词数 * 1.3
        chinese_chars = len(re.findall(r'[\u4e00-\u9fff]', text))
        english_words = len(re.findall(r'\b[a-zA-Z]+\b', text))
        
        # 中文：1 字符 ≈ 1 token
        # 英文：1 单词 ≈ 1.3 tokens
        estimated_tokens = chinese_chars + int(english_words * 1.3)
        
        return estimated_tokens
    
    def validate_chunk_size(self, chunks: List[Dict[str, Any]]) -> bool:
        """
        验证分块大小是否合理
        
        Args:
            chunks: 分块列表
            
        Returns:
            是否所有分块都在合理范围内
        """
        for chunk in chunks:
            token_count = self.estimate_token_count(chunk["chunk_text"])
            if token_count > self.chunk_size * 1.5:  # 允许 50% 的超出
                logger.warning(
                    f"分块 {chunk['chunk_index']} 超出大小限制: "
                    f"{token_count} tokens"
                )
                return False
        
        return True
