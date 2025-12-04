package com.rag.ops.session.client;

import com.rag.ops.session.dto.QueryRequest;
import com.rag.ops.session.dto.QueryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * RAG 查询服务降级处理
 */
@Component
@Slf4j
public class RagQueryClientFallback implements RagQueryClient {
    
    @Override
    public QueryResponse query(QueryRequest request) {
        log.error("RAG 查询服务调用失败，执行降级处理，问题: {}", request.getQuestion());
        
        return QueryResponse.builder()
                .answer("抱歉，查询服务暂时不可用，请稍后重试。")
                .sources(new ArrayList<>())
                .queryTime(0.0)
                .build();
    }
}
