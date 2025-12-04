package com.rag.ops.config.service;

import com.rag.ops.config.entity.SystemConfig;
import com.rag.ops.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 数据初始化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {
    
    private final SystemConfigRepository configRepository;
    
    @Override
    public void run(String... args) {
        log.info("开始初始化系统配置数据");
        initializeDefaultConfigs();
        log.info("系统配置数据初始化完成");
    }
    
    private void initializeDefaultConfigs() {
        List<SystemConfig> defaultConfigs = Arrays.asList(
                // 文档处理配置
                SystemConfig.builder()
                        .configKey("document.chunk_size")
                        .configValue("512")
                        .configType("INTEGER")
                        .description("文档分块大小（tokens）")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("document.chunk_overlap")
                        .configValue("50")
                        .configType("INTEGER")
                        .description("文档分块重叠大小（tokens）")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("document.supported_formats")
                        .configValue("pdf,docx,txt,md")
                        .configType("STRING")
                        .description("支持的文档格式")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("document.max_file_size")
                        .configValue("52428800")
                        .configType("INTEGER")
                        .description("最大文件大小（字节，默认50MB）")
                        .isActive(true)
                        .build(),
                
                // 检索配置
                SystemConfig.builder()
                        .configKey("retrieval.top_k")
                        .configValue("5")
                        .configType("INTEGER")
                        .description("检索返回的文档块数量")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("retrieval.similarity_threshold")
                        .configValue("0.7")
                        .configType("FLOAT")
                        .description("相似度阈值")
                        .isActive(true)
                        .build(),
                
                // LLM配置
                SystemConfig.builder()
                        .configKey("llm.provider")
                        .configValue("openai")
                        .configType("STRING")
                        .description("LLM提供商（openai/azure/local）")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("llm.model")
                        .configValue("gpt-4")
                        .configType("STRING")
                        .description("LLM模型名称")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("llm.temperature")
                        .configValue("0.7")
                        .configType("FLOAT")
                        .description("LLM温度参数")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("llm.max_tokens")
                        .configValue("1000")
                        .configType("INTEGER")
                        .description("LLM最大生成tokens")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("llm.api_base")
                        .configValue("https://api.openai.com/v1")
                        .configType("STRING")
                        .description("LLM API基础URL")
                        .isActive(true)
                        .build(),
                
                // 嵌入模型配置
                SystemConfig.builder()
                        .configKey("embedding.provider")
                        .configValue("openai")
                        .configType("STRING")
                        .description("嵌入模型提供商")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("embedding.model")
                        .configValue("text-embedding-ada-002")
                        .configType("STRING")
                        .description("嵌入模型名称")
                        .isActive(true)
                        .build(),
                
                // 会话配置
                SystemConfig.builder()
                        .configKey("session.max_history")
                        .configValue("10")
                        .configType("INTEGER")
                        .description("会话保留的最大历史轮数")
                        .isActive(true)
                        .build(),
                
                SystemConfig.builder()
                        .configKey("session.max_tokens")
                        .configValue("4000")
                        .configType("INTEGER")
                        .description("会话上下文最大tokens")
                        .isActive(true)
                        .build(),
                
                // 系统配置
                SystemConfig.builder()
                        .configKey("system.language")
                        .configValue("zh-CN")
                        .configType("STRING")
                        .description("系统语言")
                        .isActive(true)
                        .build()
        );
        
        for (SystemConfig config : defaultConfigs) {
            if (!configRepository.existsByConfigKey(config.getConfigKey())) {
                configRepository.save(config);
                log.info("创建默认配置: {}", config.getConfigKey());
            }
        }
    }
}
