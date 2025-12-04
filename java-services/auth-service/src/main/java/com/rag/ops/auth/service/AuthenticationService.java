package com.rag.ops.auth.service;

import com.rag.ops.auth.config.JwtProperties;
import com.rag.ops.auth.dto.LoginRequest;
import com.rag.ops.auth.dto.LoginResponse;
import com.rag.ops.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * 认证服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserService userService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    
    /**
     * 用户登录
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 查找用户
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        
        // 验证密码
        if (!userService.validatePassword(user, request.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        
        // 检查用户状态
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("用户已被禁用");
        }
        
        if (!user.getAccountNonLocked()) {
            throw new IllegalArgumentException("用户账户已被锁定");
        }
        
        // 生成 Token
        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        
        // 更新最后登录时间
        userService.updateLastLoginTime(user);
        
        log.info("User logged in: {}", user.getUsername());
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * 刷新 Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        // 验证刷新 Token
        if (!jwtService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("无效的刷新令牌");
        }
        
        // 提取用户名
        String username = jwtService.extractUsername(refreshToken);
        
        // 查找用户
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        // 检查用户状态
        if (!user.getEnabled()) {
            throw new IllegalArgumentException("用户已被禁用");
        }
        
        // 生成新的访问 Token
        String newAccessToken = jwtService.generateToken(user);
        
        log.info("Token refreshed for user: {}", user.getUsername());
        
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getExpiration() / 1000)
                .userId(user.getId())
                .username(user.getUsername())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toList()))
                .build();
    }
    
    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }
    
    /**
     * 从 Token 中获取用户
     */
    public User getUserFromToken(String token) {
        String username = jwtService.extractUsername(token);
        return userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
