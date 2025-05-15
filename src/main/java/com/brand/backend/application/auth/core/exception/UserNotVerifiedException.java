package com.brand.backend.application.auth.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается, когда пользователь не верифицирован
 */
@Getter
public class UserNotVerifiedException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.FORBIDDEN;
    private static final String ERROR_CODE = "AUTH_USER_NOT_VERIFIED";
    
    private final String verificationCode;
    
    public UserNotVerifiedException(String username, String verificationCode) {
        super(String.format("User '%s' is not verified", username), STATUS, ERROR_CODE);
        this.verificationCode = verificationCode;
    }
} 