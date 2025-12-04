package com.rag.ops.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 文档处理消息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentProcessingMessage implements Serializable {
    
    private String documentId;
    private String filename;
    private String filePath;
    private String fileType;
}
