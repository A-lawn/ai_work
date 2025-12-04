package com.rag.ops.session.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {
    
    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 健康检查端点
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "session-service");
        
        // 检查数据库连接
        boolean dbConnected = checkDatabaseConnection();
        health.put("database", dbConnected ? "UP" : "DOWN");
        
        // 检查Redis连接
        boolean redisConnected = checkRedisConnection();
        health.put("redis", redisConnected ? "UP" : "DOWN");
        
        // 如果任何依赖服务不可用，返回503
        if (!dbConnected || !redisConnected) {
            health.put("status", "DOWN");
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }
    
    private boolean checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception e) {
            log.error("数据库连接检查失败", e);
            return false;
        }
    }
    
    private boolean checkRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis连接检查失败", e);
            return false;
        }
    }
}
