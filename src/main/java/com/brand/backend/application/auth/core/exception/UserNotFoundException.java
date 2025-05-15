package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается, когда пользователь не найден
 */
public class UserNotFoundException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.NOT_FOUND;
    private static final String ERROR_CODE = "AUTH_USER_NOT_FOUND";
    
    public UserNotFoundException(String username) {
        super(String.format("User with username '%s' not found", username), STATUS, ERROR_CODE);
    }
} 