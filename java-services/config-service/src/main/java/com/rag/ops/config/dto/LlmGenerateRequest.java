package com.rag.ops.config.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM生成请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmGenerateRequest {
    
    private String prompt;
    private String provider;
    private String apiKey;
    private String endpoint;
    private String model;
    private Double temperature;
    private Integer maxTokens;
}
