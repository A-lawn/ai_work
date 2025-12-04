package com.rag.ops.session.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加消息请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {
    
    @NotBlank(message = "角色不能为空")
    private String role; // USER, ASSISTANT
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private String metadata;
}
