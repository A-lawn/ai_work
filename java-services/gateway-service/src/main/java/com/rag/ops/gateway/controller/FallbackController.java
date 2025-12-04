package com.rag.ops.gateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 降级处理控制器
 * 当服务不可用时返回友好的错误信息
 */
@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {
    
    /**
     * 查询服务降级处理
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryFallback() {
        log.warn("查询服务不可用，执行降级处理");
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_UNAVAILABLE");
        response.put("message", "查询服务暂时不可用，请稍后重试");
        response.put("code", 503);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
    
    /**
     * 流式查询服务降级处理
     */
    @PostMapping("/query-stream")
    public ResponseEntity<Map<String, Object>> queryStreamFallback() {
        log.warn("流式查询服务不可用，执行降级处理");
        
        Map<String, Object> response = new HashMap<>();
        response.put("error", "SERVICE_UNAVAILABLE");
        response.put("message", "流式查询服务暂时不可用，请稍后重试");
        response.put("code", 503);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}
