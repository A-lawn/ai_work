package com.rag.ops.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Log Request DTO
 * 接收来自其他服务的日志请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogRequest {

    private String operationType;
    private String serviceName;
    private String userId;
    private String resourceId;
    private String resourceType;
    private String action;
    private String status;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> details;
    private Long durationMs;
    private String traceId;
    private String spanId;
}
