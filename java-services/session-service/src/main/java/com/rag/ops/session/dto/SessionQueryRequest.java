package com.rag.ops.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话查询请求
 * 用于前端调用会话服务进行查询
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionQueryRequest {
    
    /**
     * 用户问题
     */
    private String question;
    
    /**
     * 会话 ID（可选，如果不提供则创建新会话）
     */
    private String sessionId;
    
    /**
     * 用户 ID（可选）
     */
    private String userId;
    
    /**
     * 返回的文档块数量（可选）
     */
    private Integer topK;
    
    /**
     * 相似度阈值（可选）
     */
    private Double similarityThreshold;
}
