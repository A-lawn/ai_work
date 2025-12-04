package com.rag.ops.document.controller;

import com.rag.ops.document.dto.DocumentResponse;
import com.rag.ops.document.dto.DocumentUploadResponse;
import com.rag.ops.document.dto.PageResponse;
import com.rag.ops.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    private final DocumentService documentService;
    
    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Uploading document: {}", file.getOriginalFilename());
        DocumentUploadResponse response = documentService.uploadDocument(file);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取文档列表（分页）
     */
    @GetMapping
    public ResponseEntity<PageResponse<DocumentResponse>> getDocuments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        log.info("Getting documents: page={}, pageSize={}, status={}", page, pageSize, status);
        PageResponse<DocumentResponse> response = documentService.getDocuments(page, pageSize, status);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String id) {
        log.info("Getting document: {}", id);
        DocumentResponse response = documentService.getDocument(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) throws IOException {
        log.info("Deleting document: {}", id);
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 批量上传文档（ZIP 文件）
     */
    @PostMapping("/batch-upload")
    public ResponseEntity<com.rag.ops.document.dto.BatchUploadResponse> batchUploadDocuments(
            @RequestParam("file") MultipartFile zipFile) throws IOException {
        log.info("Batch uploading documents from ZIP: {}", zipFile.getOriginalFilename());
        com.rag.ops.document.dto.BatchUploadResponse response = documentService.batchUploadDocuments(zipFile);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 查询批量任务状态
     */
    @GetMapping("/batch-upload/{taskId}")
    public ResponseEntity<com.rag.ops.document.dto.BatchTaskStatusResponse> getBatchTaskStatus(
            @PathVariable String taskId) {
        log.info("Getting batch task status: {}", taskId);
        com.rag.ops.document.dto.BatchTaskStatusResponse response = documentService.getBatchTaskStatus(taskId);
        return ResponseEntity.ok(response);
    }
}
