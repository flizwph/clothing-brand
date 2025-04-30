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
} 