package com.rag.ops.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Slf4j
@RestController
@RequestMapping("/actuator")
public class HealthController {

    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 基础健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "gateway-service");
        return Mono.just(health);
    }

    /**
     * 详细健康检查（包含服务发现信息）
     */
    @GetMapping("/health/detail")
    public Mono<Map<String, Object>> healthDetail() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "gateway-service");
        
        try {
            // 获取已注册的服务列表
            List<String> services = discoveryClient.getServices();
            health.put("discoveredServices", services);
            health.put("serviceCount", services.size());
        } catch (Exception e) {
            log.error("Error getting service discovery info", e);
            health.put("discoveryStatus", "ERROR");
        }
        
        return Mono.just(health);
    }
}
