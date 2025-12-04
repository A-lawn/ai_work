package com.rag.ops.config.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 配置请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigRequest {
    
    @NotBlank(message = "配置键不能为空")
    private String configKey;
    
    private String configValue;
    
    private String configType;
    
    private String description;
    
    private Boolean isActive;
}
