package com.brand.backend.application.auth.command;

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