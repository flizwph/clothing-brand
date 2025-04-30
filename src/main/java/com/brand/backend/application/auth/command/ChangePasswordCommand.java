package com.brand.backend.application.auth.command;

import lombok.Builder;
import lombok.Data;

/**
 * Команда для изменения пароля пользователя
 */
@Data
@Builder
public class ChangePasswordCommand implements Command<Boolean> {
    private String username;
    private String currentPassword;
    private String newPassword;
} 