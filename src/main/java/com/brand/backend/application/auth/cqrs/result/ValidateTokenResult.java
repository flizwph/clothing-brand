package com.brand.backend.application.auth.cqrs.result;

import lombok.Builder;
import lombok.Data;

/**
 * Результат валидации токена
 */
@Data
@Builder
public class ValidateTokenResult {
    private boolean valid;
    private String username;
    private String message;
} 