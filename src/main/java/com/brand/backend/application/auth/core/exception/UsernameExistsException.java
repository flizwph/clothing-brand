package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается, когда имя пользователя уже занято
 */
public class UsernameExistsException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.CONFLICT;
    private static final String ERROR_CODE = "AUTH_USERNAME_EXISTS";
    
    public UsernameExistsException(String username) {
        super(String.format("Username '%s' is already taken", username), STATUS, ERROR_CODE);
    }
} 