package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается при невалидном токене
 */
public class InvalidTokenException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;
    private static final String ERROR_CODE = "AUTH_INVALID_TOKEN";
    
    public InvalidTokenException(String message) {
        super(message, STATUS, ERROR_CODE);
    }
    
    public static InvalidTokenException expired() {
        return new InvalidTokenException("Token has expired");
    }
    
    public static InvalidTokenException malformed() {
        return new InvalidTokenException("Token is malformed");
    }
    
    public static InvalidTokenException invalid() {
        return new InvalidTokenException("Token is invalid");
    }
    
    public static InvalidTokenException wrongVersion() {
        return new InvalidTokenException("Token version is invalid, please login again");
    }
} 