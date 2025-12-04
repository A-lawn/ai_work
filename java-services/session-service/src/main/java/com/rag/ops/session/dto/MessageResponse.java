package com.rag.ops.session.dto;

import com.rag.ops.session.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    
    private String id;
    private String role;
    private String content;
    private LocalDateTime timestamp;
    private String metadata;
    
    /**
     * 从Message实体转换
     */
    public static MessageResponse fromEntity(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .metadata(message.getMetadata())
                .build();
    }
}
