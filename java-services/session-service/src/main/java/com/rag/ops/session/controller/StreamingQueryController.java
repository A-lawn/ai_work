package com.rag.ops.session.controller;

import com.rag.ops.session.dto.SessionStreamQueryRequest;
import com.rag.ops.session.entity.Message;
import com.rag.ops.session.entity.Session;
import com.rag.ops.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 流式查询控制器
 * 处理会话的流式查询
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class StreamingQueryController {
    
    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;
    private final SessionRepository sessionRepository;
    private final Random random = new Random();
    
    /**
     * 会话流式查询端点
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamQuery(@RequestBody SessionStreamQueryRequest request) {
        log.info("收到会话流式查询请求，问题: {}, 会话ID: {}", request.getQuestion(), request.getSessionId());
        
        return Mono.fromCallable(() -> {
            // 1. 获取或创建会话
            Session session;
            if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
                session = sessionRepository.findByIdWithMessages(request.getSessionId())
                        .orElseGet(() -> createNewSession(request.getUserId()));
            } else {
                session = createNewSession(request.getUserId());
            }
            
            // 2. 构建会话历史
            List<Map<String, String>> sessionHistory = buildSessionHistory(session);
            
            // 3. 保存用户问题
            Message userMessage = Message.builder()
                    .role("USER")
                    .content(request.getQuestion())
                    .timestamp(LocalDateTime.now())
                    .build();
            session.addMessage(userMessage);
            sessionRepository.save(session);
            
            return new SessionContext(session, sessionHistory);
        })
        .flatMapMany(context -> {
            // 4. 获取 RAG Query Service URL
            String serviceUrl = getServiceUrl("rag-query-service");
            if (serviceUrl == null) {
                return Flux.just(
                    ServerSentEvent.<String>builder()
                        .event("error")
                        .data("{\"error\": \"RAG 查询服务不可用\"}")
                        .build()
                );
            }
            
            // 5. 构建查询请求
            Map<String, Object> queryRequest = new HashMap<>();
            queryRequest.put("question", request.getQuestion());
            queryRequest.put("session_history", context.sessionHistory.isEmpty() ? null : context.sessionHistory);
            queryRequest.put("top_k", request.getTopK());
            queryRequest.put("similarity_threshold", request.getSimilarityThreshold());
            queryRequest.put("stream", true);
            
            // 6. 调用流式查询
            WebClient webClient = webClientBuilder.baseUrl(serviceUrl).build();
            
            StringBuilder answerBuilder = new StringBuilder();
            
            return webClient.post()
                    .uri("/api/query/stream")
                    .bodyValue(queryRequest)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(chunk -> answerBuilder.append(chunk))
                    .map(chunk -> ServerSentEvent.<String>builder()
                            .event("message")
                            .data(chunk)
                            .build())
                    .concatWith(Mono.fromCallable(() -> {
                        // 7. 流式响应完成后，保存助手答案
                        saveAssistantMessage(context.session, answerBuilder.toString());
                        
                        return ServerSentEvent.<String>builder()
                                .event("done")
                                .data("{\"session_id\": \"" + context.session.getId() + "\"}")
                                .build();
                    }))
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
        })
        .onErrorResume(error -> {
            log.error("会话流式查询处理异常: {}", error.getMessage(), error);
            return Flux.just(
                ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"error\": \"" + error.getMessage() + "\"}")
                    .build()
            );
        });
    }
    
    /**
     * 创建新会话
     */
    private Session createNewSession(String userId) {
        Session session = Session.builder()
                .userId(userId != null ? userId : "default")
                .messages(new ArrayList<>())
                .build();
        return sessionRepository.save(session);
    }
    
    /**
     * 构建会话历史
     */
    private List<Map<String, String>> buildSessionHistory(Session session) {
        List<Map<String, String>> history = new ArrayList<>();
        List<Message> messages = session.getMessages();
        
        // 限制历史消息数量（最近 10 轮对话）
        int startIndex = Math.max(0, messages.size() - 20);
        List<Message> recentMessages = messages.subList(startIndex, messages.size());
        
        for (Message message : recentMessages) {
            Map<String, String> historyItem = new HashMap<>();
            historyItem.put("role", message.getRole().toLowerCase());
            historyItem.put("content", message.getContent());
            history.add(historyItem);
        }
        
        return history;
    }
    
    /**
     * 保存助手消息
     */
    private void saveAssistantMessage(Session session, String answer) {
        try {
            Session updatedSession = sessionRepository.findById(session.getId())
                    .orElse(session);
            
            Message assistantMessage = Message.builder()
                    .role("ASSISTANT")
                    .content(answer)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            updatedSession.addMessage(assistantMessage);
            sessionRepository.save(updatedSession);
            
            log.info("助手消息已保存，会话ID: {}", updatedSession.getId());
        } catch (Exception e) {
            log.error("保存助手消息失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取服务 URL
     */
    private String getServiceUrl(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        
        if (instances == null || instances.isEmpty()) {
            log.warn("未找到服务实例: {}", serviceName);
            return null;
        }
        
        ServiceInstance instance = instances.get(random.nextInt(instances.size()));
        return instance.getUri().toString();
    }
    
    /**
     * 会话上下文
     */
    private static class SessionContext {
        final Session session;
        final List<Map<String, String>> sessionHistory;
        
        SessionContext(Session session, List<Map<String, String>> sessionHistory) {
            this.session = session;
            this.sessionHistory = sessionHistory;
        }
    }
}
