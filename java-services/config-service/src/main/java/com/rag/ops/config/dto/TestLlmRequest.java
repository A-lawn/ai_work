package com.rag.ops.config.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试LLM连接请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestLlmRequest {
    
    @NotBlank(message = "LLM提供商不能为空")
    private String provider;
    
    private String apiKey;
    
    private String endpoint;
    
    private String model;
    
    private String testPrompt;
}
