package com.rag.ops.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 批量任务状态响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchTaskStatusResponse {
    
    /**
     * 任务 ID
     */
    private String taskId;
    
    /**
     * 状态: PENDING, PROCESSING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED
     */
    private String status;
    
    /**
     * 总文档数
     */
    private Integer total;
    
    /**
     * 已处理数
     */
    private Integer processed;
    
    /**
     * 成功数
     */
    private Integer success;
    
    /**
     * 失败数
     */
    private Integer failed;
    
    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;
    
    /**
     * 处理结果列表
     */
    private List<Map<String, Object>> results;
    
    /**
     * 摘要信息
     */
    private String summary;
}
