package com.rag.ops.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAG 查询请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {
    
    /**
     * 用户问题
     */
    private String question;
    
    /**
     * 会话历史（可选）
     */
    private List<Map<String, String>> sessionHistory;
    
    /**
     * 返回的文档块数量（可选）
     */
    private Integer topK;
    
    /**
     * 相似度阈值（可选）
     */
    private Double similarityThreshold;
    
    /**
     * 是否使用流式响应
     */
    private Boolean stream;
}
