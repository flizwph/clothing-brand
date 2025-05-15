package com.brand.backend.presentation.rest.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Обработчик исключений для API транзакций
 */
@RestControllerAdvice
@Slf4j
public class TransactionExceptionHandler {
    
    /**
     * Обработка исключения IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] Ошибка в запросе: {}", errorId, ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    
    /**
     * Обработка исключения IllegalStateException
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {
        
        String errorId = UUID.randomUUID().toString().substring(0, 8);
        log.error("[{}] Ошибка состояния: {}", errorId, ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("errorId", errorId);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
} 