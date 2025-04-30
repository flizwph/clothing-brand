package com.brand.backend.application.auth.cqrs.query.user;

import com.brand.backend.application.auth.cqrs.query.base.Query;
import com.brand.backend.application.auth.cqrs.result.ValidateTokenResult;
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