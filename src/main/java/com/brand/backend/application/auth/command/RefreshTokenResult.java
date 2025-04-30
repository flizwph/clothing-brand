package com.brand.backend.application.auth.command;

import lombok.Builder;
import lombok.Data;

/**
 * Результат выполнения команды обновления токена
 */
@Data
@Builder
public class RefreshTokenResult {
    private boolean success;
    private String accessToken;
    private String message;
} 