package com.rag.ops.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionQueryResponse {
    
    /**
     * 会话 ID
     */
    private String sessionId;
    
    /**
     * 生成的答案
     */
    private String answer;
    
    /**
     * 引用来源列表
     */
    private List<QueryResponse.Source> sources;
    
    /**
     * 查询耗时（秒）
     */
    private Double queryTime;
}
