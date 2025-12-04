package com.rag.ops.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API Key 创建请求
 */
@Data
public class ApiKeyRequest {
    
    @NotBlank(message = "名称不能为空")
    private String name;
    
    private String description;
    
    private Integer expirationDays;
}
