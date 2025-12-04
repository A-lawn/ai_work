"""
Prompt template management and construction
"""
from typing import List, Dict, Optional
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class PromptTemplate:
    """Prompt template for different scenarios"""
    
    # System prompt for RAG assistant
    SYSTEM_PROMPT = """你是一个专业的运维助手，专门帮助用户解决运维相关的问题。
你的回答应该：
1. 准确、专业、详细
2. 基于提供的上下文信息
3. 使用清晰的中文表达
4. 如果上下文中没有相关信息，请明确说明
5. 提供具体的操作步骤和建议"""
    
    # RAG query template with context
    RAG_QUERY_TEMPLATE = """基于以下上下文信息回答用户的问题。

上下文信息：
{context}

用户问题：{question}

请提供准确、详细的答案。如果上下文中没有相关信息，请明确说明。"""
    
    # Multi-turn conversation template
    CONVERSATION_TEMPLATE = """你是一个专业的运维助手。以下是对话历史：

{history}

用户问题：{question}

请基于对话历史和上下文回答用户的问题。"""
    
    # Simple query template (no context)
    SIMPLE_QUERY_TEMPLATE = """你是一个专业的运维助手。

用户问题：{question}

请提供准确、详细的答案。"""


class PromptBuilder:
    """Build prompts for LLM generation"""
    
    def __init__(self, max_context_tokens: int = None):
        """
        Initialize prompt builder
        
        Args:
            max_context_tokens: Maximum tokens for context
        """
        self.max_context_tokens = max_context_tokens or settings.MAX_CONTEXT_TOKENS
        self.template = PromptTemplate()
    
    def build_rag_prompt(
        self,
        question: str,
        context_chunks: List[str],
        max_chunks: int = 5
    ) -> str:
        """
        Build RAG prompt with context
        
        Args:
            question: User question
            context_chunks: List of context chunks
            max_chunks: Maximum number of chunks to include
            
        Returns:
            Formatted prompt
        """
        # Limit number of chunks
        chunks = context_chunks[:max_chunks]
        
        # Format context
        context = "\n\n".join([
            f"[文档片段 {i+1}]\n{chunk}"
            for i, chunk in enumerate(chunks)
        ])
        
        # Build prompt
        prompt = self.template.RAG_QUERY_TEMPLATE.format(
            context=context,
            question=question
        )
        
        # Check token limit
        prompt = self._truncate_to_token_limit(prompt)
        
        logger.info(f"Built RAG prompt with {len(chunks)} context chunks")
        return prompt
    
    def build_conversation_prompt(
        self,
        question: str,
        history: List[Dict[str, str]],
        context_chunks: Optional[List[str]] = None
    ) -> str:
        """
        Build conversation prompt with history
        
        Args:
            question: Current user question
            history: Conversation history [{"role": "user/assistant", "content": "..."}]
            context_chunks: Optional context chunks
            
        Returns:
            Formatted prompt
        """
        # Format history
        history_text = "\n\n".join([
            f"{'用户' if msg['role'] == 'user' else '助手'}: {msg['content']}"
            for msg in history[-10:]  # Keep last 10 messages
        ])
        
        # Build base prompt
        if context_chunks:
            # Include context
            context = "\n\n".join([
                f"[文档片段 {i+1}]\n{chunk}"
                for i, chunk in enumerate(context_chunks[:5])
            ])
            
            prompt = f"""基于以下上下文信息和对话历史回答用户的问题。

上下文信息：
{context}

对话历史：
{history_text}

用户问题：{question}

请提供准确、详细的答案。"""
        else:
            # No context, just history
            prompt = self.template.CONVERSATION_TEMPLATE.format(
                history=history_text,
                question=question
            )
        
        # Check token limit
        prompt = self._truncate_to_token_limit(prompt)
        
        logger.info(f"Built conversation prompt with {len(history)} history messages")
        return prompt
    
    def build_simple_prompt(self, question: str) -> str:
        """
        Build simple prompt without context
        
        Args:
            question: User question
            
        Returns:
            Formatted prompt
        """
        prompt = self.template.SIMPLE_QUERY_TEMPLATE.format(question=question)
        
        logger.info("Built simple prompt")
        return prompt
    
    def _truncate_to_token_limit(self, prompt: str) -> str:
        """
        Truncate prompt to token limit
        
        Args:
            prompt: Prompt to truncate
            
        Returns:
            Truncated prompt
        """
        # Rough estimate: 1 token ≈ 3 characters for Chinese
        # 1 token ≈ 4 characters for English
        # Use conservative estimate of 3 characters per token
        max_chars = self.max_context_tokens * 3
        
        if len(prompt) > max_chars:
            logger.warning(
                f"Prompt exceeds token limit ({len(prompt)} chars > {max_chars} chars), truncating"
            )
            # Truncate and add notice
            prompt = prompt[:max_chars] + "\n\n[注意：上下文已截断]"
        
        return prompt
    
    def estimate_tokens(self, text: str) -> int:
        """
        Estimate token count for text
        
        Args:
            text: Text to estimate
            
        Returns:
            Estimated token count
        """
        # Rough estimate for Chinese/English mixed text
        return len(text) // 3
    
    def validate_prompt_length(self, prompt: str) -> bool:
        """
        Validate if prompt is within token limit
        
        Args:
            prompt: Prompt to validate
            
        Returns:
            True if within limit, False otherwise
        """
        estimated_tokens = self.estimate_tokens(prompt)
        return estimated_tokens <= self.max_context_tokens


class TokenCounter:
    """Utility for counting tokens"""
    
    @staticmethod
    def count_tokens_rough(text: str) -> int:
        """
        Rough token count estimate
        
        Args:
            text: Text to count
            
        Returns:
            Estimated token count
        """
        # For Chinese: ~1.5 chars per token
        # For English: ~4 chars per token
        # Use conservative estimate
        chinese_chars = sum(1 for c in text if '\u4e00' <= c <= '\u9fff')
        other_chars = len(text) - chinese_chars
        
        chinese_tokens = chinese_chars / 1.5
        other_tokens = other_chars / 4
        
        return int(chinese_tokens + other_tokens)
    
    @staticmethod
    def ensure_token_limit(
        text: str,
        max_tokens: int,
        truncate_message: str = "[内容已截断]"
    ) -> str:
        """
        Ensure text is within token limit
        
        Args:
            text: Text to check
            max_tokens: Maximum tokens allowed
            truncate_message: Message to append if truncated
            
        Returns:
            Text within token limit
        """
        current_tokens = TokenCounter.count_tokens_rough(text)
        
        if current_tokens <= max_tokens:
            return text
        
        # Calculate how much to keep
        ratio = max_tokens / current_tokens
        keep_chars = int(len(text) * ratio * 0.9)  # Keep 90% to be safe
        
        return text[:keep_chars] + f"\n\n{truncate_message}"


# Global prompt builder instance
prompt_builder = PromptBuilder()
