package com.brand.backend.application.auth.exception;

/**
 * Исключение для невалидных учетных данных
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
} 