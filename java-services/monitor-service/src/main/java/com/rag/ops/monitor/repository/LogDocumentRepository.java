package com.rag.ops.monitor.repository;

import com.rag.ops.monitor.document.LogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch Log Document Repository
 */
@Repository
public interface LogDocumentRepository extends ElasticsearchRepository<LogDocument, String> {

    /**
     * Find by operation type
     */
    Page<LogDocument> findByOperationType(String operationType, Pageable pageable);

    /**
     * Find by service name
     */
    Page<LogDocument> findByServiceName(String serviceName, Pageable pageable);

    /**
     * Find by user ID
     */
    Page<LogDocument> findByUserId(String userId, Pageable pageable);

    /**
     * Find by time range
     */
    Page<LogDocument> findByTimestampBetween(
        LocalDateTime startTime, 
        LocalDateTime endTime, 
        Pageable pageable
    );

    /**
     * Full text search in error messages
     */
    @Query("{\"match\": {\"errorMessage\": \"?0\"}}")
    Page<LogDocument> searchByErrorMessage(String keyword, Pageable pageable);

    /**
     * Search by trace ID
     */
    List<LogDocument> findByTraceId(String traceId);
}
