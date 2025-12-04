"""文档解析器模块"""
from app.parsers.base import DocumentParser
from app.parsers.pdf_parser import PDFParser
from app.parsers.docx_parser import DOCXParser
from app.parsers.text_parser import TextParser
from app.parsers.markdown_parser import MarkdownParser

__all__ = [
    "DocumentParser",
    "PDFParser",
    "DOCXParser",
    "TextParser",
    "MarkdownParser"
]
