package com.brand.backend.application.auth.cqrs.command.user;

import com.brand.backend.application.auth.cqrs.command.base.Command;
import lombok.Builder;
import lombok.Data;

/**
 * Команда для выхода из системы
 */
@Data
@Builder
public class LogoutCommand implements Command<Boolean> {
    private String username;
} 