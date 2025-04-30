package com.brand.backend.application.auth.command;

import lombok.Builder;
import lombok.Data;

/**
 * Команда для обновления токена доступа
 */
@Data
@Builder
public class RefreshTokenCommand implements Command<RefreshTokenResult> {
    private String refreshToken;
} 