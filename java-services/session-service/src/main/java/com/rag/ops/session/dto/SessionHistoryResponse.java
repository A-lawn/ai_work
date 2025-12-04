package com.rag.ops.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 会话历史响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionHistoryResponse {
    
    private String sessionId;
    private String userId;
    private List<MessageResponse> messages;
    private Integer totalMessages;
    private Integer totalTokens;
}
