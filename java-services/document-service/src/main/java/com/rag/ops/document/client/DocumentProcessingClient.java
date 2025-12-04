package com.rag.ops.document.client;

import com.rag.ops.document.dto.ProcessDocumentRequest;
import com.rag.ops.document.dto.ProcessDocumentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 文档处理服务客户端
 */
@FeignClient(
    name = "document-processing-service",
    fallback = DocumentProcessingClientFallback.class
)
public interface DocumentProcessingClient {
    
    /**
     * 处理文档
     */
    @PostMapping("/api/process-document")
    ProcessDocumentResponse processDocument(@RequestBody ProcessDocumentRequest request);
    
    /**
     * 删除文档向量
     */
    @DeleteMapping("/api/vectors/{documentId}")
    void deleteVectors(@PathVariable("documentId") String documentId);
}
