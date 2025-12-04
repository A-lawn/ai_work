package com.rag.ops.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 处理文档请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDocumentRequest {
    
    private String documentId;
    private String filePath;
    private String fileType;
}
