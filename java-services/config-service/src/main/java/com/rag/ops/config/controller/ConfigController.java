package com.rag.ops.config.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.rag.ops.config.dto.ConfigRequest;
import com.rag.ops.config.dto.ConfigResponse;
import com.rag.ops.config.dto.TestLlmRequest;
import com.rag.ops.config.dto.TestLlmResponse;
import com.rag.ops.config.service.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 配置管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
public class ConfigController {
    
    private final ConfigService configService;
    
    /**
     * 获取所有配置
     */
    @GetMapping
    @SentinelResource(value = "getAllConfigs")
    public ResponseEntity<Map<String, Object>> getAllConfigs() {
        log.info("GET /api/v1/config - 获取所有配置");
        Map<String, Object> configs = configService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 获取所有配置详情
     */
    @GetMapping("/details")
    @SentinelResource(value = "getAllConfigDetails")
    public ResponseEntity<List<ConfigResponse>> getAllConfigDetails() {
        log.info("GET /api/v1/config/details - 获取所有配置详情");
        List<ConfigResponse> configs = configService.getAllConfigDetails();
        return ResponseEntity.ok(configs);
    }
    
    /**
     * 根据配置键获取配置
     */
    @GetMapping("/{configKey}")
    @SentinelResource(value = "getConfigByKey")
    public ResponseEntity<ConfigResponse> getConfigByKey(@PathVariable String configKey) {
        log.info("GET /api/v1/config/{} - 获取配置", configKey);
        ConfigResponse config = configService.getConfigByKey(configKey);
        return ResponseEntity.ok(config);
    }
    
    /**
     * 更新配置
     */
    @PutMapping("/{configKey}")
    @SentinelResource(value = "updateConfig")
    public ResponseEntity<ConfigResponse> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody ConfigRequest request) {
        log.info("PUT /api/v1/config/{} - 更新配置", configKey);
        ConfigResponse config = configService.updateConfig(configKey, request);
        return ResponseEntity.ok(config);
    }
    
    /**
     * 创建配置
     */
    @PostMapping
    @SentinelResource(value = "createConfig")
    public ResponseEntity<ConfigResponse> createConfig(@Valid @RequestBody ConfigRequest request) {
        log.info("POST /api/v1/config - 创建配置: {}", request.getConfigKey());
        ConfigResponse config = configService.createConfig(request);
        return ResponseEntity.ok(config);
    }
    
    /**
     * 批量更新配置
     */
    @PutMapping
    @SentinelResource(value = "batchUpdateConfigs")
    public ResponseEntity<Map<String, Object>> batchUpdateConfigs(
            @RequestBody Map<String, String> configs) {
        log.info("PUT /api/v1/config - 批量更新配置，数量: {}", configs.size());
        Map<String, Object> result = configService.batchUpdateConfigs(configs);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 测试LLM连接
     */
    @PostMapping("/test-llm")
    @SentinelResource(value = "testLlmConnection")
    public ResponseEntity<TestLlmResponse> testLlmConnection(
            @Valid @RequestBody TestLlmRequest request) {
        log.info("POST /api/v1/config/test-llm - 测试LLM连接: {}", request.getProvider());
        TestLlmResponse response = configService.testLlmConnection(request);
        return ResponseEntity.ok(response);
    }
}
