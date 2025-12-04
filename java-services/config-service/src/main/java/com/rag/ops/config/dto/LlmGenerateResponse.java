package com.rag.ops.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM生成响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmGenerateResponse {
    
    private Boolean success;
    private String response;
    private String message;
    private Integer tokensUsed;
}
