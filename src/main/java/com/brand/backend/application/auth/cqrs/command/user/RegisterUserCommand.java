package com.brand.backend.application.auth.cqrs.command.user;

import com.brand.backend.application.auth.cqrs.command.base.Command;
import com.brand.backend.domain.user.model.User;
import lombok.Builder;
import lombok.Data;

/**
 * Команда для регистрации нового пользователя
 */
@Data
@Builder
public class RegisterUserCommand implements Command<User> {
    
    private String username;
    private String password;
    
} 