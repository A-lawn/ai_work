package com.rag.ops.document.exception;

/**
 * 无效文件异常
 */
public class InvalidFileException extends RuntimeException {
    
    public InvalidFileException(String message) {
        super(message);
    }
}
