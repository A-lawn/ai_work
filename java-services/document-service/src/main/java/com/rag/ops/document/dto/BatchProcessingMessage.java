package com.rag.ops.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 批量处理消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchProcessingMessage implements Serializable {
    
    /**
     * 批量任务 ID
     */
    private String taskId;
    
    /**
     * 文档 ID 列表
     */
    private List<String> documentIds;
    
    /**
     * 文档信息列表
     */
    private List<DocumentInfo> documents;
    
    /**
     * 文档信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentInfo implements Serializable {
        private String documentId;
        private String filename;
        private String filePath;
        private String fileType;
    }
}
