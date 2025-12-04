package com.rag.ops.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量上传响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {
    
    /**
     * 批量任务 ID
     */
    private String taskId;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 文档数量
     */
    private Integer documentCount;
    
    /**
     * 文档 ID 列表
     */
    private List<String> documentIds;
    
    /**
     * 消息
     */
    private String message;
}
