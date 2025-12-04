package com.rag.ops.monitor.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource dataSource;
    private final ElasticsearchOperations elasticsearchOperations;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "monitor-service");
        health.put("timestamp", System.currentTimeMillis());

        // Check PostgreSQL
        try (Connection conn = dataSource.getConnection()) {
            health.put("postgresql", conn.isValid(2) ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("postgresql", "DOWN");
            health.put("postgresql_error", e.getMessage());
            log.error("PostgreSQL health check failed", e);
        }

        // Check Elasticsearch
        try {
            boolean esHealthy = elasticsearchOperations.indexOps(
                com.rag.ops.monitor.document.LogDocument.class
            ).exists();
            health.put("elasticsearch", esHealthy ? "UP" : "DOWN");
        } catch (Exception e) {
            health.put("elasticsearch", "DOWN");
            health.put("elasticsearch_error", e.getMessage());
            log.error("Elasticsearch health check failed", e);
        }

        return ResponseEntity.ok(health);
    }
}
