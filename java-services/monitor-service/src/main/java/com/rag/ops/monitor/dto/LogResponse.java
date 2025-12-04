package com.rag.ops.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Log Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponse {

    private String id;
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
    private LocalDateTime timestamp;
    private Long durationMs;
    private String traceId;
    private String spanId;
}
