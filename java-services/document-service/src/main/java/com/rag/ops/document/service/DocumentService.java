package com.rag.ops.document.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.rag.ops.document.client.DocumentProcessingClient;
import com.rag.ops.document.config.RabbitMQConfig;
import com.rag.ops.document.dto.BatchProcessingMessage;
import com.rag.ops.document.dto.BatchTaskStatusResponse;
import com.rag.ops.document.dto.BatchUploadResponse;
import com.rag.ops.document.dto.DocumentProcessingMessage;
import com.rag.ops.document.dto.DocumentResponse;
import com.rag.ops.document.dto.DocumentUploadResponse;
import com.rag.ops.document.dto.PageResponse;
import com.rag.ops.document.dto.ProcessDocumentRequest;
import com.rag.ops.document.dto.ProcessDocumentResponse;
import com.rag.ops.document.entity.Document;
import com.rag.ops.document.exception.DocumentNotFoundException;
import com.rag.ops.document.exception.InvalidFileException;
import com.rag.ops.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文档服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final RabbitTemplate rabbitTemplate;
    private final DocumentProcessingClient documentProcessingClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("pdf", "docx", "txt", "md");
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    
    /**
     * 上传文档
     */
    @Transactional
    @SentinelResource(
        value = "uploadDocument",
        blockHandler = "uploadDocumentBlockHandler",
        fallback = "uploadDocumentFallback"
    )
    public DocumentUploadResponse uploadDocument(MultipartFile file) throws IOException {
        // 验证文件
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String fileType = getFileExtension(originalFilename);
        
        // 上传文件到 MinIO
        String filePath = minioService.uploadFile(file);
        
        // 保存文档元数据
        Document document = Document.builder()
                .filename(originalFilename)
                .fileType(fileType)
                .fileSize(file.getSize())
                .filePath(filePath)
                .uploadTime(LocalDateTime.now())
                .status(Document.Status.PROCESSING.name())
                .build();
        
        document = documentRepository.save(document);
        log.info("Document saved to database: {}", document.getId());
        
        // 发送消息到 RabbitMQ 触发文档处理
        DocumentProcessingMessage message = DocumentProcessingMessage.builder()
                .documentId(document.getId())
                .filename(originalFilename)
                .filePath(filePath)
                .fileType(fileType)
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_PROCESSING_EXCHANGE,
                RabbitMQConfig.DOCUMENT_PROCESSING_ROUTING_KEY,
                message
        );
        log.info("Document processing message sent for document: {}", document.getId());
        
        return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .filename(originalFilename)
                .status(document.getStatus())
                .message("文档上传成功，正在处理中")
                .fileSize(file.getSize())
                .build();
    }
    
    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new InvalidFileException("文件名不能为空");
        }
        
        String fileType = getFileExtension(originalFilename);
        if (!SUPPORTED_FORMATS.contains(fileType.toLowerCase())) {
            throw new InvalidFileException("不支持的文件格式: " + fileType + 
                    "。支持的格式: " + String.join(", ", SUPPORTED_FORMATS));
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("文件大小超过限制: " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * 获取文档列表（分页）
     */
    public PageResponse<DocumentResponse> getDocuments(int page, int pageSize, String status) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "uploadTime"));
        
        Page<Document> documentPage;
        if (status != null && !status.isEmpty()) {
            documentPage = documentRepository.findByStatus(status, pageable);
        } else {
            documentPage = documentRepository.findAll(pageable);
        }
        
        List<DocumentResponse> content = documentPage.getContent().stream()
                .map(this::convertToResponse)
                .toList();
        
        return PageResponse.<DocumentResponse>builder()
                .content(content)
                .page(page)
                .pageSize(pageSize)
                .total(documentPage.getTotalElements())
                .totalPages(documentPage.getTotalPages())
                .build();
    }
    
    /**
     * 获取文档详情
     */
    public DocumentResponse getDocument(String id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("文档不存在: " + id));
        return convertToResponse(document);
    }
    
    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(String id) throws IOException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("文档不存在: " + id));
        
        // 删除向量数据库中的向量
        try {
            documentProcessingClient.deleteVectors(id);
            log.info("Vectors deleted for document: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete vectors for document: {}", id, e);
            // 继续删除其他数据
        }
        
        // 删除 MinIO 中的文件
        if (document.getFilePath() != null) {
            try {
                minioService.deleteFile(document.getFilePath());
                log.info("File deleted from MinIO: {}", document.getFilePath());
            } catch (IOException e) {
                log.error("Failed to delete file from MinIO: {}", document.getFilePath(), e);
                // 继续删除数据库记录
            }
        }
        
        // 删除数据库记录
        documentRepository.delete(document);
        log.info("Document deleted from database: {}", id);
    }
    
    /**
     * 更新文档状态
     */
    @Transactional
    public void updateDocumentStatus(String id, String status, Integer chunkCount, String errorMessage) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("文档不存在: " + id));
        
        document.setStatus(status);
        if (chunkCount != null) {
            document.setChunkCount(chunkCount);
        }
        if (errorMessage != null) {
            document.setErrorMessage(errorMessage);
        }
        
        documentRepository.save(document);
        log.info("Document status updated: {} -> {}", id, status);
    }
    
    /**
     * 转换为响应 DTO
     */
    private DocumentResponse convertToResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .filename(document.getFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .uploadTime(document.getUploadTime())
                .chunkCount(document.getChunkCount())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
    
    /**
     * 上传文档限流处理
     */
    public DocumentUploadResponse uploadDocumentBlockHandler(MultipartFile file, BlockException e) {
        log.warn("Upload document blocked: {}", e.getMessage());
        return DocumentUploadResponse.builder()
                .status("BLOCKED")
                .message("系统繁忙，请稍后重试")
                .build();
    }
    
    /**
     * 上传文档降级处理
     */
    public DocumentUploadResponse uploadDocumentFallback(MultipartFile file, Throwable e) {
        log.error("Upload document fallback: {}", e.getMessage());
        return DocumentUploadResponse.builder()
                .status("FAILED")
                .message("文档上传失败: " + e.getMessage())
                .build();
    }
    
    /**
     * 批量上传文档（ZIP 文件）
     */
    @Transactional
    @SentinelResource(
        value = "batchUploadDocuments",
        blockHandler = "batchUploadBlockHandler",
        fallback = "batchUploadFallback"
    )
    public BatchUploadResponse batchUploadDocuments(MultipartFile zipFile) throws IOException {
        // 验证 ZIP 文件
        if (zipFile == null || zipFile.isEmpty()) {
            throw new InvalidFileException("ZIP 文件不能为空");
        }
        
        String originalFilename = zipFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new InvalidFileException("只支持 ZIP 格式的压缩文件");
        }
        
        log.info("Processing batch upload: {}", originalFilename);
        
        // 生成批量任务 ID
        String taskId = UUID.randomUUID().toString();
        
        List<String> documentIds = new ArrayList<>();
        List<BatchProcessingMessage.DocumentInfo> documentInfos = new ArrayList<>();
        
        // 解压并处理 ZIP 文件
        try (InputStream inputStream = zipFile.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                // 跳过目录
                if (entry.isDirectory()) {
                    continue;
                }
                
                String entryName = entry.getName();
                // 跳过隐藏文件和系统文件
                if (entryName.startsWith(".") || entryName.startsWith("__MACOSX")) {
                    continue;
                }
                
                // 获取文件名（去除路径）
                String filename = entryName.contains("/") 
                    ? entryName.substring(entryName.lastIndexOf("/") + 1) 
                    : entryName;
                
                // 验证文件格式
                String fileType = getFileExtension(filename);
                if (!SUPPORTED_FORMATS.contains(fileType.toLowerCase())) {
                    log.warn("Skipping unsupported file: {}", filename);
                    continue;
                }
                
                try {
                    // 读取文件内容
                    byte[] fileContent = zipInputStream.readAllBytes();
                    
                    // 上传到 MinIO
                    String filePath = minioService.uploadFile(filename, fileContent);
                    
                    // 保存文档元数据
                    Document document = Document.builder()
                            .filename(filename)
                            .fileType(fileType)
                            .fileSize((long) fileContent.length)
                            .filePath(filePath)
                            .uploadTime(LocalDateTime.now())
                            .status(Document.Status.PROCESSING.name())
                            .metadata("{\"batch_task_id\":\"" + taskId + "\"}")
                            .build();
                    
                    document = documentRepository.save(document);
                    log.info("Document saved: {} ({})", document.getId(), filename);
                    
                    documentIds.add(document.getId());
                    documentInfos.add(BatchProcessingMessage.DocumentInfo.builder()
                            .documentId(document.getId())
                            .filename(filename)
                            .filePath(filePath)
                            .fileType(fileType)
                            .build());
                    
                } catch (Exception e) {
                    log.error("Failed to process file in ZIP: {}", filename, e);
                    // 继续处理其他文件
                }
                
                zipInputStream.closeEntry();
            }
        }
        
        if (documentIds.isEmpty()) {
            throw new InvalidFileException("ZIP 文件中没有找到支持的文档");
        }
        
        log.info("Extracted {} documents from ZIP file", documentIds.size());
        
        // 发送批量处理消息到 RabbitMQ
        BatchProcessingMessage message = BatchProcessingMessage.builder()
                .taskId(taskId)
                .documentIds(documentIds)
                .documents(documentInfos)
                .build();
        
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_PROCESSING_EXCHANGE,
                "document.batch.process",  // 批量处理路由键
                message
        );
        log.info("Batch processing message sent for task: {}", taskId);
        
        return BatchUploadResponse.builder()
                .taskId(taskId)
                .status("PENDING")
                .documentCount(documentIds.size())
                .documentIds(documentIds)
                .message("批量上传成功，共 " + documentIds.size() + " 个文档，正在处理中")
                .build();
    }
    
    /**
     * 批量上传限流处理
     */
    public BatchUploadResponse batchUploadBlockHandler(MultipartFile zipFile, BlockException e) {
        log.warn("Batch upload blocked: {}", e.getMessage());
        return BatchUploadResponse.builder()
                .status("BLOCKED")
                .message("系统繁忙，请稍后重试")
                .build();
    }
    
    /**
     * 批量上传降级处理
     */
    public BatchUploadResponse batchUploadFallback(MultipartFile zipFile, Throwable e) {
        log.error("Batch upload fallback: {}", e.getMessage());
        return BatchUploadResponse.builder()
                .status("FAILED")
                .message("批量上传失败: " + e.getMessage())
                .build();
    }
    
    /**
     * 查询批量任务状态
     */
    public BatchTaskStatusResponse getBatchTaskStatus(String taskId) {
        try {
            // 从 Redis 获取任务进度
            String key = "batch_task:" + taskId;
            String progressJson = redisTemplate.opsForValue().get(key);
            
            if (progressJson == null || progressJson.isEmpty()) {
                // 任务不存在或已过期
                return BatchTaskStatusResponse.builder()
                        .taskId(taskId)
                        .status("NOT_FOUND")
                        .summary("任务不存在或已过期")
                        .build();
            }
            
            // 解析 JSON
            @SuppressWarnings("unchecked")
            Map<String, Object> progressData = objectMapper.readValue(progressJson, Map.class);
            
            return BatchTaskStatusResponse.builder()
                    .taskId(taskId)
                    .status((String) progressData.get("status"))
                    .total((Integer) progressData.get("total"))
                    .processed((Integer) progressData.get("processed"))
                    .success((Integer) progressData.get("success"))
                    .failed((Integer) progressData.get("failed"))
                    .progress((Integer) progressData.get("progress"))
                    .results((List<Map<String, Object>>) progressData.get("results"))
                    .summary((String) progressData.get("summary"))
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get batch task status: {}", taskId, e);
            return BatchTaskStatusResponse.builder()
                    .taskId(taskId)
                    .status("ERROR")
                    .summary("查询任务状态失败: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 调用文档处理服务（带 Sentinel 保护）
     */
    @SentinelResource(
        value = "processDocument",
        blockHandler = "processDocumentBlockHandler",
        fallback = "processDocumentFallback"
    )
    public ProcessDocumentResponse callDocumentProcessingService(ProcessDocumentRequest request) {
        return documentProcessingClient.processDocument(request);
    }
    
    /**
     * 文档处理限流处理
     */
    public ProcessDocumentResponse processDocumentBlockHandler(
            ProcessDocumentRequest request, BlockException e) {
        log.warn("Process document blocked: {}", e.getMessage());
        return ProcessDocumentResponse.builder()
                .success(false)
                .message("系统繁忙，文档已加入处理队列")
                .build();
    }
    
    /**
     * 文档处理降级处理
     */
    public ProcessDocumentResponse processDocumentFallback(
            ProcessDocumentRequest request, Throwable e) {
        log.error("Process document fallback: {}", e.getMessage());
        return ProcessDocumentResponse.builder()
                .success(false)
                .message("文档处理服务暂时不可用，文档已加入处理队列")
                .build();
    }
}
