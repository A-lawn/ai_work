package com.rag.ops.session.client;

import com.rag.ops.session.dto.QueryRequest;
import com.rag.ops.session.dto.QueryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * RAG 查询服务 Feign 客户端
 */
@FeignClient(
    name = "rag-query-service",
    path = "/api",
    fallback = RagQueryClientFallback.class
)
public interface RagQueryClient {
    
    /**
     * 执行 RAG 查询
     * 
     * @param request 查询请求
     * @return 查询响应
     */
    @PostMapping("/query")
    QueryResponse query(@RequestBody QueryRequest request);
}
