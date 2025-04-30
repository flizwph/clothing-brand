package com.brand.backend.application.auth.cqrs.command.user;

import com.brand.backend.application.auth.cqrs.result.LoginCommandResult;
import com.brand.backend.application.auth.cqrs.command.base.Command;
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