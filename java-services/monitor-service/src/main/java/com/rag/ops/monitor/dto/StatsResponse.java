package com.rag.ops.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * System Statistics Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {

    private Long totalLogs;
    private Long totalMetrics;
    private Map<String, Long> logsByType;
    private Map<String, Long> logsByStatus;
    private Map<String, Double> averageMetrics;
    private Map<String, Long> logsByService;
    private Double avgResponseTime;
    private Double maxResponseTime;
    private Long errorCount;
    private Double errorRate;
}
