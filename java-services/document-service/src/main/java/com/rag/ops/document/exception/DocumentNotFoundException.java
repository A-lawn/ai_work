package com.rag.ops.document.exception;

/**
 * 文档未找到异常
 */
public class DocumentNotFoundException extends RuntimeException {
    
    public DocumentNotFoundException(String message) {
        super(message);
    }
}
