"""嵌入向量生成器"""
import logging
from typing import List, Optional
from abc import ABC, abstractmethod
import openai
from sentence_transformers import SentenceTransformer
from app.config import settings

logger = logging.getLogger(__name__)


class EmbeddingGenerator(ABC):
    """嵌入向量生成器基类"""
    
    @abstractmethod
    def generate(self, texts: List[str]) -> List[List[float]]:
        """生成嵌入向量"""
        pass
    
    @abstractmethod
    def get_dimension(self) -> int:
        """获取向量维度"""
        pass
    
    @abstractmethod
    def get_model_name(self) -> str:
        """获取模型名称"""
        pass


class OpenAIEmbeddingGenerator(EmbeddingGenerator):
    """OpenAI 嵌入向量生成器"""
    
    def __init__(self):
        self.model = settings.openai_embedding_model
        self.dimension = settings.embedding_dimension
        
        # 配置 OpenAI 客户端
        openai.api_key = settings.openai_api_key
        openai.api_base = settings.openai_api_base
        
        logger.info(f"初始化 OpenAI 嵌入生成器: {self.model}")
    
    def generate(self, texts: List[str]) -> List[List[float]]:
        """使用 OpenAI API 生成嵌入向量"""
        try:
            # 截断过长的文本
            truncated_texts = [
                text[:settings.max_text_length] for text in texts
            ]
            
            # 调用 OpenAI API
            response = openai.embeddings.create(
                model=self.model,
                input=truncated_texts,
                timeout=settings.openai_timeout
            )
            
            # 提取向量
            embeddings = [item.embedding for item in response.data]
            
            logger.info(f"成功生成 {len(embeddings)} 个向量")
            return embeddings
            
        except Exception as e:
            logger.error(f"OpenAI 向量生成失败: {str(e)}")
            raise
    
    def get_dimension(self) -> int:
        """获取向量维度"""
        return self.dimension
    
    def get_model_name(self) -> str:
        """获取模型名称"""
        return self.model


class LocalEmbeddingGenerator(EmbeddingGenerator):
    """本地嵌入向量生成器（BGE 模型）"""
    
    def __init__(self):
        self.model_name = settings.local_model_name
        self.device = settings.local_model_device
        
        logger.info(f"加载本地模型: {self.model_name} (设备: {self.device})")
        
        try:
            self.model = SentenceTransformer(
                self.model_name,
                device=self.device
            )
            self.dimension = self.model.get_sentence_embedding_dimension()
            logger.info(f"本地模型加载成功，向量维度: {self.dimension}")
        except Exception as e:
            logger.error(f"加载本地模型失败: {str(e)}")
            raise
    
    def generate(self, texts: List[str]) -> List[List[float]]:
        """使用本地模型生成嵌入向量"""
        try:
            # 批量生成向量
            embeddings = self.model.encode(
                texts,
                batch_size=settings.batch_size,
                show_progress_bar=False,
                convert_to_numpy=True
            )
            
            # 转换为列表格式
            embeddings_list = [emb.tolist() for emb in embeddings]
            
            logger.info(f"成功生成 {len(embeddings_list)} 个向量")
            return embeddings_list
            
        except Exception as e:
            logger.error(f"本地模型向量生成失败: {str(e)}")
            raise
    
    def get_dimension(self) -> int:
        """获取向量维度"""
        return self.dimension
    
    def get_model_name(self) -> str:
        """获取模型名称"""
        return self.model_name


class EmbeddingService:
    """嵌入向量服务（支持模型切换）"""
    
    def __init__(self):
        self.generator: Optional[EmbeddingGenerator] = None
        self._initialize_generator()
    
    def _initialize_generator(self):
        """初始化向量生成器"""
        try:
            if settings.use_local_model:
                self.generator = LocalEmbeddingGenerator()
            else:
                self.generator = OpenAIEmbeddingGenerator()
            
            logger.info(
                f"嵌入服务初始化完成，使用模型: {self.generator.get_model_name()}"
            )
        except Exception as e:
            logger.error(f"初始化嵌入生成器失败: {str(e)}")
            raise
    
    def generate_embeddings(self, texts: List[str]) -> List[List[float]]:
        """生成嵌入向量"""
        if not self.generator:
            raise RuntimeError("嵌入生成器未初始化")
        
        return self.generator.generate(texts)
    
    def get_dimension(self) -> int:
        """获取向量维度"""
        if not self.generator:
            raise RuntimeError("嵌入生成器未初始化")
        
        return self.generator.get_dimension()
    
    def get_model_name(self) -> str:
        """获取模型名称"""
        if not self.generator:
            raise RuntimeError("嵌入生成器未初始化")
        
        return self.generator.get_model_name()
    
    def get_provider(self) -> str:
        """获取模型提供商"""
        if isinstance(self.generator, OpenAIEmbeddingGenerator):
            return "openai"
        elif isinstance(self.generator, LocalEmbeddingGenerator):
            return "local"
        else:
            return "unknown"
    
    def switch_model(self, use_local: bool, model_name: Optional[str] = None):
        """切换模型"""
        logger.info(f"切换模型: use_local={use_local}, model_name={model_name}")
        
        # 更新配置
        settings.use_local_model = use_local
        if model_name:
            if use_local:
                settings.local_model_name = model_name
            else:
                settings.openai_embedding_model = model_name
        
        # 重新初始化生成器
        self._initialize_generator()


# 全局嵌入服务实例
embedding_service = EmbeddingService()
