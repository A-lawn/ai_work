package com.rag.ops.gateway.config;

import brave.sampler.Sampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 链路追踪配置
 */
@Configuration
public class TracingConfig {

    /**
     * 配置采样率
     * 1.0 表示 100% 采样（开发环境）
     * 生产环境建议设置为 0.1（10%）以减少性能开销
     */
    @Bean
    public Sampler defaultSampler() {
        return Sampler.ALWAYS_SAMPLE;
    }
}
