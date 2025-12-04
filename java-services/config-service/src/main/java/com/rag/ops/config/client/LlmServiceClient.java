package com.rag.ops.config.client;

import com.rag.ops.config.dto.LlmGenerateRequest;
import com.rag.ops.config.dto.LlmGenerateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * LLM服务Feign客户端
 */
@FeignClient(
    name = "llm-service",
    fallback = LlmServiceClientFallback.class
)
public interface LlmServiceClient {
    
    /**
     * 调用LLM生成接口
     */
    @PostMapping("/api/generate")
    LlmGenerateResponse generate(@RequestBody LlmGenerateRequest request);
}
