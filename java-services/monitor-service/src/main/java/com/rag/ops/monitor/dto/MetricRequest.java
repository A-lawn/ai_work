package com.rag.ops.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Metric Request DTO
 * 接收性能指标请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRequest {

    private String metricType;
    private String serviceName;
    private String metricName;
    private Double metricValue;
    private String unit;
    private Map<String, Object> metadata;
    private String tags;
}
