package com.rag.ops.auth.controller;

import com.rag.ops.auth.dto.*;
import com.rag.ops.auth.entity.User;
import com.rag.ops.auth.service.ApiKeyService;
import com.rag.ops.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthenticationService authenticationService;
    private final ApiKeyService apiKeyService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());
        LoginResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        LoginResponse response = authenticationService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 验证 JWT Token
     */
    @PostMapping("/validate-token")
    public ResponseEntity<ValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            
            if (authenticationService.validateToken(token)) {
                User user = authenticationService.getUserFromToken(token);
                return ResponseEntity.ok(ValidationResponse.builder()
                        .valid(true)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .message("Token is valid")
                        .build());
            } else {
                return ResponseEntity.ok(ValidationResponse.builder()
                        .valid(false)
                        .message("Invalid token")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error validating token", e);
            return ResponseEntity.ok(ValidationResponse.builder()
                    .valid(false)
                    .message("Token validation failed: " + e.getMessage())
                    .build());
        }
    }
    
    /**
     * 验证 API Key
     */
    @PostMapping("/validate-api-key")
    public ResponseEntity<ValidationResponse> validateApiKey(@RequestHeader("X-API-Key") String apiKey) {
        try {
            boolean isValid = apiKeyService.validateApiKey(apiKey);
            
            return ResponseEntity.ok(ValidationResponse.builder()
                    .valid(isValid)
                    .message(isValid ? "API Key is valid" : "Invalid API Key")
                    .build());
        } catch (Exception e) {
            log.error("Error validating API key", e);
            return ResponseEntity.ok(ValidationResponse.builder()
                    .valid(false)
                    .message("API Key validation failed: " + e.getMessage())
                    .build());
        }
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
}
