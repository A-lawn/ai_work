package com.rag.ops.session.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.rag.ops.session.client.RagQueryClient;
import com.rag.ops.session.dto.*;
import com.rag.ops.session.entity.Message;
import com.rag.ops.session.entity.Session;
import com.rag.ops.session.exception.SessionNotFoundException;
import com.rag.ops.session.exception.TokenLimitExceededException;
import com.rag.ops.session.repository.MessageRepository;
import com.rag.ops.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会话服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {
    
    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final RagQueryClient ragQueryClient;
    
    // 配置常量
    private static final int MAX_HISTORY_MESSAGES = 10; // 最多保留10轮对话
    private static final int MAX_TOKEN_COUNT = 4000; // 最大Token数量
    
    /**
     * 创建新会话
     */
    @Transactional
    @SentinelResource(value = "createSession", blockHandler = "createSessionBlockHandler")
    public SessionResponse createSession(CreateSessionRequest request) {
        log.info("创建新会话，用户ID: {}", request.getUserId());
        
        Session session = Session.builder()
                .userId(request.getUserId())
                .metadata(request.getMetadata())
                .messages(new ArrayList<>())
                .build();
        
        session = sessionRepository.save(session);
        log.info("会话创建成功，会话ID: {}", session.getId());
        
        return SessionResponse.fromEntity(session);
    }
    
    /**
     * 获取会话历史
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "sessionHistory", key = "#sessionId")
    @SentinelResource(value = "getSessionHistory", blockHandler = "getSessionHistoryBlockHandler")
    public SessionHistoryResponse getSessionHistory(String sessionId) {
        log.info("获取会话历史，会话ID: {}", sessionId);
        
        Session session = sessionRepository.findByIdWithMessages(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("会话不存在: " + sessionId));
        
        List<MessageResponse> messageResponses = session.getMessages().stream()
                .map(MessageResponse::fromEntity)
                .collect(Collectors.toList());
        
        int totalTokens = session.getTotalTokenCount();
        
        return SessionHistoryResponse.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .messages(messageResponses)
                .totalMessages(messageResponses.size())
                .totalTokens(totalTokens)
                .build();
    }
    
    /**
     * 添加消息到会话
     */
    @Transactional
    @CacheEvict(value = "sessionHistory", key = "#sessionId")
    @SentinelResource(value = "addMessage", blockHandler = "addMessageBlockHandler")
    public MessageResponse addMessage(String sessionId, AddMessageRequest request) {
        log.info("添加消息到会话，会话ID: {}, 角色: {}", sessionId, request.getRole());
        
        Session session = sessionRepository.findByIdWithMessages(sessionId)
                .orElseThrow(() -> new SessionNotFoundException("会话不存在: " + sessionId));
        
        // 创建新消息
        Message message = Message.builder()
                .role(request.getRole())
                .content(request.getContent())
                .metadata(request.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
        
        // 添加消息到会话
        session.addMessage(message);
        
        // 应用滑动窗口策略（保留最近的消息）
        applySlidingWindow(session);
        
        // 检查Token限制
        checkTokenLimit(session);
        
        sessionRepository.save(session);
        log.info("消息添加成功，消息ID: {}", message.getId());
        
        return MessageResponse.fromEntity(message);
    }
    
    /**
     * 删除会话
     */
    @Transactional
    @CacheEvict(value = "sessionHistory", key = "#sessionId")
    @SentinelResource(value = "deleteSession", blockHandler = "deleteSessionBlockHandler")
    public void deleteSession(String sessionId) {
        log.info("删除会话，会话ID: {}", sessionId);
        
        if (!sessionRepository.existsById(sessionId)) {
            throw new SessionNotFoundException("会话不存在: " + sessionId);
        }
        
        sessionRepository.deleteById(sessionId);
        log.info("会话删除成功，会话ID: {}", sessionId);
    }
    
    /**
     * 获取用户的所有会话
     */
    @Transactional(readOnly = true)
    @SentinelResource(value = "getUserSessions")
    public List<SessionResponse> getUserSessions(String userId) {
        log.info("获取用户会话列表，用户ID: {}", userId);
        
        List<Session> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        
        return sessions.stream()
                .map(SessionResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    /**
     * 应用滑动窗口策略
     * 保留最近的MAX_HISTORY_MESSAGES条消息
     */
    private void applySlidingWindow(Session session) {
        List<Message> messages = session.getMessages();
        
        if (messages.size() > MAX_HISTORY_MESSAGES * 2) { // 每轮对话包含用户和助手两条消息
            log.info("应用滑动窗口策略，当前消息数: {}", messages.size());
            
            // 按时间戳排序
            messages.sort((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()));
            
            // 保留最近的消息
            int removeCount = messages.size() - (MAX_HISTORY_MESSAGES * 2);
            List<Message> messagesToRemove = new ArrayList<>(messages.subList(0, removeCount));
            
            // 从会话中移除旧消息
            messages.removeAll(messagesToRemove);
            
            // 从数据库中删除旧消息
            messageRepository.deleteAll(messagesToRemove);
            
            log.info("滑动窗口策略应用完成，移除消息数: {}, 剩余消息数: {}", 
                    removeCount, messages.size());
        }
    }
    
    /**
     * 检查Token限制
     */
    private void checkTokenLimit(Session session) {
        int totalTokens = session.getTotalTokenCount();
        
        if (totalTokens > MAX_TOKEN_COUNT) {
            log.warn("会话Token数量超过限制，会话ID: {}, Token数: {}", 
                    session.getId(), totalTokens);
            throw new TokenLimitExceededException(
                    String.format("会话Token数量超过限制: %d/%d", totalTokens, MAX_TOKEN_COUNT));
        }
        
        log.debug("会话Token数量检查通过，会话ID: {}, Token数: {}", 
                session.getId(), totalTokens);
    }
    
    // Sentinel 降级处理方法
    public SessionResponse createSessionBlockHandler(CreateSessionRequest request, 
                                                     com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("创建会话被限流或熔断: {}", ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后重试");
    }
    
    public SessionHistoryResponse getSessionHistoryBlockHandler(String sessionId, 
                                                                 com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("获取会话历史被限流或熔断: {}", ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后重试");
    }
    
    public MessageResponse addMessageBlockHandler(String sessionId, AddMessageRequest request, 
                                                   com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("添加消息被限流或熔断: {}", ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后重试");
    }
    
    public void deleteSessionBlockHandler(String sessionId, 
                                          com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("删除会话被限流或熔断: {}", ex.getMessage());
        throw new RuntimeException("系统繁忙，请稍后重试");
    }
    
    /**
     * 处理会话查询
     * 整合会话历史和 RAG 查询
     */
    @Transactional
    @SentinelResource(value = "processQuery", blockHandler = "processQueryBlockHandler")
    public SessionQueryResponse processQuery(SessionQueryRequest request) {
        log.info("处理会话查询，问题: {}, 会话ID: {}", request.getQuestion(), request.getSessionId());
        
        // 1. 获取或创建会话
        Session session;
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            session = sessionRepository.findByIdWithMessages(request.getSessionId())
                    .orElseThrow(() -> new SessionNotFoundException("会话不存在: " + request.getSessionId()));
            log.info("使用现有会话，会话ID: {}", session.getId());
        } else {
            // 创建新会话
            session = Session.builder()
                    .userId(request.getUserId())
                    .messages(new ArrayList<>())
                    .build();
            session = sessionRepository.save(session);
            log.info("创建新会话，会话ID: {}", session.getId());
        }
        
        // 2. 获取会话历史
        List<Map<String, String>> sessionHistory = buildSessionHistory(session);
        log.debug("会话历史消息数: {}", sessionHistory.size());
        
        // 3. 构建 RAG 查询请求
        QueryRequest queryRequest = QueryRequest.builder()
                .question(request.getQuestion())
                .sessionHistory(sessionHistory.isEmpty() ? null : sessionHistory)
                .topK(request.getTopK())
                .similarityThreshold(request.getSimilarityThreshold())
                .stream(false)
                .build();
        
        // 4. 调用 RAG 查询服务
        log.info("调用 RAG 查询服务");
        QueryResponse queryResponse = ragQueryClient.query(queryRequest);
        log.info("RAG 查询完成，耗时: {}秒", queryResponse.getQueryTime());
        
        // 5. 保存用户问题到会话历史
        Message userMessage = Message.builder()
                .role("USER")
                .content(request.getQuestion())
                .timestamp(LocalDateTime.now())
                .build();
        session.addMessage(userMessage);
        
        // 6. 保存助手答案到会话历史
        Map<String, Object> assistantMetadata = new HashMap<>();
        assistantMetadata.put("sources", queryResponse.getSources());
        assistantMetadata.put("queryTime", queryResponse.getQueryTime());
        
        Message assistantMessage = Message.builder()
                .role("ASSISTANT")
                .content(queryResponse.getAnswer())
                .metadata(assistantMetadata.toString())
                .timestamp(LocalDateTime.now())
                .build();
        session.addMessage(assistantMessage);
        
        // 7. 应用滑动窗口策略
        applySlidingWindow(session);
        
        // 8. 检查 Token 限制
        checkTokenLimit(session);
        
        // 9. 保存会话
        sessionRepository.save(session);
        log.info("会话更新成功，会话ID: {}, 消息数: {}", session.getId(), session.getMessages().size());
        
        // 10. 返回响应
        return SessionQueryResponse.builder()
                .sessionId(session.getId())
                .answer(queryResponse.getAnswer())
                .sources(queryResponse.getSources())
                .queryTime(queryResponse.getQueryTime())
                .build();
    }
    
    /**
     * 构建会话历史
     * 转换为 RAG 服务需要的格式
     */
    private List<Map<String, String>> buildSessionHistory(Session session) {
        List<Map<String, String>> history = new ArrayList<>();
        
        // 获取最近的消息（已按时间排序）
        List<Message> messages = session.getMessages();
        
        // 限制历史消息数量
        int startIndex = Math.max(0, messages.size() - (MAX_HISTORY_MESSAGES * 2));
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
     * 查询处理降级方法
     */
    public SessionQueryResponse processQueryBlockHandler(SessionQueryRequest request,
                                                         com.alibaba.csp.sentinel.slots.block.BlockException ex) {
        log.warn("查询处理被限流或熔断: {}", ex.getMessage());
        
        return SessionQueryResponse.builder()
                .sessionId(request.getSessionId())
                .answer("系统繁忙，请稍后重试")
                .sources(new ArrayList<>())
                .queryTime(0.0)
                .build();
    }
}
