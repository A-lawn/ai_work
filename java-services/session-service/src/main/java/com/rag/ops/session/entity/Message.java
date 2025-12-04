package com.rag.ops.session.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "id", columnDefinition = "VARCHAR(36)")
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonBackReference
    private Session session;
    
    @Column(name = "role", nullable = false, length = 20)
    private String role; // USER, ASSISTANT
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    /**
     * 估算消息的Token数量（简单估算：中文按字符数，英文按单词数*1.3）
     */
    public int getEstimatedTokens() {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        // 简单估算：中文字符数 + 英文单词数 * 1.3
        int chineseChars = 0;
        int englishWords = 0;
        
        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.matches(".*[\\u4e00-\\u9fa5].*")) {
                chineseChars += word.length();
            } else {
                englishWords++;
            }
        }
        
        return chineseChars + (int) (englishWords * 1.3);
    }
}
