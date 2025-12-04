package com.rag.ops.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处理文档响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDocumentResponse {
    
    private boolean success;
    private Integer chunkCount;
    private String message;
}
