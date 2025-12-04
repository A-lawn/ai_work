package com.rag.ops.monitor.service;

import com.rag.ops.monitor.document.LogDocument;
import com.rag.ops.monitor.dto.*;
import com.rag.ops.monitor.entity.OperationLog;
import com.rag.ops.monitor.entity.PerformanceMetric;
import com.rag.ops.monitor.repository.LogDocumentRepository;
import com.rag.ops.monitor.repository.OperationLogRepository;
import com.rag.ops.monitor.repository.PerformanceMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Monitor Service
 * 监控日志服务核心业务逻辑
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitorService {

    private final OperationLogRepository operationLogRepository;
    private final PerformanceMetricRepository performanceMetricRepository;
    private final LogDocumentRepository logDocumentRepository;

    /**
     * 收集操作日志
     */
    @Async
    @Transactional
    public void collectLog(LogRequest request) {
        try {
            // 保存到 PostgreSQL
            OperationLog operationLog = OperationLog.builder()
                    .operationType(request.getOperationType())
                    .serviceName(request.getServiceName())
                    .userId(request.getUserId())
                    .resourceId(request.getResourceId())
                    .resourceType(request.getResourceType())
                    .action(request.getAction())
                    .status(request.getStatus())
                    .errorMessage(request.getErrorMessage())
                    .ipAddress(request.getIpAddress())
                    .userAgent(request.getUserAgent())
                    .details(request.getDetails())
                    .durationMs(request.getDurationMs())
                    .traceId(request.getTraceId())
                    .spanId(request.getSpanId())
                    .timestamp(LocalDateTime.now())
                    .build();

            OperationLog savedLog = operationLogRepository.save(operationLog);

            // 异步保存到 Elasticsearch
            saveToElasticsearch(savedLog);

            log.debug("Log collected: {} - {}", request.getOperationType(), request.getServiceName());
        } catch (Exception e) {
            log.error("Failed to collect log: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存日志到 Elasticsearch
     */
    @Async
    public void saveToElasticsearch(OperationLog operationLog) {
        try {
            LogDocument logDocument = LogDocument.builder()
                    .id(operationLog.getId().toString())
                    .operationType(operationLog.getOperationType())
                    .serviceName(operationLog.getServiceName())
                    .userId(operationLog.getUserId())
                    .resourceId(operationLog.getResourceId())
                    .resourceType(operationLog.getResourceType())
                    .action(operationLog.getAction())
                    .status(operationLog.getStatus())
                    .errorMessage(operationLog.getErrorMessage())
                    .ipAddress(operationLog.getIpAddress())
                    .userAgent(operationLog.getUserAgent())
                    .details(operationLog.getDetails())
                    .timestamp(operationLog.getTimestamp())
                    .durationMs(operationLog.getDurationMs())
                    .traceId(operationLog.getTraceId())
                    .spanId(operationLog.getSpanId())
                    .build();

            logDocumentRepository.save(logDocument);
            log.debug("Log saved to Elasticsearch: {}", operationLog.getId());
        } catch (Exception e) {
            log.error("Failed to save log to Elasticsearch: {}", e.getMessage(), e);
        }
    }

    /**
     * 收集性能指标
     */
    @Async
    @Transactional
    public void collectMetric(MetricRequest request) {
        try {
            PerformanceMetric metric = PerformanceMetric.builder()
                    .metricType(request.getMetricType())
                    .serviceName(request.getServiceName())
                    .metricName(request.getMetricName())
                    .metricValue(request.getMetricValue())
                    .unit(request.getUnit())
                    .metadata(request.getMetadata())
                    .tags(request.getTags())
                    .timestamp(LocalDateTime.now())
                    .build();

            performanceMetricRepository.save(metric);
            log.debug("Metric collected: {} - {} = {}", 
                request.getServiceName(), request.getMetricName(), request.getMetricValue());
        } catch (Exception e) {
            log.error("Failed to collect metric: {}", e.getMessage(), e);
        }
    }

    /**
     * 查询日志
     */
    @Transactional(readOnly = true)
    public Page<LogResponse> queryLogs(
            String operationType,
            String serviceName,
            String userId,
            String status,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size
    ) {
        // 设置默认时间范围（最近24小时）
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(1);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<OperationLog> logs = operationLogRepository.findByFilters(
                operationType, serviceName, userId, status, startTime, endTime, pageable
        );

        return logs.map(this::convertToLogResponse);
    }

    /**
     * 查询性能指标
     */
    @Transactional(readOnly = true)
    public Page<MetricResponse> queryMetrics(
            String metricType,
            String serviceName,
            String metricName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size
    ) {
        // 设置默认时间范围（最近1小时）
        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(1);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<PerformanceMetric> metrics = performanceMetricRepository.findByFilters(
                metricType, serviceName, metricName, startTime, endTime, pageable
        );

        return metrics.map(this::convertToMetricResponse);
    }

    /**
     * 获取系统统计信息
     */
    @Transactional(readOnly = true)
    public StatsResponse getSystemStats(LocalDateTime startTime, LocalDateTime endTime) {
        // 设置默认时间范围（最近24小时）
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(1);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        // 统计日志数量
        Long totalLogs = operationLogRepository.count();

        // 按类型统计日志
        List<Object[]> logsByTypeList = operationLogRepository.countByOperationType(startTime, endTime);
        Map<String, Long> logsByType = new HashMap<>();
        for (Object[] row : logsByTypeList) {
            logsByType.put((String) row[0], (Long) row[1]);
        }

        // 按状态统计日志
        List<Object[]> logsByStatusList = operationLogRepository.countByStatus(startTime, endTime);
        Map<String, Long> logsByStatus = new HashMap<>();
        Long errorCount = 0L;
        for (Object[] row : logsByStatusList) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            logsByStatus.put(status, count);
            if ("ERROR".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
                errorCount += count;
            }
        }

        // 计算错误率
        Long totalRequests = logsByStatus.values().stream().mapToLong(Long::longValue).sum();
        Double errorRate = totalRequests > 0 ? (errorCount.doubleValue() / totalRequests) * 100 : 0.0;

        // 统计性能指标
        Long totalMetrics = performanceMetricRepository.count();

        // 计算平均响应时间
        Double avgResponseTime = performanceMetricRepository.calculateAverage(
                "response_time", null, "api_response_time", startTime, endTime
        );

        // 计算最大响应时间
        Double maxResponseTime = performanceMetricRepository.calculateMax(
                "response_time", null, "api_response_time", startTime, endTime
        );

        return StatsResponse.builder()
                .totalLogs(totalLogs)
                .totalMetrics(totalMetrics)
                .logsByType(logsByType)
                .logsByStatus(logsByStatus)
                .avgResponseTime(avgResponseTime != null ? avgResponseTime : 0.0)
                .maxResponseTime(maxResponseTime != null ? maxResponseTime : 0.0)
                .errorCount(errorCount)
                .errorRate(errorRate)
                .build();
    }

    /**
     * 搜索日志（使用 Elasticsearch）
     */
    @Transactional(readOnly = true)
    public Page<LogResponse> searchLogs(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LogDocument> documents = logDocumentRepository.searchByErrorMessage(keyword, pageable);
        return documents.map(this::convertDocumentToResponse);
    }

    /**
     * 根据 Trace ID 查询日志
     */
    @Transactional(readOnly = true)
    public List<LogResponse> getLogsByTraceId(String traceId) {
        List<LogDocument> documents = logDocumentRepository.findByTraceId(traceId);
        return documents.stream()
                .map(this::convertDocumentToResponse)
                .collect(Collectors.toList());
    }

    // 转换方法
    private LogResponse convertToLogResponse(OperationLog log) {
        return LogResponse.builder()
                .id(log.getId().toString())
                .operationType(log.getOperationType())
                .serviceName(log.getServiceName())
                .userId(log.getUserId())
                .resourceId(log.getResourceId())
                .resourceType(log.getResourceType())
                .action(log.getAction())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .details(log.getDetails())
                .timestamp(log.getTimestamp())
                .durationMs(log.getDurationMs())
                .traceId(log.getTraceId())
                .spanId(log.getSpanId())
                .build();
    }

    private MetricResponse convertToMetricResponse(PerformanceMetric metric) {
        return MetricResponse.builder()
                .id(metric.getId().toString())
                .metricType(metric.getMetricType())
                .serviceName(metric.getServiceName())
                .metricName(metric.getMetricName())
                .metricValue(metric.getMetricValue())
                .unit(metric.getUnit())
                .timestamp(metric.getTimestamp())
                .metadata(metric.getMetadata())
                .tags(metric.getTags())
                .build();
    }

    private LogResponse convertDocumentToResponse(LogDocument doc) {
        return LogResponse.builder()
                .id(doc.getId())
                .operationType(doc.getOperationType())
                .serviceName(doc.getServiceName())
                .userId(doc.getUserId())
                .resourceId(doc.getResourceId())
                .resourceType(doc.getResourceType())
                .action(doc.getAction())
                .status(doc.getStatus())
                .errorMessage(doc.getErrorMessage())
                .ipAddress(doc.getIpAddress())
                .userAgent(doc.getUserAgent())
                .details(doc.getDetails())
                .timestamp(doc.getTimestamp())
                .durationMs(doc.getDurationMs())
                .traceId(doc.getTraceId())
                .spanId(doc.getSpanId())
                .build();
    }
}
