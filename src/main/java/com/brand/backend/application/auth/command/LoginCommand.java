package com.brand.backend.application.auth.command;

import lombok.Builder;
import lombok.Data;

/**
 * Команда для аутентификации пользователя
 */
@Data
@Builder
public class LoginCommand implements Command<LoginCommandResult> {
    private String username;
    private String password;
} 