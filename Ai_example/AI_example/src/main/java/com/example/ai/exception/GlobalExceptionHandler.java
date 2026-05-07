package com.example.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", "INTERNAL_ERROR");
        errorDetails.put("message", e.getMessage());
        error.put("error", errorDetails);
        
        error.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", "DOCUMENT_TOO_LARGE");
        errorDetails.put("message", "文档大小超过 10MB 限制");
        error.put("error", errorDetails);
        
        error.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("code", "INVALID_REQUEST");
        errorDetails.put("message", e.getMessage());
        error.put("error", errorDetails);
        
        error.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
