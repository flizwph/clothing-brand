package com.brand.backend.presentation.rest.advice;

import com.brand.backend.application.auth.core.exception.AuthException;
import com.brand.backend.application.auth.core.exception.UserBlockedException;
import com.brand.backend.application.auth.core.exception.UserNotVerifiedException;
import com.brand.backend.application.auth.core.exception.UsernameExistsException;
import com.brand.backend.presentation.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Глобальный обработчик исключений аутентификации и авторизации
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {
    
    /**
     * Специальный обработчик для исключения о существующем пользователе
     * Наивысший приоритет
     */
    @Order(1)
    @ExceptionHandler(UsernameExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameExistsException(
            UsernameExistsException ex, HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        log.error("[{}] Registration error: {}", errorId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .code("USERNAME_EXISTS")
                .path(request.getRequestURI())
                .errorId(errorId)
                .build();
                
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
    
    /**
     * Обрабатывает все исключения авторизации кроме UsernameExistsException
     * Срабатывает если ни один специализированный обработчик не подошел
     */
    @Order(2)
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex, HttpServletRequest request) {
        String errorId = generateErrorId();
        
        log.error("[{}] Auth error: {}", errorId, ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .message(ex.getMessage())
                .code(ex.getErrorCode())
                .path(request.getRequestURI())
                .errorId(errorId)
                .build();
                
        // Добавление дополнительной информации для определенных типов исключений
        if (ex instanceof UserBlockedException) {
            response.addAdditionalInfo("minutesLeft", ((UserBlockedException) ex).getMinutesLeft());
        } else if (ex instanceof UserNotVerifiedException) {
            response.addAdditionalInfo("verificationCode", ((UserNotVerifiedException) ex).getVerificationCode());
        }
        
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    /**
     * Обрабатывает ошибки валидации запросов
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String errorId = generateErrorId();
        
        log.error("[{}] Validation error: {}", errorId, ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .code("VALIDATION_ERROR")
                .path(request.getRequestURI())
                .errorId(errorId)
                .validationErrors(errors)
                .build();
                
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Генерирует уникальный ID ошибки для отслеживания
     */
    private String generateErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
} 