package com.rag.ops.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * API Key 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "api-key")
public class ApiKeyProperties {
    
    /**
     * 默认过期天数
     */
    private Integer defaultExpirationDays = 365;
    
    /**
     * 哈希算法
     */
    private String hashAlgorithm = "SHA-256";
}
