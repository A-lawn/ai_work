package com.rag.ops.document.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 配置
 */
@Slf4j
@Configuration
public class SentinelConfig {
    
    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel rules initialized");
    }
    
    /**
     * 初始化流控规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // 文档上传流控规则
        FlowRule uploadRule = new FlowRule();
        uploadRule.setResource("uploadDocument");
        uploadRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        uploadRule.setCount(10); // QPS 限制为 10
        rules.add(uploadRule);
        
        // 文档处理流控规则
        FlowRule processRule = new FlowRule();
        processRule.setResource("processDocument");
        processRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        processRule.setCount(20); // QPS 限制为 20
        rules.add(processRule);
        
        FlowRuleManager.loadRules(rules);
    }
    
    /**
     * 初始化熔断降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        
        // 文档处理服务熔断规则
        DegradeRule processRule = new DegradeRule();
        processRule.setResource("processDocument");
        processRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO); // 异常比例
        processRule.setCount(0.5); // 50% 异常比例
        processRule.setTimeWindow(10); // 熔断时长 10 秒
        processRule.setMinRequestAmount(5); // 最小请求数
        processRule.setStatIntervalMs(1000); // 统计时长 1 秒
        rules.add(processRule);
        
        // 慢调用熔断规则
        DegradeRule slowCallRule = new DegradeRule();
        slowCallRule.setResource("processDocument");
        slowCallRule.setGrade(RuleConstant.DEGRADE_GRADE_RT); // 响应时间
        slowCallRule.setCount(3000); // 3 秒
        slowCallRule.setTimeWindow(10); // 熔断时长 10 秒
        slowCallRule.setMinRequestAmount(5);
        slowCallRule.setStatIntervalMs(1000);
        rules.add(slowCallRule);
        
        DegradeRuleManager.loadRules(rules);
    }
}
