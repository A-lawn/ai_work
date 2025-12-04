package com.rag.ops.auth.repository;

import com.rag.ops.auth.entity.ApiKey;
import com.rag.ops.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * API Key 仓储
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    
    /**
     * 根据 Key Hash 查找 API Key
     */
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    /**
     * 根据用户查找所有 API Keys
     */
    List<ApiKey> findByUser(User user);
    
    /**
     * 根据用户和激活状态查找 API Keys
     */
    List<ApiKey> findByUserAndIsActive(User user, Boolean isActive);
    
    /**
     * 查找已过期的 API Keys
     */
    List<ApiKey> findByExpiresAtBefore(LocalDateTime dateTime);
}
