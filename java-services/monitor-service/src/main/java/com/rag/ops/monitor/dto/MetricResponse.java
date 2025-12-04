package com.rag.ops.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Metric Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricResponse {

    private String id;
    private String metricType;
    private String serviceName;
    private String metricName;
    private Double metricValue;
    private String unit;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
    private String tags;
}
