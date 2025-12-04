package com.rag.ops.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会话请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {
    
    /**
     * 用户ID（可选）
     */
    private String userId;
    
    /**
     * 元数据（可选）
     */
    private String metadata;
}
