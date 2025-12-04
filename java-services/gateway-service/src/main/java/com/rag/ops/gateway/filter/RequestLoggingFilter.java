package com.rag.ops.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 请求日志过滤器
 * 记录所有通过网关的请求和响应信息
 */
@Slf4j
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = request.getId();
        String method = request.getMethod().toString();
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        String clientIp = getClientIp(request);
        
        long startTime = System.currentTimeMillis();
        String startTimeStr = LocalDateTime.now().format(FORMATTER);

        log.info("==> [{}] {} {} {} from {} at {}",
                requestId, method, path, query != null ? "?" + query : "", clientIp, startTimeStr);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
            String endTimeStr = LocalDateTime.now().format(FORMATTER);

            log.info("<== [{}] {} {} - Status: {} - Duration: {}ms - Completed at {}",
                    requestId, method, path, statusCode, duration, endTimeStr);

            // 记录慢请求
            if (duration > 3000) {
                log.warn("SLOW REQUEST: [{}] {} {} took {}ms", requestId, method, path, duration);
            }

            // 记录错误响应
            if (statusCode >= 400) {
                log.warn("ERROR RESPONSE: [{}] {} {} returned status {}", requestId, method, path, statusCode);
            }
        }));
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddress() != null ? 
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        }
        // 如果有多个代理，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public int getOrder() {
        return -200; // 最高优先级，第一个执行
    }
}
