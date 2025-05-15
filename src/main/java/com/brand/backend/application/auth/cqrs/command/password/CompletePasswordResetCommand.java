package com.brand.backend.application.auth.cqrs.command.password;

import com.brand.backend.application.auth.cqrs.command.base.Command;
import lombok.Builder;
import lombok.Data;

/**
 * Команда для завершения процесса восстановления пароля
 */
@Data
@Builder
public class CompletePasswordResetCommand implements Command<Boolean> {
    
    /**
     * Имя пользователя
     */
    private String username;
    
    /**
     * Код подтверждения восстановления пароля
     */
    private String resetCode;
    
    /**
     * Новый пароль
     */
    private String newPassword;
} 