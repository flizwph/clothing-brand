package com.brand.backend.application.auth.exception;

/**
 * Исключение, которое выбрасывается, когда пользователь заблокирован из-за слишком большого количества неудачных попыток входа
 */
public class UserBlockedException extends RuntimeException {
    
    private final String username;
    private final int minutesLeft;
    
    public UserBlockedException(String username, int minutesLeft) {
        super(String.format("Пользователь %s заблокирован из-за слишком большого количества неудачных попыток входа. " +
                "Пожалуйста, попробуйте снова через %d минут.", username, minutesLeft));
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