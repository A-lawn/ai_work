package com.rag.ops.config.repository;

import com.rag.ops.config.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置仓储
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * 根据配置键查找配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
    
    /**
     * 根据配置类型查找配置列表
     */
    List<SystemConfig> findByConfigType(String configType);
    
    /**
     * 查找所有启用的配置
     */
    List<SystemConfig> findByIsActiveTrue();
    
    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKey(String configKey);
}
