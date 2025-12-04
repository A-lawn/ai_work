package com.rag.ops.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Sentinel 配置类
 * 配置限流、熔断规则和降级处理
 */
@Slf4j
@Configuration
public class SentinelConfig {

    @Value("${spring.cloud.nacos.discovery.server-addr}")
    private String nacosServerAddr;

    @Value("${spring.application.name}")
    private String applicationName;

    @PostConstruct
    public void init() {
        log.info("Initializing Sentinel configuration");
        
        // 配置限流规则数据源
        configureFlowRules();
        
        // 配置熔断规则数据源
        configureDegradeRules();
        
        // 配置自定义限流/熔断处理器
        configureBlockHandler();
        
        log.info("Sentinel configuration initialized successfully");
    }

    /**
     * 配置流控规则数据源（从 Nacos 读取）
     */
    private void configureFlowRules() {
        try {
            String dataId = applicationName + "-flow-rules";
            String groupId = "SENTINEL_GROUP";
            
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(
                    nacosServerAddr,
                    groupId,
                    dataId,
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {})
            );
            
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
            log.info("Flow rules data source configured: dataId={}, groupId={}", dataId, groupId);
        } catch (Exception e) {
            log.error("Failed to configure flow rules data source", e);
        }
    }

    /**
     * 配置熔断降级规则数据源（从 Nacos 读取）
     */
    private void configureDegradeRules() {
        try {
            String dataId = applicationName + "-degrade-rules";
            String groupId = "SENTINEL_GROUP";
            
            ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource = new NacosDataSource<>(
                    nacosServerAddr,
                    groupId,
                    dataId,
                    source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {})
            );
            
            DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
            log.info("Degrade rules data source configured: dataId={}, groupId={}", dataId, groupId);
        } catch (Exception e) {
            log.error("Failed to configure degrade rules data source", e);
        }
    }

    /**
     * 配置自定义限流/熔断处理器
     */
    private void configureBlockHandler() {
        BlockRequestHandler blockRequestHandler = (exchange, t) -> {
            log.warn("Request blocked by Sentinel: {}", t.getMessage());
            
            String errorMessage = String.format(
                    "{\"error\":{\"code\":1003,\"message\":\"系统繁忙，请稍后重试\",\"details\":\"%s\"}}",
                    t.getClass().getSimpleName()
            );
            
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(errorMessage);
        };
        
        GatewayCallbackManager.setBlockHandler(blockRequestHandler);
        log.info("Custom block handler configured");
    }
}
