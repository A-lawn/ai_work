package com.rag.ops.session.exception;

/**
 * Token限制超出异常
 */
public class TokenLimitExceededException extends RuntimeException {
    
    public TokenLimitExceededException(String message) {
        super(message);
    }
    
    public TokenLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
