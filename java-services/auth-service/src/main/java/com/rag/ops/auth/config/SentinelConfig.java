package com.rag.ops.auth.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Sentinel 配置
 */
@Slf4j
@Configuration
public class SentinelConfig {
    
    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosServerAddr;
    
    @Value("${spring.cloud.nacos.discovery.namespace:}")
    private String namespace;
    
    @Value("${spring.application.name}")
    private String applicationName;
    
    /**
     * 注册 Sentinel 切面
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
    
    /**
     * 初始化 Sentinel 规则数据源
     */
    @PostConstruct
    public void initRules() {
        log.info("Initializing Sentinel rules from Nacos");
        
        try {
            // 流控规则
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(
                    nacosServerAddr,
                    "SENTINEL_GROUP",
                    applicationName + "-flow-rules",
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {})
            );
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
            log.info("Flow rules loaded from Nacos");
            
            // 熔断降级规则
            ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource = new NacosDataSource<>(
                    nacosServerAddr,
                    "SENTINEL_GROUP",
                    applicationName + "-degrade-rules",
                    source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {})
            );
            DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
            log.info("Degrade rules loaded from Nacos");
            
            // 系统保护规则
            ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new NacosDataSource<>(
                    nacosServerAddr,
                    "SENTINEL_GROUP",
                    applicationName + "-system-rules",
                    source -> JSON.parseObject(source, new TypeReference<List<SystemRule>>() {})
            );
            SystemRuleManager.register2Property(systemRuleDataSource.getProperty());
            log.info("System rules loaded from Nacos");
            
        } catch (Exception e) {
            log.error("Failed to initialize Sentinel rules from Nacos", e);
        }
    }
}
