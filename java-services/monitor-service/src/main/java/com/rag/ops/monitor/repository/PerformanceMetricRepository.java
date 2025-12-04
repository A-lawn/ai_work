package com.rag.ops.monitor.repository;

import com.rag.ops.monitor.entity.PerformanceMetric;
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
 * Performance Metric Repository
 */
@Repository
public interface PerformanceMetricRepository extends JpaRepository<PerformanceMetric, UUID> {

    /**
     * Find metrics by type
     */
    Page<PerformanceMetric> findByMetricType(String metricType, Pageable pageable);

    /**
     * Find metrics by service name
     */
    Page<PerformanceMetric> findByServiceName(String serviceName, Pageable pageable);

    /**
     * Find metrics by time range
     */
    Page<PerformanceMetric> findByTimestampBetween(
        LocalDateTime startTime, 
        LocalDateTime endTime, 
        Pageable pageable
    );

    /**
     * Find metrics by filters
     */
    @Query("SELECT pm FROM PerformanceMetric pm WHERE " +
           "(:metricType IS NULL OR pm.metricType = :metricType) AND " +
           "(:serviceName IS NULL OR pm.serviceName = :serviceName) AND " +
           "(:metricName IS NULL OR pm.metricName = :metricName) AND " +
           "pm.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY pm.timestamp DESC")
    Page<PerformanceMetric> findByFilters(
        @Param("metricType") String metricType,
        @Param("serviceName") String serviceName,
        @Param("metricName") String metricName,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );

    /**
     * Calculate average metric value
     */
    @Query("SELECT AVG(pm.metricValue) FROM PerformanceMetric pm WHERE " +
           "pm.metricType = :metricType AND " +
           "pm.serviceName = :serviceName AND " +
           "pm.metricName = :metricName AND " +
           "pm.timestamp BETWEEN :startTime AND :endTime")
    Double calculateAverage(
        @Param("metricType") String metricType,
        @Param("serviceName") String serviceName,
        @Param("metricName") String metricName,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Calculate max metric value
     */
    @Query("SELECT MAX(pm.metricValue) FROM PerformanceMetric pm WHERE " +
           "pm.metricType = :metricType AND " +
           "pm.serviceName = :serviceName AND " +
           "pm.metricName = :metricName AND " +
           "pm.timestamp BETWEEN :startTime AND :endTime")
    Double calculateMax(
        @Param("metricType") String metricType,
        @Param("serviceName") String serviceName,
        @Param("metricName") String metricName,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Delete old metrics
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
