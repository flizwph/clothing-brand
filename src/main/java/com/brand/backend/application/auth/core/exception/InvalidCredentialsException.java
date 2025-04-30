package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение для невалидных учетных данных
 */
public class InvalidCredentialsException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;
    private static final String ERROR_CODE = "AUTH_INVALID_CREDENTIALS";
    
    public InvalidCredentialsException(String message) {
        super(message, STATUS, ERROR_CODE);
    }
} 