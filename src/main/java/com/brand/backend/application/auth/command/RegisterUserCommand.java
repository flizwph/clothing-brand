package com.brand.backend.application.auth.command;

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