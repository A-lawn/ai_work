package com.rag.ops.config.client;

import com.rag.ops.config.dto.LlmGenerateRequest;
import com.rag.ops.config.dto.LlmGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LLM服务Feign客户端降级处理
 */
@Slf4j
@Component
public class LlmServiceClientFallback implements LlmServiceClient {
    
    @Override
    public LlmGenerateResponse generate(LlmGenerateRequest request) {
        log.error("LLM服务调用失败，执行降级逻辑");
        return LlmGenerateResponse.builder()
                .success(false)
                .message("LLM服务暂时不可用，请稍后重试")
                .build();
    }
}
