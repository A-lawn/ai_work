package com.rag.ops.monitor.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.rag.ops.monitor.dto.*;
import com.rag.ops.monitor.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Monitor Controller
 * 监控日志服务 REST API
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class MonitorController {

    private final MonitorService monitorService;

    /**
     * 收集操作日志
     * POST /api/v1/logs
     */
    @PostMapping("/logs")
    @SentinelResource(value = "collectLog")
    public ResponseEntity<Map<String, String>> collectLog(@RequestBody LogRequest request) {
        log.info("Collecting log: {} - {}", request.getOperationType(), request.getServiceName());
        monitorService.collectLog(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Log collected successfully"
        ));
    }

    /**
     * 查询操作日志
     * GET /api/v1/logs
     */
    @GetMapping("/logs")
    @SentinelResource(value = "queryLogs")
    public ResponseEntity<Page<LogResponse>> queryLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Querying logs: operationType={}, serviceName={}, page={}, size={}", 
                operationType, serviceName, page, size);
        
        Page<LogResponse> logs = monitorService.queryLogs(
                operationType, serviceName, userId, status, startTime, endTime, page, size
        );
        
        return ResponseEntity.ok(logs);
    }

    /**
     * 搜索日志（全文搜索）
     * GET /api/v1/logs/search
     */
    @GetMapping("/logs/search")
    @SentinelResource(value = "searchLogs")
    public ResponseEntity<Page<LogResponse>> searchLogs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Searching logs with keyword: {}", keyword);
        Page<LogResponse> logs = monitorService.searchLogs(keyword, page, size);
        return ResponseEntity.ok(logs);
    }

    /**
     * 根据 Trace ID 查询日志
     * GET /api/v1/logs/trace/{traceId}
     */
    @GetMapping("/logs/trace/{traceId}")
    @SentinelResource(value = "getLogsByTraceId")
    public ResponseEntity<List<LogResponse>> getLogsByTraceId(@PathVariable String traceId) {
        log.info("Getting logs by trace ID: {}", traceId);
        List<LogResponse> logs = monitorService.getLogsByTraceId(traceId);
        return ResponseEntity.ok(logs);
    }

    /**
     * 收集性能指标
     * POST /api/v1/metrics
     */
    @PostMapping("/metrics")
    @SentinelResource(value = "collectMetric")
    public ResponseEntity<Map<String, String>> collectMetric(@RequestBody MetricRequest request) {
        log.info("Collecting metric: {} - {} = {}", 
                request.getServiceName(), request.getMetricName(), request.getMetricValue());
        monitorService.collectMetric(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Metric collected successfully"
        ));
    }

    /**
     * 查询性能指标
     * GET /api/v1/metrics
     */
    @GetMapping("/metrics")
    @SentinelResource(value = "queryMetrics")
    public ResponseEntity<Page<MetricResponse>> queryMetrics(
            @RequestParam(required = false) String metricType,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Querying metrics: metricType={}, serviceName={}, metricName={}", 
                metricType, serviceName, metricName);
        
        Page<MetricResponse> metrics = monitorService.queryMetrics(
                metricType, serviceName, metricName, startTime, endTime, page, size
        );
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * 获取系统统计信息
     * GET /api/v1/stats
     */
    @GetMapping("/stats")
    @SentinelResource(value = "getSystemStats")
    public ResponseEntity<StatsResponse> getSystemStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("Getting system stats");
        StatsResponse stats = monitorService.getSystemStats(startTime, endTime);
        return ResponseEntity.ok(stats);
    }
}
