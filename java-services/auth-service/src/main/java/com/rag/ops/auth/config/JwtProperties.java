package com.rag.ops.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    /**
     * JWT 密钥
     */
    private String secret;
    
    /**
     * Token 过期时间（毫秒）
     */
    private Long expiration;
    
    /**
     * Refresh Token 过期时间（毫秒）
     */
    private Long refreshExpiration;
    
    /**
     * Token 请求头名称
     */
    private String header = "Authorization";
    
    /**
     * Token 前缀
     */
    private String prefix = "Bearer ";
}
