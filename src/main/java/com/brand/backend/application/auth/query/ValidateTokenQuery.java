package com.brand.backend.application.auth.query;

import lombok.Builder;
import lombok.Data;

/**
 * Запрос для валидации токена
 */
@Data
@Builder
public class ValidateTokenQuery implements Query<ValidateTokenResult> {
    private String token;
} 