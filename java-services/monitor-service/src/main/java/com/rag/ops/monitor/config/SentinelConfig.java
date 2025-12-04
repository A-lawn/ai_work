package com.rag.ops.monitor.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel Configuration
 * 配置 Sentinel 流控、熔断规则
 */
@Configuration
@Slf4j
public class SentinelConfig {

    @Value("${spring.cloud.nacos.config.server-addr:localhost:8848}")
    private String nacosServerAddr;

    @Value("${spring.application.name:monitor-service}")
    private String applicationName;

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    @PostConstruct
    public void init() {
        // 初始化默认流控规则
        initDefaultFlowRules();

        // 从 Nacos 加载流控规则
        loadFlowRulesFromNacos();

        log.info("Sentinel configuration initialized for {}", applicationName);
    }

    /**
     * 初始化默认流控规则
     */
    private void initDefaultFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 日志收集接口限流
        FlowRule collectLogRule = new FlowRule();
        collectLogRule.setResource("collectLog");
        collectLogRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        collectLogRule.setCount(500); // QPS 500
        rules.add(collectLogRule);

        // 指标收集接口限流
        FlowRule collectMetricRule = new FlowRule();
        collectMetricRule.setResource("collectMetric");
        collectMetricRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        collectMetricRule.setCount(1000); // QPS 1000
        rules.add(collectMetricRule);

        // 日志查询接口限流
        FlowRule queryLogsRule = new FlowRule();
        queryLogsRule.setResource("queryLogs");
        queryLogsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        queryLogsRule.setCount(100); // QPS 100
        rules.add(queryLogsRule);

        // 指标查询接口限流
        FlowRule queryMetricsRule = new FlowRule();
        queryMetricsRule.setResource("queryMetrics");
        queryMetricsRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        queryMetricsRule.setCount(100); // QPS 100
        rules.add(queryMetricsRule);

        FlowRuleManager.loadRules(rules);
        log.info("Default flow rules loaded: {} rules", rules.size());
    }

    /**
     * 从 Nacos 加载流控规则
     */
    private void loadFlowRulesFromNacos() {
        try {
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(
                    nacosServerAddr,
                    "SENTINEL_GROUP",
                    applicationName + "-flow-rules",
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {})
            );
            FlowRuleManager.register2Property(flowRuleDataSource.getProperty());
            log.info("Flow rules loaded from Nacos");
        } catch (Exception e) {
            log.warn("Failed to load flow rules from Nacos, using default rules: {}", e.getMessage());
        }
    }
}
