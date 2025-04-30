package com.brand.backend.application.auth.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Исключение, которое выбрасывается, когда пользователь заблокирован из-за слишком большого количества неудачных попыток входа
 */
@Getter
public class UserBlockedException extends AuthException {
    
    private static final HttpStatus STATUS = HttpStatus.TOO_MANY_REQUESTS;
    private static final String ERROR_CODE = "AUTH_USER_BLOCKED";
    
    private final String username;
    private final int minutesLeft;
    
    public UserBlockedException(String username, int minutesLeft) {
        super(String.format("User %s is blocked due to too many failed login attempts. Please try again in %d minutes.", 
                username, minutesLeft), STATUS, ERROR_CODE);
        this.username = username;
        this.minutesLeft = minutesLeft;
    }
    
    /**
     * Получить имя пользователя
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Получить оставшееся время блокировки в минутах
     */
    public int getMinutesLeft() {
        return minutesLeft;
    }
} 