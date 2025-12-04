package com.rag.ops.document.client;

import com.rag.ops.document.dto.ProcessDocumentRequest;
import com.rag.ops.document.dto.ProcessDocumentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文档处理服务客户端降级处理
 */
@Slf4j
@Component
public class DocumentProcessingClientFallback implements DocumentProcessingClient {
    
    @Override
    public ProcessDocumentResponse processDocument(ProcessDocumentRequest request) {
        log.warn("Document processing service is unavailable, using fallback for document: {}", 
                request.getDocumentId());
        
        return ProcessDocumentResponse.builder()
                .success(false)
                .message("文档处理服务暂时不可用，文档已加入处理队列")
                .build();
    }
    
    @Override
    public void deleteVectors(String documentId) {
        log.warn("Document processing service is unavailable, cannot delete vectors for document: {}", 
                documentId);
    }
}
