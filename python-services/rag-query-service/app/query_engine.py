"""RAG 查询引擎"""
import logging
import time
from typing import List, Dict, Any, Optional, AsyncIterator
from app.retriever import retriever_service
from app.llm_client import llm_client
from app.redis_client import redis_cache
from app.config import settings
from app.models import Source
from app.metrics import (
    query_total,
    query_duration,
    llm_generation_duration,
    cache_hit_total,
    cache_miss_total,
    error_total
)

logger = logging.getLogger(__name__)


class QueryEngine:
    """RAG 查询引擎"""
    
    def __init__(self):
        self.prompt_template = settings.prompt_template
    
    def _build_prompt(
        self,
        question: str,
        retrieved_docs: List[Dict[str, Any]],
        session_history: Optional[List[dict]] = None
    ) -> str:
        """构建提示词
        
        Args:
            question: 用户问题
            retrieved_docs: 检索到的文档
            session_history: 会话历史
            
        Returns:
            构建的提示词
        """
        # 构建上下文
        context_parts = []
        for i, doc in enumerate(retrieved_docs, 1):
            context_parts.append(
                f"[文档 {i}] (来源: {doc['document_name']}, "
                f"相似度: {doc['similarity_score']:.2f})\n"
                f"{doc['chunk_text']}"
            )
        
        context = "\n\n".join(context_parts)
        
        # 如果有会话历史，添加到提示词中
        if session_history and len(session_history) > 0:
            history_text = "\n".join([
                f"{msg.get('role', 'user')}: {msg.get('content', '')}"
                for msg in session_history[-5:]  # 只保留最近 5 轮对话
            ])
            
            prompt = f"""会话历史：
{history_text}

{self.prompt_template.format(context=context, question=question)}"""
        else:
            prompt = self.prompt_template.format(
                context=context,
                question=question
            )
        
        return prompt
    
    def _convert_to_sources(
        self,
        retrieved_docs: List[Dict[str, Any]]
    ) -> List[Source]:
        """将检索结果转换为 Source 对象
        
        Args:
            retrieved_docs: 检索到的文档
            
        Returns:
            Source 对象列表
        """
        sources = []
        for doc in retrieved_docs:
            source = Source(
                chunk_text=doc["chunk_text"],
                similarity_score=doc["similarity_score"],
                document_id=doc["document_id"],
                document_name=doc["document_name"],
                chunk_index=doc["chunk_index"],
                page_number=doc.get("page_number"),
                section=doc.get("section")
            )
            sources.append(source)
        
        return sources
    
    async def query(
        self,
        question: str,
        session_history: Optional[List[dict]] = None,
        top_k: Optional[int] = None,
        similarity_threshold: Optional[float] = None,
        use_cache: bool = True
    ) -> Dict[str, Any]:
        """执行 RAG 查询（同步模式）
        
        Args:
            question: 用户问题
            session_history: 会话历史
            top_k: 返回的文档块数量
            similarity_threshold: 相似度阈值
            use_cache: 是否使用缓存
            
        Returns:
            查询结果字典
        """
        start_time = time.time()
        
        try:
            # 1. 检查缓存
            if use_cache:
                cached_result = redis_cache.get_query_result(question)
                if cached_result:
                    cache_hit_total.inc()
                    logger.info(f"缓存命中: {question[:50]}...")
                    query_total.labels(status="cache_hit").inc()
                    return cached_result
                
                cache_miss_total.inc()
            
            # 2. 向量检索
            logger.info(f"开始 RAG 查询: {question[:50]}...")
            retrieved_docs = await retriever_service.retrieve(
                question=question,
                top_k=top_k,
                similarity_threshold=similarity_threshold
            )
            
            if not retrieved_docs:
                logger.warning("未检索到相关文档")
                result = {
                    "answer": "抱歉，我在知识库中没有找到相关信息来回答您的问题。",
                    "sources": [],
                    "query_time": time.time() - start_time
                }
                query_total.labels(status="no_results").inc()
                return result
            
            # 3. 构建提示词
            prompt = self._build_prompt(question, retrieved_docs, session_history)
            logger.debug(f"提示词长度: {len(prompt)} 字符")
            
            # 4. 调用 LLM 生成答案
            llm_start_time = time.time()
            answer = await llm_client.generate(prompt)
            llm_duration = time.time() - llm_start_time
            llm_generation_duration.observe(llm_duration)
            
            if not answer:
                logger.error("LLM 生成答案失败")
                error_total.labels(error_type="llm_generation_failed").inc()
                result = {
                    "answer": "抱歉，生成答案时发生错误，请稍后重试。",
                    "sources": self._convert_to_sources(retrieved_docs),
                    "query_time": time.time() - start_time
                }
                query_total.labels(status="llm_error").inc()
                return result
            
            # 5. 构建响应
            result = {
                "answer": answer,
                "sources": [s.model_dump() for s in self._convert_to_sources(retrieved_docs)],
                "query_time": time.time() - start_time
            }
            
            # 6. 缓存结果
            if use_cache:
                redis_cache.set_query_result(question, result)
            
            # 记录指标
            query_duration.observe(result["query_time"])
            query_total.labels(status="success").inc()
            
            logger.info(
                f"查询完成，耗时 {result['query_time']:.2f}秒 "
                f"(LLM: {llm_duration:.2f}秒)"
            )
            
            return result
            
        except Exception as e:
            logger.error(f"查询失败: {str(e)}")
            error_total.labels(error_type="query_failed").inc()
            query_total.labels(status="error").inc()
            
            return {
                "answer": f"抱歉，处理您的问题时发生错误: {str(e)}",
                "sources": [],
                "query_time": time.time() - start_time
            }
    
    async def query_stream(
        self,
        question: str,
        session_history: Optional[List[dict]] = None,
        top_k: Optional[int] = None,
        similarity_threshold: Optional[float] = None
    ) -> AsyncIterator[str]:
        """执行 RAG 查询（流式模式）
        
        Args:
            question: 用户问题
            session_history: 会话历史
            top_k: 返回的文档块数量
            similarity_threshold: 相似度阈值
            
        Yields:
            生成的文本片段
        """
        try:
            # 1. 向量检索
            logger.info(f"开始流式 RAG 查询: {question[:50]}...")
            retrieved_docs = await retriever_service.retrieve(
                question=question,
                top_k=top_k,
                similarity_threshold=similarity_threshold
            )
            
            if not retrieved_docs:
                logger.warning("未检索到相关文档")
                yield "抱歉，我在知识库中没有找到相关信息来回答您的问题。"
                return
            
            # 2. 构建提示词
            prompt = self._build_prompt(question, retrieved_docs, session_history)
            
            # 3. 流式生成答案
            async for chunk in llm_client.generate_stream(prompt):
                yield chunk
            
            query_total.labels(status="stream_success").inc()
            
        except Exception as e:
            logger.error(f"流式查询失败: {str(e)}")
            error_total.labels(error_type="stream_query_failed").inc()
            query_total.labels(status="stream_error").inc()
            yield f"错误：{str(e)}"


# 全局查询引擎实例
query_engine = QueryEngine()
