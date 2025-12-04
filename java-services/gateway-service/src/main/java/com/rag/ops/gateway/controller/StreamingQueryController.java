package com.rag.ops.gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 流式查询控制器
 * 代理 RAG Query Service 的流式响应
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class StreamingQueryController {
    
    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;
    private final Random random = new Random();
    
    /**
     * 流式查询端点
     * 使用 Server-Sent Events (SSE) 返回流式响应
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamQuery(@RequestBody Map<String, Object> request) {
        log.info("收到流式查询请求: {}", request.get("question"));
        
        try {
            // 获取 RAG Query Service 实例
            String serviceUrl = getServiceUrl("rag-query-service");
            if (serviceUrl == null) {
                log.error("无法找到 rag-query-service 实例");
                return Flux.just(
                    ServerSentEvent.<String>builder()
                        .event("error")
                        .data("{\"error\": \"服务不可用\"}")
                        .build()
                );
            }
            
            log.info("代理流式查询到: {}", serviceUrl);
            
            // 创建 WebClient 并调用流式接口
            WebClient webClient = webClientBuilder.baseUrl(serviceUrl).build();
            
            return webClient.post()
                    .uri("/api/query/stream")
                    .bodyValue(request)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> {
                            log.error("流式查询失败，状态码: {}", response.statusCode());
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        log.error("错误响应: {}", body);
                                        return reactor.core.publisher.Mono.error(
                                            new RuntimeException("查询失败: " + body)
                                        );
                                    });
                        }
                    )
                    .bodyToFlux(String.class)
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .data(chunk)
                            .build())
                    .timeout(Duration.ofSeconds(60))
                    .onErrorResume(error -> {
                        log.error("流式查询异常: {}", error.getMessage(), error);
                        return Flux.just(
                            ServerSentEvent.<String>builder()
                                .event("error")
                                .data("{\"error\": \"" + error.getMessage() + "\"}")
                                .build()
                        );
                    });
            
        } catch (Exception e) {
            log.error("流式查询处理异常: {}", e.getMessage(), e);
            return Flux.just(
                ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\": \"" + e.getMessage() + "\"}")
                    .build()
            );
        }
    }
    
    /**
     * 获取服务 URL（负载均衡）
     */
    private String getServiceUrl(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        if (instances == null || instances.isEmpty()) {
            log.warn("未找到服务实例: {}", serviceName);
            return null;
        }
        
        // 简单的随机负载均衡
        ServiceInstance instance = instances.get(random.nextInt(instances.size()));
        String url = instance.getUri().toString();
        log.debug("选择服务实例: {} -> {}", serviceName, url);
        
        return url;
    }
}
