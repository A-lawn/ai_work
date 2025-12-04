package com.rag.ops.session.dto;

import com.rag.ops.session.entity.Session;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 会话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    
    private String sessionId;
    private String userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer messageCount;
    private String metadata;
    
    /**
     * 从Session实体转换
     */
    public static SessionResponse fromEntity(Session session) {
        return SessionResponse.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessages() != null ? session.getMessages().size() : 0)
                .metadata(session.getMetadata())
                .build();
    }
}
