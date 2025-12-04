package com.rag.ops.session.exception;

/**
 * 会话未找到异常
 */
public class SessionNotFoundException extends RuntimeException {
    
    public SessionNotFoundException(String message) {
        super(message);
    }
    
    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
