package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается при невалидном пароле
 */
public class InvalidPasswordException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;
    private static final String ERROR_CODE = "AUTH_INVALID_PASSWORD";
    
    public InvalidPasswordException(String message) {
        super(message, STATUS, ERROR_CODE);
    }
    
    public static InvalidPasswordException tooShort(int minLength) {
        return new InvalidPasswordException(String.format("Password must be at least %d characters long", minLength));
    }
    
    public static InvalidPasswordException sameAsOld() {
        return new InvalidPasswordException("New password must be different from the current one");
    }
    
    public static InvalidPasswordException empty() {
        return new InvalidPasswordException("Password cannot be empty");
    }
} 