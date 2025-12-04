package com.rag.ops.auth.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import com.rag.ops.auth.config.ApiKeyProperties;
import com.rag.ops.auth.entity.ApiKey;
import com.rag.ops.auth.entity.User;
import com.rag.ops.auth.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * API Key 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {
    
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyProperties apiKeyProperties;
    
    /**
     * 生成新的 API Key
     */
    @Transactional
    public ApiKeyResult generateApiKey(User user, String name, String description, Integer expirationDays) {
        // 生成随机 API Key (32字节，转为64字符的十六进制字符串)
        String rawKey = "rak_" + RandomUtil.randomString(64);
        
        // 计算哈希值
        String keyHash = hashApiKey(rawKey);
        
        // 计算过期时间
        LocalDateTime expiresAt = null;
        if (expirationDays != null && expirationDays > 0) {
            expiresAt = LocalDateTime.now().plusDays(expirationDays);
        } else if (apiKeyProperties.getDefaultExpirationDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(apiKeyProperties.getDefaultExpirationDays());
        }
        
        // 创建 API Key 实体
        ApiKey apiKey = ApiKey.builder()
                .keyHash(keyHash)
                .name(name)
                .description(description)
                .user(user)
                .isActive(true)
                .expiresAt(expiresAt)
                .build();
        
        apiKeyRepository.save(apiKey);
        
        log.info("Generated new API key for user: {}, name: {}", user.getUsername(), name);
        
        // 返回原始 Key（仅此一次）
        return new ApiKeyResult(apiKey.getId(), rawKey, apiKey.getExpiresAt());
    }
    
    /**
     * 验证 API Key
     */
    @Transactional
    public boolean validateApiKey(String rawKey) {
        String keyHash = hashApiKey(rawKey);
        
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);
        
        if (apiKeyOpt.isEmpty()) {
            log.debug("API key not found");
            return false;
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // 检查是否激活
        if (!apiKey.getIsActive()) {
            log.debug("API key is inactive");
            return false;
        }
        
        // 检查是否过期
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.debug("API key has expired");
            return false;
        }
        
        // 更新最后使用时间
        apiKey.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        
        return true;
    }
    
    /**
     * 获取用户的所有 API Keys
     */
    public List<ApiKey> getUserApiKeys(User user) {
        return apiKeyRepository.findByUser(user);
    }
    
    /**
     * 获取用户的激活 API Keys
     */
    public List<ApiKey> getUserActiveApiKeys(User user) {
        return apiKeyRepository.findByUserAndIsActive(user, true);
    }
    
    /**
     * 撤销 API Key
     */
    @Transactional
    public boolean revokeApiKey(String apiKeyId, User user) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(apiKeyId);
        
        if (apiKeyOpt.isEmpty()) {
            return false;
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // 检查所有权
        if (!apiKey.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to revoke API key {} owned by another user", 
                    user.getUsername(), apiKeyId);
            return false;
        }
        
        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        
        log.info("Revoked API key {} for user {}", apiKeyId, user.getUsername());
        return true;
    }
    
    /**
     * 删除 API Key
     */
    @Transactional
    public boolean deleteApiKey(String apiKeyId, User user) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(apiKeyId);
        
        if (apiKeyOpt.isEmpty()) {
            return false;
        }
        
        ApiKey apiKey = apiKeyOpt.get();
        
        // 检查所有权
        if (!apiKey.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to delete API key {} owned by another user", 
                    user.getUsername(), apiKeyId);
            return false;
        }
        
        apiKeyRepository.delete(apiKey);
        
        log.info("Deleted API key {} for user {}", apiKeyId, user.getUsername());
        return true;
    }
    
    /**
     * 清理过期的 API Keys
     */
    @Transactional
    public int cleanupExpiredApiKeys() {
        List<ApiKey> expiredKeys = apiKeyRepository.findByExpiresAtBefore(LocalDateTime.now());
        
        for (ApiKey apiKey : expiredKeys) {
            apiKey.setIsActive(false);
        }
        
        apiKeyRepository.saveAll(expiredKeys);
        
        log.info("Deactivated {} expired API keys", expiredKeys.size());
        return expiredKeys.size();
    }
    
    /**
     * 哈希 API Key
     */
    private String hashApiKey(String rawKey) {
        return SecureUtil.sha256(rawKey);
    }
    
    /**
     * API Key 生成结果
     */
    public record ApiKeyResult(String id, String rawKey, LocalDateTime expiresAt) {}
}
