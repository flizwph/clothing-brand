package com.brand.backend.application.auth.cqrs.command.password;

import com.brand.backend.application.auth.cqrs.command.base.Command;
import lombok.Builder;
import lombok.Data;

/**
 * Команда для инициации процесса восстановления пароля
 */
@Data
@Builder
public class InitiatePasswordResetCommand implements Command<String> {
    
    /**
     * Имя пользователя или идентификатор, для которого нужно восстановить пароль
     */
    private String username;
} 