package com.brand.backend.application.auth.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается при попытке доступа к деактивированному аккаунту
 */
public class AccountDisabledException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.FORBIDDEN;
    private static final String ERROR_CODE = "AUTH_ACCOUNT_DISABLED";
    
    public AccountDisabledException(String username) {
        super(String.format("Account '%s' has been disabled. Please contact support for assistance.", username), 
              STATUS, ERROR_CODE);
    }
} 