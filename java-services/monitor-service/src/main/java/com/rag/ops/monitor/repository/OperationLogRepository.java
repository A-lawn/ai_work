package com.rag.ops.monitor.repository;

import com.rag.ops.monitor.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Operation Log Repository
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, UUID> {

    /**
     * Find logs by operation type
     */
    Page<OperationLog> findByOperationType(String operationType, Pageable pageable);

    /**
     * Find logs by user ID
     */
    Page<OperationLog> findByUserId(String userId, Pageable pageable);

    /**
     * Find logs by service name
     */
    Page<OperationLog> findByServiceName(String serviceName, Pageable pageable);

    /**
     * Find logs by time range
     */
    Page<OperationLog> findByTimestampBetween(
        LocalDateTime startTime, 
        LocalDateTime endTime, 
        Pageable pageable
    );

    /**
     * Find logs by operation type and time range
     */
    @Query("SELECT ol FROM OperationLog ol WHERE " +
           "(:operationType IS NULL OR ol.operationType = :operationType) AND " +
           "(:serviceName IS NULL OR ol.serviceName = :serviceName) AND " +
           "(:userId IS NULL OR ol.userId = :userId) AND " +
           "(:status IS NULL OR ol.status = :status) AND " +
           "ol.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY ol.timestamp DESC")
    Page<OperationLog> findByFilters(
        @Param("operationType") String operationType,
        @Param("serviceName") String serviceName,
        @Param("userId") String userId,
        @Param("status") String status,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * Count logs by operation type
     */
    @Query("SELECT ol.operationType, COUNT(ol) FROM OperationLog ol " +
           "WHERE ol.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY ol.operationType")
    List<Object[]> countByOperationType(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Count logs by status
     */
    @Query("SELECT ol.status, COUNT(ol) FROM OperationLog ol " +
           "WHERE ol.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY ol.status")
    List<Object[]> countByStatus(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Delete old logs
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
