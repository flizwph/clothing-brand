package com.brand.backend.application.auth.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Базовый класс для всех исключений, связанных с авторизацией
 */
@Getter
public abstract class AuthException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;
    
    protected AuthException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
} 