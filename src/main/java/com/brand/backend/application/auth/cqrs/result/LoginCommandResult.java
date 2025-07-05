package com.brand.backend.application.auth.cqrs.result;

import com.brand.backend.domain.user.model.User;
import lombok.Builder;
import lombok.Data;

/**
 * Результат выполнения команды аутентификации
 */
@Data
@Builder
public class LoginCommandResult {
    private boolean success;
    private User user;
    private String accessToken;
    private String refreshToken;
    private String message;
    private boolean needsVerification;
    private String verificationCode;
    
    /**
     * Создает результат неудачной аутентификации
     */
    public static LoginCommandResult failure(String message) {
        return LoginCommandResult.builder()
                .success(false)
                .message(message)
                .needsVerification(false)
                .build();
    }
    
    /**
     * Создает результат успешной аутентификации с токенами
     */
    public static LoginCommandResult success(String message, String accessToken, String refreshToken, User user) {
        return LoginCommandResult.builder()
                .success(true)
                .message(message)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .needsVerification(false)
                .build();
    }
    
    /**
     * Создает результат, требующий верификации
     */
    public static LoginCommandResult verificationNeeded(String message, String verificationCode) {
        return LoginCommandResult.builder()
                .success(true)
                .message(message)
                .needsVerification(true)
                .verificationCode(verificationCode)
                .build();
    }
    
    /**
     * Проверяет, нужна ли верификация
     */
    public boolean isNeedsVerification() {
        return needsVerification;
    }
} 