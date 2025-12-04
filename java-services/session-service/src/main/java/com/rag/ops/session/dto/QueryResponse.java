package com.rag.ops.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {
    
    /**
     * 生成的答案
     */
    private String answer;
    
    /**
     * 引用来源列表
     */
    private List<Source> sources;
    
    /**
     * 查询耗时（秒）
     */
    private Double queryTime;
    
    /**
     * 引用来源
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        /**
         * 文档块文本
         */
        private String chunkText;
        
        /**
         * 相似度分数
         */
        private Double similarityScore;
        
        /**
         * 文档 ID
         */
        private String documentId;
        
        /**
         * 文档名称
         */
        private String documentName;
        
        /**
         * 文档块索引
         */
        private Integer chunkIndex;
        
        /**
         * 页码
         */
        private Integer pageNumber;
        
        /**
         * 章节
         */
        private String section;
    }
}
