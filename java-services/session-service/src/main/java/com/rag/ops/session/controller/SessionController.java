package com.rag.ops.session.controller;

import com.rag.ops.session.dto.*;
import com.rag.ops.session.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理控制器
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {
    
    private final SessionService sessionService;
    
    /**
     * 创建新会话
     */
    @PostMapping
    public ResponseEntity<SessionResponse> createSession(@RequestBody(required = false) CreateSessionRequest request) {
        log.info("收到创建会话请求");
        
        if (request == null) {
            request = new CreateSessionRequest();
        }
        
        SessionResponse response = sessionService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 获取会话历史
     */
    @GetMapping("/{id}/history")
    public ResponseEntity<SessionHistoryResponse> getSessionHistory(@PathVariable String id) {
        log.info("收到获取会话历史请求，会话ID: {}", id);
        
        SessionHistoryResponse response = sessionService.getSessionHistory(id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 添加消息到会话
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageResponse> addMessage(
            @PathVariable String id,
            @Valid @RequestBody AddMessageRequest request) {
        log.info("收到添加消息请求，会话ID: {}", id);
        
        MessageResponse response = sessionService.addMessage(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String id) {
        log.info("收到删除会话请求，会话ID: {}", id);
        
        sessionService.deleteSession(id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "会话删除成功");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户的所有会话
     */
    @GetMapping
    public ResponseEntity<List<SessionResponse>> getUserSessions(@RequestParam(required = false) String userId) {
        log.info("收到获取用户会话列表请求，用户ID: {}", userId);
        
        if (userId == null || userId.isEmpty()) {
            userId = "default";
        }
        
        List<SessionResponse> response = sessionService.getUserSessions(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 处理会话查询
     * 整合会话历史和 RAG 查询
     */
    @PostMapping("/query")
    public ResponseEntity<SessionQueryResponse> processQuery(@Valid @RequestBody SessionQueryRequest request) {
        log.info("收到会话查询请求，问题: {}, 会话ID: {}", request.getQuestion(), request.getSessionId());
        
        SessionQueryResponse response = sessionService.processQuery(request);
        return ResponseEntity.ok(response);
    }
}
