package com.rag.ops.config.service;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.rag.ops.config.client.LlmServiceClient;
import com.rag.ops.config.dto.*;
import com.rag.ops.config.entity.SystemConfig;
import com.rag.ops.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {
    
    private final SystemConfigRepository configRepository;
    private final NacosConfigManager nacosConfigManager;
    private final LlmServiceClient llmServiceClient;
    
    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String nacosGroup;
    
    /**
     * 获取所有配置
     */
    public Map<String, Object> getAllConfigs() {
        log.info("获取所有配置");
        List<SystemConfig> configs = configRepository.findByIsActiveTrue();
        
        Map<String, Object> configMap = new HashMap<>();
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), parseConfigValue(config));
        }
        
        return configMap;
    }
    
    /**
     * 获取配置详情列表
     */
    public List<ConfigResponse> getAllConfigDetails() {
        log.info("获取所有配置详情");
        List<SystemConfig> configs = configRepository.findAll();
        
        return configs.stream()
                .map(this::toConfigResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据配置键获取配置
     */
    public ConfigResponse getConfigByKey(String configKey) {
        log.info("获取配置: {}", configKey);
        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("配置不存在: " + configKey));
        
        return toConfigResponse(config);
    }
    
    /**
     * 更新配置
     */
    @Transactional
    public ConfigResponse updateConfig(String configKey, ConfigRequest request) {
        log.info("更新配置: {}", configKey);
        
        SystemConfig config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new RuntimeException("配置不存在: " + configKey));
        
        // 更新配置
        if (request.getConfigValue() != null) {
            config.setConfigValue(request.getConfigValue());
        }
        if (request.getConfigType() != null) {
            config.setConfigType(request.getConfigType());
        }
        if (request.getDescription() != null) {
            config.setDescription(request.getDescription());
        }
        if (request.getIsActive() != null) {
            config.setIsActive(request.getIsActive());
        }
        
        // 验证配置值
        validateConfig(config);
        
        // 保存到数据库
        SystemConfig savedConfig = configRepository.save(config);
        
        // 同步到Nacos配置中心
        syncToNacos(savedConfig);
        
        return toConfigResponse(savedConfig);
    }
    
    /**
     * 创建配置
     */
    @Transactional
    public ConfigResponse createConfig(ConfigRequest request) {
        log.info("创建配置: {}", request.getConfigKey());
        
        // 检查配置是否已存在
        if (configRepository.existsByConfigKey(request.getConfigKey())) {
            throw new RuntimeException("配置已存在: " + request.getConfigKey());
        }
        
        SystemConfig config = SystemConfig.builder()
                .configKey(request.getConfigKey())
                .configValue(request.getConfigValue())
                .configType(request.getConfigType())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        // 验证配置值
        validateConfig(config);
        
        // 保存到数据库
        SystemConfig savedConfig = configRepository.save(config);
        
        // 同步到Nacos配置中心
        syncToNacos(savedConfig);
        
        return toConfigResponse(savedConfig);
    }
    
    /**
     * 批量更新配置
     */
    @Transactional
    public Map<String, Object> batchUpdateConfigs(Map<String, String> configs) {
        log.info("批量更新配置，数量: {}", configs.size());
        
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            try {
                SystemConfig config = configRepository.findByConfigKey(entry.getKey())
                        .orElse(SystemConfig.builder()
                                .configKey(entry.getKey())
                                .isActive(true)
                                .build());
                
                config.setConfigValue(entry.getValue());
                validateConfig(config);
                
                SystemConfig savedConfig = configRepository.save(config);
                syncToNacos(savedConfig);
                
                successCount++;
            } catch (Exception e) {
                log.error("更新配置失败: {}", entry.getKey(), e);
                failCount++;
            }
        }
        
        result.put("success", successCount);
        result.put("failed", failCount);
        result.put("total", configs.size());
        
        return result;
    }
    
    /**
     * 测试LLM连接
     */
    public TestLlmResponse testLlmConnection(TestLlmRequest request) {
        log.info("测试LLM连接: provider={}", request.getProvider());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 构建测试请求
            String testPrompt = request.getTestPrompt() != null 
                    ? request.getTestPrompt() 
                    : "Hello, this is a test message. Please respond with 'OK'.";
            
            LlmGenerateRequest llmRequest = LlmGenerateRequest.builder()
                    .prompt(testPrompt)
                    .provider(request.getProvider())
                    .apiKey(request.getApiKey())
                    .endpoint(request.getEndpoint())
                    .model(request.getModel())
                    .temperature(0.7)
                    .maxTokens(50)
                    .build();
            
            // 调用LLM服务
            LlmGenerateResponse llmResponse = llmServiceClient.generate(llmRequest);
            
            long latency = System.currentTimeMillis() - startTime;
            
            if (llmResponse.getSuccess()) {
                log.info("LLM连接测试成功，延迟: {}ms", latency);
                return TestLlmResponse.builder()
                        .success(true)
                        .message("LLM连接测试成功")
                        .latency(latency)
                        .response(llmResponse.getResponse())
                        .build();
            } else {
                log.error("LLM连接测试失败: {}", llmResponse.getMessage());
                return TestLlmResponse.builder()
                        .success(false)
                        .message("LLM连接测试失败: " + llmResponse.getMessage())
                        .latency(latency)
                        .build();
            }
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("LLM连接测试异常", e);
            return TestLlmResponse.builder()
                    .success(false)
                    .message("LLM连接测试异常: " + e.getMessage())
                    .latency(latency)
                    .build();
        }
    }
    
    /**
     * 同步配置到Nacos配置中心
     */
    private void syncToNacos(SystemConfig config) {
        try {
            ConfigService nacosConfigService = nacosConfigManager.getConfigService();
            String dataId = "system-config-" + config.getConfigKey();
            
            boolean result = nacosConfigService.publishConfig(
                    dataId,
                    nacosGroup,
                    config.getConfigValue()
            );
            
            if (result) {
                log.info("配置同步到Nacos成功: {}", config.getConfigKey());
            } else {
                log.warn("配置同步到Nacos失败: {}", config.getConfigKey());
            }
        } catch (NacosException e) {
            log.error("同步配置到Nacos异常: {}", config.getConfigKey(), e);
        }
    }
    
    /**
     * 验证配置值
     */
    private void validateConfig(SystemConfig config) {
        String configType = config.getConfigType();
        String configValue = config.getConfigValue();
        
        if (configValue == null || configValue.trim().isEmpty()) {
            return;
        }
        
        try {
            switch (configType != null ? configType : "") {
                case "INTEGER":
                    Integer.parseInt(configValue);
                    break;
                case "FLOAT":
                    Double.parseDouble(configValue);
                    break;
                case "BOOLEAN":
                    if (!"true".equalsIgnoreCase(configValue) && !"false".equalsIgnoreCase(configValue)) {
                        throw new IllegalArgumentException("布尔值必须是true或false");
                    }
                    break;
                case "JSON":
                    // 简单验证JSON格式
                    if (!configValue.trim().startsWith("{") && !configValue.trim().startsWith("[")) {
                        throw new IllegalArgumentException("JSON格式不正确");
                    }
                    break;
                default:
                    // STRING类型或其他，不需要特殊验证
                    break;
            }
        } catch (Exception e) {
            throw new RuntimeException("配置值验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析配置值
     */
    private Object parseConfigValue(SystemConfig config) {
        String configType = config.getConfigType();
        String configValue = config.getConfigValue();
        
        if (configValue == null) {
            return null;
        }
        
        try {
            switch (configType != null ? configType : "STRING") {
                case "INTEGER":
                    return Integer.parseInt(configValue);
                case "FLOAT":
                    return Double.parseDouble(configValue);
                case "BOOLEAN":
                    return Boolean.parseBoolean(configValue);
                default:
                    return configValue;
            }
        } catch (Exception e) {
            log.warn("解析配置值失败，返回原始字符串: {}", config.getConfigKey());
            return configValue;
        }
    }
    
    /**
     * 转换为响应DTO
     */
    private ConfigResponse toConfigResponse(SystemConfig config) {
        return ConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .configType(config.getConfigType())
                .description(config.getDescription())
                .isActive(config.getIsActive())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
