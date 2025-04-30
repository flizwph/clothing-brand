package com.brand.backend.application.auth.cqrs.command.user;

import com.brand.backend.application.auth.cqrs.result.RefreshTokenResult;
import com.brand.backend.application.auth.cqrs.command.base.Command;
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