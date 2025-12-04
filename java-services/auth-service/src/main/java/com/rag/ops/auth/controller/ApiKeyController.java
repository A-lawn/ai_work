package com.rag.ops.auth.controller;

import com.rag.ops.auth.dto.ApiKeyRequest;
import com.rag.ops.auth.dto.ApiKeyResponse;
import com.rag.ops.auth.entity.ApiKey;
import com.rag.ops.auth.entity.User;
import com.rag.ops.auth.service.ApiKeyService;
import com.rag.ops.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key 管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {
    
    private final ApiKeyService apiKeyService;
    private final AuthenticationService authenticationService;
    
    /**
     * 创建 API Key
     */
    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ApiKeyRequest request) {
        
        User user = getUserFromToken(authHeader);
        
        ApiKeyService.ApiKeyResult result = apiKeyService.generateApiKey(
                user,
                request.getName(),
                request.getDescription(),
                request.getExpirationDays()
        );
        
        ApiKeyResponse response = ApiKeyResponse.builder()
                .id(result.id())
                .name(request.getName())
                .description(request.getDescription())
                .apiKey(result.rawKey())  // 仅在创建时返回
                .isActive(true)
                .expiresAt(result.expiresAt())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户的所有 API Keys
     */
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(
            @RequestHeader("Authorization") String authHeader) {
        
        User user = getUserFromToken(authHeader);
        
        List<ApiKey> apiKeys = apiKeyService.getUserApiKeys(user);
        
        List<ApiKeyResponse> responses = apiKeys.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
    
    /**
     * 撤销 API Key
     */
    @DeleteMapping("/{apiKeyId}")
    public ResponseEntity<Void> revokeApiKey(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String apiKeyId) {
        
        User user = getUserFromToken(authHeader);
        
        boolean success = apiKeyService.revokeApiKey(apiKeyId, user);
        
        if (success) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 从 Token 中获取用户
     */
    private User getUserFromToken(String authHeader) {
        String token = extractToken(authHeader);
        return authenticationService.getUserFromToken(token);
    }
    
    /**
     * 从 Authorization header 中提取 Token
     */
    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
    
    /**
     * 转换为响应对象
     */
    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .description(apiKey.getDescription())
                .isActive(apiKey.getIsActive())
                .createdAt(apiKey.getCreatedAt())
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .build();
    }
}
