package com.rag.ops.document.repository;

import com.rag.ops.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 文档仓储接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {
    
    /**
     * 根据状态分页查询文档
     */
    Page<Document> findByStatus(String status, Pageable pageable);
    
    /**
     * 根据文件名查询文档
     */
    Optional<Document> findByFilename(String filename);
    
    /**
     * 统计指定状态的文档数量
     */
    long countByStatus(String status);
}
