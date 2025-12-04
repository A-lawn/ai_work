package com.rag.ops.document.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_filename", columnList = "filename"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_upload_time", columnList = "upload_time")
})
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, length = 255)
    private String filename;
    
    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;
    
    @Column(name = "chunk_count")
    @Builder.Default
    private Integer chunkCount = 0;
    
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PROCESSING";
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 文档状态枚举
     */
    public enum Status {
        PROCESSING("处理中"),
        COMPLETED("已完成"),
        FAILED("失败");
        
        private final String description;
        
        Status(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
