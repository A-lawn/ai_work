package com.rag.ops.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试LLM连接响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestLlmResponse {
    
    private Boolean success;
    private String message;
    private Long latency;
    private String response;
}
