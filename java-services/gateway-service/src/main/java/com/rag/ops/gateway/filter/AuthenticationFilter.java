package com.rag.ops.gateway.filter;

import cn.hutool.core.util.StrUtil;
import com.rag.ops.gateway.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 全局认证过滤器
 */
@Slf4j
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    @Autowired
    private AuthService authService;

    /**
     * 白名单路径 - 不需要认证的路径
     */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health",
            "/actuator/prometheus"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Processing request: {} {}", request.getMethod(), path);

        // 检查是否在白名单中
        if (isWhitelisted(path)) {
            log.debug("Path {} is whitelisted, skipping authentication", path);
            return chain.filter(exchange);
        }

        // 尝试从 Header 中获取 Token
        String token = extractToken(request, "Authorization");
        if (StrUtil.isNotBlank(token)) {
            return validateToken(exchange, chain, token);
        }

        // 尝试从 Header 中获取 API Key
        String apiKey = extractToken(request, "X-API-Key");
        if (StrUtil.isNotBlank(apiKey)) {
            return validateApiKey(exchange, chain, apiKey);
        }

        // 没有提供认证信息
        log.warn("No authentication credentials provided for path: {}", path);
        return unauthorized(exchange.getResponse(), "Missing authentication credentials");
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhitelisted(String path) {
        return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求头中提取 Token
     */
    private String extractToken(ServerHttpRequest request, String headerName) {
        List<String> headers = request.getHeaders().get(headerName);
        if (headers != null && !headers.isEmpty()) {
            String value = headers.get(0);
            if (headerName.equals("Authorization") && value.startsWith("Bearer ")) {
                return value.substring(7);
            }
            return value;
        }
        return null;
    }

    /**
     * 验证 JWT Token
     */
    private Mono<Void> validateToken(ServerWebExchange exchange, GatewayFilterChain chain, String token) {
        return authService.validateToken(token)
                .flatMap(isValid -> {
                    if (isValid) {
                        log.debug("Token validation successful");
                        // 可以在这里添加用户信息到请求头
                        return chain.filter(exchange);
                    } else {
                        log.warn("Token validation failed");
                        return unauthorized(exchange.getResponse(), "Invalid or expired token");
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error validating token", e);
                    return unauthorized(exchange.getResponse(), "Authentication service error");
                });
    }

    /**
     * 验证 API Key
     */
    private Mono<Void> validateApiKey(ServerWebExchange exchange, GatewayFilterChain chain, String apiKey) {
        return authService.validateApiKey(apiKey)
                .flatMap(isValid -> {
                    if (isValid) {
                        log.debug("API Key validation successful");
                        return chain.filter(exchange);
                    } else {
                        log.warn("API Key validation failed");
                        return unauthorized(exchange.getResponse(), "Invalid API Key");
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error validating API Key", e);
                    return unauthorized(exchange.getResponse(), "Authentication service error");
                });
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\":{\"code\":1002,\"message\":\"%s\"}}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级，在其他过滤器之前执行
    }
}
