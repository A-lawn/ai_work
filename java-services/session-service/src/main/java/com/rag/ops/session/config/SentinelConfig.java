package com.rag.ops.session.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Sentinel配置
 */
@Configuration
@Slf4j
public class SentinelConfig {
    
    @Value("${spring.cloud.nacos.discovery.server-addr:localhost:8848}")
    private String nacosServerAddr;
    
    @Value("${spring.application.name:session-service}")
    private String applicationName;
    
    private static final String SENTINEL_GROUP = "SENTINEL_GROUP";
    
    /**
     * 注册Sentinel切面
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
    
    /**
     * 初始化Sentinel规则数据源
     */
    @PostConstruct
    public void init() {
        log.info("初始化Sentinel规则数据源，Nacos地址: {}", nacosServerAddr);
        
        try {
            // 配置流控规则数据源
            initFlowRules();
            
            // 配置熔断降级规则数据源
            initDegradeRules();
            
            log.info("Sentinel规则数据源初始化完成");
        } catch (Exception e) {
            log.error("Sentinel规则数据源初始化失败", e);
        }
    }
    
    /**
     * 初始化流控规则
     */
    private void initFlowRules() {
        String flowRuleDataId = applicationName + "-flow-rules";
        
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(
                nacosServerAddr,
                SENTINEL_GROUP,
                flowRuleDataId,
                source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {})
        );
        
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
        log.info("流控规则数据源注册成功，DataId: {}", flowRuleDataId);
    }
    
    /**
     * 初始化熔断降级规则
     */
    private void initDegradeRules() {
        String degradeRuleDataId = applicationName + "-degrade-rules";
        
        ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource = new NacosDataSource<>(
                nacosServerAddr,
                SENTINEL_GROUP,
                degradeRuleDataId,
                source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {})
        );
        
        DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
        log.info("熔断降级规则数据源注册成功，DataId: {}", degradeRuleDataId);
    }
}
